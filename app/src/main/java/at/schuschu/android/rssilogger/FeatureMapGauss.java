package at.schuschu.android.rssilogger;

//import org.apache.commons.math3.MathException;
import android.util.Log;

import com.google.gson.internal.LinkedTreeMap;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
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
        StringBuilder sb = new StringBuilder();
        for (String acc_point : f_map.keySet()) {
            sb.append("===AP: ").append(acc_point).append("===\n");
            getFeature_map().put(acc_point, new LinkedTreeMap<String, Gaussian>());
            LinkedTreeMap<String, Gaussian> cur_rooms = getFeature_map().get(acc_point);
            for (String room : f_map.get(acc_point).keySet()) {
                sb.append(room).append(": ");
                Integer sum = 0;
                Integer number_of_measurements = 0;
                Mean m = new Mean();
                StandardDeviation dev = new StandardDeviation();
                for (String level : f_map.get(acc_point).get(room).keySet()) {
                    sb.append(level).append("*").append(f_map.get(acc_point).get(room).get(level)).append(" ");
                    for (Integer i = 0; i < f_map.get(acc_point).get(room).get(level); i++) {
                        m.increment(Integer.parseInt(level));
                        dev.increment(Integer.parseInt(level));
                        sum += Integer.parseInt(level);
                        number_of_measurements++;
                    }
                }
                Double mean2 = sum.doubleValue()/number_of_measurements.doubleValue();
                Double mean = m.getResult();
                Double devi = dev.getResult();
                if(devi==0){
                    devi=0.1;
                }
                sb.append(" results in u=").append(mean).append("(").append(mean2).append(") ").append(" s=").append(devi);
                cur_rooms.put(room, new Gaussian(mean, devi));
                Log.i("GAUSS", sb.toString());
                sb = new StringBuilder();

            }
            sb.append("\n");
        }
    }

    @Override
    public Float getProbability(String bssid, String room, String level) {
        if (getFeature_map().containsKey(bssid)) {
            if (getFeature_map().get(bssid).containsKey(room)) {
                try {
                    NormalDistribution d = new NormalDistribution(getFeature_map().get(bssid).get(room).mean.doubleValue(), getFeature_map().get(bssid).get(room).variance.doubleValue());
                    Log.i("GETPROP",bssid+ " in room " + room + " at " + level + " results in " + d.cumulativeProbability(Integer.parseInt(level)));
                    return new Float(d.cumulativeProbability(Integer.parseInt(level)));
                } catch (NotStrictlyPositiveException e) {
                    return 0.0f;
                }
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
