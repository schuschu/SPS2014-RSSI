package at.schuschu.android.rssilogger;

//import org.apache.commons.math3.MathException;
import android.util.Log;

import com.google.gson.internal.LinkedTreeMap;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;


/**
 * Created by shinji on 7/30/2014.
 */
public class FeatureMapGauss implements FeatureMapInterface {

    public LinkedTreeMap<String, LinkedTreeMap<String, Gaussian>> getFeature_map() {
        return feature_map;
    }

    public void setFeature_map(LinkedTreeMap<String, LinkedTreeMap<String, Gaussian>> feature_map) {
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

    private LinkedTreeMap<String, LinkedTreeMap<String, Gaussian>> feature_map;

    public FeatureMapGauss(LinkedTreeMap<String, LinkedTreeMap<String, LinkedTreeMap<String, Double>>> f_map) {
        setFeature_map(new LinkedTreeMap<String, LinkedTreeMap<String, Gaussian>>());

        for (String acc_point : f_map.keySet()) {
            getFeature_map().put(acc_point, new LinkedTreeMap<String, Gaussian>());
            LinkedTreeMap<String, Gaussian> cur_rooms = getFeature_map().get(acc_point);
            for (String room : f_map.get(acc_point).keySet()) {
                Mean m = new Mean();
                StandardDeviation dev = new StandardDeviation();
                for (String level : f_map.get(acc_point).get(room).keySet()) {
                    Log.i("serialz", Double.toString(f_map.get(acc_point).get(room).get(level)));
                    for (Integer i = 0; i < f_map.get(acc_point).get(room).get(level); i++) {
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
