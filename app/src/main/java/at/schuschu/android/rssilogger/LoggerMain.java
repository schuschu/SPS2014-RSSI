package at.schuschu.android.rssilogger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class LoggerMain extends Activity {

    private static final String rssi_dir = File.separator + "rssi_logger";
    BroCast bc = null;
    IntentFilter filter;
    WifiManager wifimanager;
    ListView listview;
    Button button;
    Spinner spinner;
    TextView lastscan;
    Spinny spinny;
    // Accesspoint is key 1, cell is key 2, rssi value is key 3 and we get a probability
    private HashMap<String, HashMap<String, HashMap<String, Integer> > > pmf_map;
    public HashMap<String, HashMap<String, HashMap< String, Float >>> feature_map;

    boolean running;
    String suffix;

    SimpleAdapter adapter;
    ArrayAdapter<String> roomadapter;

    static ArrayList<String> roomlist = new ArrayList<String>();
    ArrayList<HashMap<String, String>> arraylist = new ArrayList<HashMap<String, String>>();
    HashMap<String, ArrayList<HashMap<String, String>>> backlog = new HashMap<String, ArrayList<HashMap<String, String>>>();

    List<ScanResult> results;
    final static String SSID_KEY = "secret_ssid_key_that_could_be_a_UUID";
    final static String LEVEL_KEY = "secret_level_key_that_could_be_a_UUID";
    final static String BSSID_KEY = "secret_bssid_key_that_could_be_a_UUID";
    int size = 0;


    class Spinny implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            suffix = String.valueOf(spinner.getSelectedItem());
            backlog.clear();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    class BroCast extends BroadcastReceiver {

        @Override
        public void onReceive(Context c, Intent intent) {
            results = wifimanager.getScanResults();
            size = results.size();
            updateview();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logger_main);
        feature_map = new HashMap<String, HashMap<String, HashMap< String, Float >>>();
        running = false;
        lastscan = (TextView) findViewById(R.id.tv_lastscan);
        button = (Button) findViewById(R.id.bu_scan);
        listview = (ListView) findViewById(R.id.lv_results);
        spinner = (Spinner) findViewById(R.id.sp_room);

        try{
            File rssi_dir = new File(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + LoggerMain.rssi_dir);
            if (!rssi_dir.exists())
            rssi_dir.mkdirs();
        } catch (Exception e) {
            e.printStackTrace();
        }

        wifimanager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        {
            //Toast.makeText(getApplicationContext(), "Works better with WiFi(TM) enabled", Toast.LENGTH_LONG).show();
            wifimanager.setWifiEnabled(true);
        }

        try {
            File json = new File(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + LoggerMain.rssi_dir + File.separator + "rssilogger-cfg.json");
            if (json.exists()) {
                BufferedReader br;
                br = new BufferedReader(new FileReader(json));
                Gson gson = new Gson();
                roomlist = gson.fromJson(br, roomlist.getClass());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

//        try {
//            File json = new File(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + LoggerMain.rssi_dir + File.separator +"rssilogger.json");
//            if (json.exists()) {
//                BufferedReader br;
//                br = new BufferedReader(new FileReader(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + LoggerMain.rssi_dir + File.separator +"rssilogger.json"));
//                Gson gson = new Gson();
//                backlog = gson.fromJson(br, backlog.getClass());
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        if (roomlist.isEmpty()) {
            roomlist.add("Default");
        }

        pmf_map = new HashMap<String, HashMap<String, HashMap<String, Integer> > >();

        roomadapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, roomlist);
        spinner.setAdapter(roomadapter);

        adapter = new SimpleAdapter(this, arraylist, R.layout.listview_row, new String[]{SSID_KEY, LEVEL_KEY, BSSID_KEY}, new int[]{R.id.tv_row_ssid, R.id.tv_row_level, R.id.tv_row_bssid});
        listview.setAdapter(adapter);

        bc = new BroCast();
        filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(bc, filter);

        spinny=new Spinny();
        spinner.setOnItemSelectedListener(spinny);

        suffix=String.valueOf(spinner.getSelectedItem());

    }

    protected void onPause() {
        super.onPause();
        if (bc != null) {
            unregisterReceiver(bc);
            bc = null;
        }
    }

    protected void onResume() {
        super.onResume();
        bc = new BroCast();
        filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(bc, filter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.logger_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    public void addRoom(View v) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Add new room:");
        alert.setMessage("Name:");

// Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = String.valueOf(input.getText());
                //Toast.makeText(getApplicationContext(), "Input:! " + value, Toast.LENGTH_SHORT).show();
                LoggerMain.roomlist.add(value);
                try {
                    Gson gson = new Gson();
                    String json = gson.toJson(LoggerMain.roomlist);
                    FileWriter writer = new FileWriter(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + LoggerMain.rssi_dir + File.separator + "rssilogger-cfg.json",false);
                    writer.write(json);
                    writer.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    public void replay(View v) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable("BACKLOG", backlog);
        intent.putExtras(bundle);
        intent.setClass(this, LoggerReplay.class);
        startActivity(intent);
    }

    public void guessMyRoomIntent(View v) {

    }

    public void scan(View v) {
        Toast.makeText(this, "Scan STARTED!", Toast.LENGTH_SHORT).show();
        running = true;
        wifimanager.startScan();

    }

    public void stopscan(View view) {
        Toast.makeText(this, "Scan STOPPED!", Toast.LENGTH_SHORT).show();

        running = false;
        if (bc != null) {
            unregisterReceiver(bc);
            bc = null;
        }
    }

    public void deleteRoom(View view) {

        if (roomlist.isEmpty()|| roomlist.size()<=1) {
            return;
        }

        roomlist.remove(spinner.getSelectedItemPosition());
        roomadapter.notifyDataSetChanged();
    }

    public void updateview() {

        if (!running) {
            if (bc != null) {
                unregisterReceiver(bc);
                bc = null;
            }
            return;
        }

        arraylist.clear();
        wifimanager.startScan();

        try {
            size = size - 1;
            while (size >= 0) {
                HashMap<String, String> item = new HashMap<String, String>();
                item.put(SSID_KEY, results.get(size).SSID);
                item.put(LEVEL_KEY, Integer.toString(results.get(size).level));
                item.put(BSSID_KEY, results.get(size).BSSID);

                arraylist.add(item);
                size--;
                adapter.notifyDataSetChanged();
            }
        } catch (Exception e) { /* this is a sad day for mankind*/ } finally {
            ArrayList<HashMap<String, String>> tempy = new ArrayList<HashMap<String, String>>(arraylist);

            long mills = System.currentTimeMillis();
            lastscan.setText("Last scan: " + mills);
            backlog.put(Long.toString(mills), tempy);

            try {
                Gson gson = new Gson();
                String json = gson.toJson(backlog);
                FileWriter writer = new FileWriter(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + LoggerMain.rssi_dir + File.separator + "rssilogger-"+suffix+"-json",false);
                writer.write(json);
                writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void extractFeatures(View view) {
        for (String room : roomlist) {
            HashMap<String, ArrayList<LinkedTreeMap<String, String>>> current_room = new HashMap<String, ArrayList<LinkedTreeMap<String, String>>>();
            try {
                File json = new File(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + LoggerMain.rssi_dir + File.separator +"rssilogger-"+room+"-json");
                if (json.exists()) {
                    BufferedReader br;
                    br = new BufferedReader(new FileReader(json));
                    Gson gson = new Gson();
                    current_room = gson.fromJson(br, current_room.getClass());
                }
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            for (ArrayList<LinkedTreeMap<String, String>> access_points : current_room.values()) {
                for (LinkedTreeMap<String, String> cur_access_points : access_points) {
                    String name = cur_access_points.get(BSSID_KEY);
                    if(Integer.parseInt(cur_access_points.get(LEVEL_KEY))<-80)
                        continue;

//                    name = name.substring(0,name.length() - 4);
                    // we need to filter bssid which is done by the above statemet.. <.<
                    if (!pmf_map.containsKey(name)) {
                        pmf_map.put(name, new HashMap<String, HashMap<String, Integer> >());
                    }

                    if (!pmf_map.get(name).containsKey(room)) {
                        pmf_map.get(name).put(room, new HashMap<String, Integer>());
                    }

                    if (!pmf_map.get(name).get(room).containsKey(cur_access_points.get(LEVEL_KEY))) {
                        pmf_map.get(name).get(room).put(cur_access_points.get(LEVEL_KEY), 0);
                    }
                    pmf_map.get(name).get(room).put(cur_access_points.get(LEVEL_KEY), pmf_map.get(name).get(room).get(cur_access_points.get(LEVEL_KEY)) + 1);
                }
            }


        }
        // Accesspoint is key 1, cell is key 2, rssi value is key 3 and we get a probability
//        public HashMap<String, HashMap<String, HashMap< String, Float >>> feature_map;
        for (String access_point_string : pmf_map.keySet()) {
            HashMap<String, HashMap <String, Integer>> access_point = pmf_map.get(access_point_string);
            if (!feature_map.containsKey(access_point_string)) {
                feature_map.put(access_point_string, new HashMap<String, HashMap<String, Float>>());
            }
            for (String room_string : access_point.keySet()) {
                HashMap<String, Integer> room = access_point.get(room_string);
                if (!feature_map.get(access_point_string).containsKey(room_string)) {
                    feature_map.get(access_point_string).put(room_string, new HashMap<String, Float>());
                }
                Integer sum = 0;
                for (Integer i : room.values()) {
                    sum += i;
                }
                for (String i_string : room.keySet()) {
                    Integer i = room.get(i_string);
                    feature_map.get(access_point_string).get(room_string).put(i_string, i.floatValue()/sum.floatValue());
                }

            }
        }
        try {
            Gson gson = new Gson();
            String json = gson.toJson(pmf_map);
            FileWriter writer = new FileWriter(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + LoggerMain.rssi_dir + File.separator + "pmf_map.json",false);
            writer.write(json);
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Gson gson = new Gson();
            String json = gson.toJson(feature_map);
            FileWriter writer = new FileWriter(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + LoggerMain.rssi_dir + File.separator + "feature_map.json",false);
            writer.write(json);
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
