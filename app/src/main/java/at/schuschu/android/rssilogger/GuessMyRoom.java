package at.schuschu.android.rssilogger;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import at.schuschu.android.rssilogger.R;

public class GuessMyRoom extends Activity {
    Integer number_of_measurements;
    private HashMap<String, HashMap<String, HashMap< String, Double >>> feature_map;
    private ArrayList<String> roomlist;
    private HashMap<String, Float> room_probabilities;
    private BayesThread bc;
    private IntentFilter filter;
    private WifiManager wifimanager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guess_my_room);
        feature_map = (HashMap<String, HashMap<String, HashMap< String, Double >>>) getIntent().getSerializableExtra("Features");
        roomlist = (ArrayList<String>) getIntent().getSerializableExtra("Config");
        room_probabilities = createInitialBelief(roomlist);
        wifimanager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        //Toast.makeText(getApplicationContext(), "Works better with WiFi(TM) enabled", Toast.LENGTH_LONG).show();
        wifimanager.setWifiEnabled(true);
        number_of_measurements = 0;
        bc = new BayesThread();
        filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(bc, filter);
        wifimanager.startScan();

    }

    private HashMap<String, Float> createInitialBelief(ArrayList<String> rooms) {
        HashMap<String, Float> room_probs = new HashMap<String, Float>();
        Float init = 1.0f/(new Float(rooms.size()));
        for (String room : rooms) {
            room_probs.put(room,init);
        }
        return room_probs;
    }

    private void UpdateBayes(List<ScanResult> rssi_sigs) {
        wifimanager.startScan();
        number_of_measurements++;
        Comparator<ScanResult> c = new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult lhs, ScanResult rhs) {
                return Integer.compare(rhs.level,lhs.level);
            }
        };
        Collections.sort(rssi_sigs, c);
        int i = 0;
        HashMap< String, HashMap<String, Float> > acc_point_beliefs = new HashMap<String, HashMap<String, Float>>();
        for (ScanResult rssi_signal : rssi_sigs) {
            acc_point_beliefs.put(rssi_signal.BSSID, new HashMap<String, Float>(room_probabilities));
            if (i > 3) {
                break;
            }
            i++;
        }
        i = 0;
        for (ScanResult rssi_signal : rssi_sigs) {
            if (i > 3) {
                break;
            }
            i++;
            HashMap<String, HashMap<String, Double>> acc_point;
            if (feature_map.containsKey(rssi_signal.BSSID)) {
                acc_point = feature_map.get(rssi_signal.BSSID);
            } else {
                continue;
            }


            HashMap<String, Float> cur_acc_point_belief = acc_point_beliefs.get(rssi_signal.BSSID);
            Float sum = 0.0f;

            for (String rooms : acc_point.keySet()) {
                Float cur_prob = cur_acc_point_belief.get(rooms);

                Double measurement = 0.0;
                if (acc_point.get(rooms).containsKey(Integer.toString(rssi_signal.level))) {
                    measurement = acc_point.get(rooms).get(Integer.toString(rssi_signal.level));
                }

                cur_acc_point_belief.put(rooms, (cur_prob * measurement.floatValue()));
                sum += cur_acc_point_belief.get(rooms);
            }
            if (sum != 0.0f) {
                for (String rooms : acc_point.keySet()) {
                    if (cur_acc_point_belief.get(rooms) != 0.0f) {
                        cur_acc_point_belief.put(rooms, cur_acc_point_belief.get(rooms) / sum);
                    }
                }
            }
            Float checksum = 0.0f;
            for (Float acc_point_bel : cur_acc_point_belief.values()) {
                checksum += acc_point_bel.floatValue();
            }
            if (checksum > 1.1f || checksum < 0.99f) {
                    Log.e("sum != 1", "We have a problem here " + rssi_signal.BSSID + " " + rssi_signal.SSID + ",but " + Float.toString(checksum) + ".");

            } else {
                Log.d("checksum", "current sum is " + Float.toString(checksum));
            }
        }
        i = 0;
        for (String acc_point : acc_point_beliefs.keySet()) {
            if (i > 3) {
                break;
            }
            i++;
            try {
                Gson gson = new Gson();
                String json = gson.toJson(acc_point_beliefs.get(acc_point));
                FileWriter writer = new FileWriter(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + LoggerMain.rssi_dir + File.separator + acc_point + ".json", true);
                writer.write(json);
                writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (HashMap<String, Float> room_belief : acc_point_beliefs.values()) {
            boolean first = true;
            for (String cur_room : room_belief.keySet()) {
                if (first) {
                    room_probabilities.put(cur_room, 0.0f);
                }
                room_probabilities.put(cur_room, room_belief.get(cur_room) + room_probabilities.get(cur_room));
            }
            first = false;
        }
        normalize(room_probabilities);
        try {
            Gson gson = new Gson();
            String json = gson.toJson(room_probabilities);
            FileWriter writer = new FileWriter(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + LoggerMain.rssi_dir + File.separator + "roomprobs.json", true);
            writer.write(json);
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        TextView result = (TextView) findViewById(R.id.resultText);
        result.setText("Current number of measurements: " + Integer.toString(number_of_measurements));
    }

    private void normalize(HashMap<String, Float> probs) {
        Float checksum = 0.0f;
        for (Float cur_prob : probs.values()) {
            checksum += cur_prob;
        }
        if (checksum == 0.0f)
            return;
        for (String cur_prob_string : probs.keySet()) {
            if (probs.get(cur_prob_string) == 0.0f)
                continue;
            probs.put(cur_prob_string, probs.get(cur_prob_string) / checksum);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.guess_my_room, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class BayesThread extends BroadcastReceiver {



        @Override
        public void onReceive(Context context, Intent intent) {
            UpdateBayes(wifimanager.getScanResults());
        }
    }

    public void initBelief(View v) {
        room_probabilities = createInitialBelief(roomlist);
    }
}
