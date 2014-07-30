package at.schuschu.android.rssilogger;

/**
 * Created by shinji on 7/30/2014.
 */
public interface FeatureMapInterface {
    public Float getProbability(String bssid, String room, String level);
    public boolean doesAccPointExist(String bssid);
}
