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

    public class Gaussian {
        public Double mean;
        public Double variance;

        public Gaussian(Double mean, Double variance) {
            this.mean = mean;
            this.variance = variance;
        }
    }

    private HashMap<String, HashMap<String, Gaussian>> feature_map;

    public FeatureMapGauss(HashMap<String, HashMap<String, Gaussian>> gaussian_map, boolean to_be_ignored) {
        this.feature_map = gaussian_map;
    }

    public FeatureMapGauss(HashMap<String, HashMap<String, HashMap<String, Double>>> f_map) {
        feature_map = new HashMap<String, HashMap<String, Gaussian>>();

        for (String acc_point : f_map.keySet()) {
            feature_map.put(acc_point, new HashMap<String, Gaussian>());
            HashMap<String, Gaussian> cur_rooms = feature_map.get(acc_point);
            for (String room : f_map.get(acc_point).keySet()) {
                Mean m = new Mean();
                StandardDeviation dev = new StandardDeviation();
                for (Double prob : f_map.get(acc_point).get(room).values()) {
                    m.increment(prob);
                    dev.increment(prob);
                }

                Double mean = m.getResult();
                Double devi = dev.getResult();
                cur_rooms.put(room, new Gaussian(mean, devi));

            }
        }

    }

    @Override
    public Float getProbability(String bssid, String room, String level) {
        if (feature_map.containsKey(bssid)) {
            if (feature_map.get(bssid).containsKey(room)) {
                NormalDistribution d = new NormalDistribution(feature_map.get(bssid).get(room).mean.doubleValue(), feature_map.get(bssid).get(room).variance.doubleValue());
                return new Float(d.cumulativeProbability(Integer.parseInt(level)));
            }
        }
        return null;
    }

    @Override
    public boolean doesAccPointExist(String bssid) {
        return feature_map.containsKey(bssid);
    }
}
