package at.schuschu.android.rssilogger;

import java.util.HashMap;
//import org.apache.commons.math3.MathException;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;


/**
 * Created by shinji on 7/30/2014.
 */
public class FeatureMapGauss implements FeatureMapInterface {

    public HashMap<String, HashMap<String, Gaussian>> getFeature_map() {
        return feature_map;
    }

    public void setFeature_map(HashMap<String, HashMap<String, Gaussian>> feature_map) {
        this.feature_map = feature_map;
    }

    public class Gaussian {
        public Double mean;
        public Double variance;

        public Gaussian(Double mean, Double variance) {
            this.mean = mean;
            this.variance = variance;
        }
    }

    private HashMap<String, HashMap<String, Gaussian>> feature_map;

    public FeatureMapGauss(HashMap<String, HashMap<String, HashMap<String, Integer>>> f_map) {
        setFeature_map(new HashMap<String, HashMap<String, Gaussian>>());

        for (String acc_point : f_map.keySet()) {
            getFeature_map().put(acc_point, new HashMap<String, Gaussian>());
            HashMap<String, Gaussian> cur_rooms = getFeature_map().get(acc_point);
            for (String room : f_map.get(acc_point).keySet()) {
                Mean m = new Mean();
                StandardDeviation dev = new StandardDeviation();
                for (String level : f_map.get(acc_point).get(room).keySet()) {
                    for (int i = 0; i < f_map.get(acc_point).get(room).get(level); i++) {
                        m.increment(Integer.parseInt(level));
                        dev.increment(Integer.parseInt(level));
                    }
                }

                Double mean = m.getResult();
                Double devi = dev.getResult();
                cur_rooms.put(room, new Gaussian(mean, devi));

            }
        }

    }

    @Override
    public Float getProbability(String bssid, String room, String level) {
        if (getFeature_map().containsKey(bssid)) {
            if (getFeature_map().get(bssid).containsKey(room)) {
                NormalDistribution d = new NormalDistribution(getFeature_map().get(bssid).get(room).mean.doubleValue(), getFeature_map().get(bssid).get(room).variance.doubleValue());
                return new Float(d.cumulativeProbability(Integer.parseInt(level)));
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
        return getFeature_map().containsKey(bssid);
    }
}
