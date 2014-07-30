package at.schuschu.android.rssilogger;

import java.util.HashMap;

/**
 * Created by shinji on 7/30/2014.
 */
public class FeatureMapLUT implements FeatureMapInterface {

    private HashMap<String, HashMap<String, HashMap<String, Double>>> feature_map;

    public FeatureMapLUT(HashMap<String, HashMap<String, HashMap<String, Double>>> feature_map) {
        this.feature_map = feature_map;
    }

    @Override
    public Float getProbability(String bssid, String room, String level) {
        if (feature_map.containsKey(bssid)) {
            if (feature_map.get(bssid).containsKey(room)) {
                return feature_map.get(bssid).get(room).get(level).floatValue();
            }
        }
        return null;
    }

    @Override
    public Object getFeatureMap() {
        return feature_map;
    }

    @Override
    public boolean doesAccPointExist(String bssid) {
        return feature_map.containsKey(bssid);
    }
}
