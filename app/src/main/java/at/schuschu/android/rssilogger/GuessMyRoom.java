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
import com.google.gson.internal.LinkedTreeMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import at.schuschu.android.rssilogger.R;

public class GuessMyRoom extends Activity {
    Integer number_of_measurements;
    private LinkedTreeMap<String, LinkedTreeMap<String, LinkedTreeMap< String, Double >>> feature_map;
    private LinkedTreeMap<String, LinkedTreeMap<String, LinkedTreeMap<String, Double >>> pmf_map;
    private ArrayList<String> roomlist;
    private LinkedTreeMap<String, Float> room_probabilities;
    private BayesThread bc;
    private IntentFilter filter;
    private WifiManager wifimanager;
    private FeatureMapInterface features;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guess_my_room);
        feature_map = new LinkedTreeMap<String, LinkedTreeMap<String, LinkedTreeMap<String, Double>>>();
        pmf_map = new LinkedTreeMap<String, LinkedTreeMap<String, LinkedTreeMap<String, Double>>>();
//        feature_map = (LinkedTreeMap<String, LinkedTreeMap<String, LinkedTreeMap< String, Double >>>) getIntent().getSerializableExtra("Features");
//        pmf_map = (LinkedTreeMap<String, LinkedTreeMap<String, LinkedTreeMap<String, Integer>>>) getIntent().getSerializableExtra("PMF_MAP");
        //change this for gaussian

        try {
            File json = new File(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + LoggerMain.rssi_dir + File.separator + "feature_map.json");
            if (json.exists()) {
                BufferedReader br;
                br = new BufferedReader(new FileReader(json));
                Gson gson = new Gson();
                feature_map = gson.fromJson(br, feature_map.getClass());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            File json = new File(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + LoggerMain.rssi_dir + File.separator + "pmf_map.json");
            if (json.exists()) {
                BufferedReader br;
                br = new BufferedReader(new FileReader(json));
                Gson gson = new Gson();
                pmf_map = gson.fromJson(br, pmf_map.getClass());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
//
// features = new FeatureMapLUT(feature_map);
        features = new FeatureMapGauss(pmf_map);
        try {
            Gson gson = new Gson();
            String json = gson.toJson(features.getFeatureMap());
            FileWriter writer = new FileWriter(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + LoggerMain.rssi_dir + File.separator + "current_feature_map.json", true);
            writer.write(json);
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private LinkedTreeMap<String, Float> createInitialBelief(ArrayList<String> rooms) {
        LinkedTreeMap<String, Float> room_probs = new LinkedTreeMap<String, Float>();
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
        LinkedTreeMap< String, LinkedTreeMap<String, Float> > acc_point_beliefs = new LinkedTreeMap<String, LinkedTreeMap<String, Float>>();
        for (ScanResult rssi_signal : rssi_sigs) {
            if (!features.doesAccPointExist(rssi_signal.BSSID)) {
                continue;
            }

            acc_point_beliefs.put(rssi_signal.BSSID, new LinkedTreeMap<String, Float>());
            for (String room : room_probabilities.keySet()) {
                acc_point_beliefs.get(rssi_signal).put(room, room_probabilities.get(room));
            }
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

           // LinkedTreeMap<String, LinkedTreeMap<String, Double>> acc_point;
            if (!features.doesAccPointExist(rssi_signal.BSSID)) {
                continue;
            }
            i++;


            LinkedTreeMap<String, Float> cur_acc_point_belief = acc_point_beliefs.get(rssi_signal.BSSID);
            Float sum = 0.0f;

            for (String rooms : cur_acc_point_belief.keySet()) {
                Float cur_prob = cur_acc_point_belief.get(rooms);

                Float measurement = features.getProbability(rssi_signal.BSSID, rooms, Integer.toString(rssi_signal.level));
                if (measurement == null) {
                        measurement = 0.0f;
                }

                cur_acc_point_belief.put(rooms, (cur_prob * measurement));
                sum += cur_acc_point_belief.get(rooms);
            }
            if (sum != 0.0f) {
                for (String rooms : cur_acc_point_belief.keySet()) {
                    if (cur_acc_point_belief.get(rooms) != 0.0f) {
                        cur_acc_point_belief.put(rooms, cur_acc_point_belief.get(rooms) / sum);
                    }
                }
            } else {

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
/*        for (String acc_point : acc_point_beliefs.keySet()) {
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
        }*/
        boolean first = true;

        for (LinkedTreeMap<String, Float> room_belief : acc_point_beliefs.values()) {
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
        StringBuilder sb = new StringBuilder();
        sb.append("Current number of measurements: " + Integer.toString(number_of_measurements)+ "\n");
        for(String key : room_probabilities.keySet()) {
            sb.append(key);
            sb.append(": ");
            sb.append(room_probabilities.get(key));
            sb.append("\n");
        }
        result.setText(sb.toString());
    }

    private void normalize(LinkedTreeMap<String, Float> probs) {
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
