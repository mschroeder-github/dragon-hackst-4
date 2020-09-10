package net.markus.projects.dh4.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Stats for doubles.
 */
public class MinAvgMaxSdDouble {

    private double min = Double.MAX_VALUE;
    private double max = Double.MIN_VALUE;
    private double sum = 0;
    private double count = 0;
    private List<Double> ns;
    private Histogram histogram;

    public MinAvgMaxSdDouble() {
        ns = new ArrayList<>();
        histogram = new Histogram();
    }

    public void add(double n) {
        min = Math.min(n, min);
        max = Math.max(n, max);
        sum += n;
        count += 1;
        ns.add(n);//for variance
        histogram.add(n);
    }

    public static final String MIN = "min";
    public static final String MAX = "max";
    public static final String SUM = "sum";
    public static final String COUNT = "count";
    public static final String AVG = "avg";
    public static final String VAR = "var";
    public static final String STDDEV = "stddev";
    
    public static final List<String> METRICS = Arrays.asList(
            MIN, MAX, SUM, COUNT, AVG, VAR, STDDEV
    );
    
    public double get(String name) {
        switch(name) {
            case "min": return getMin();
            case "max": return getMax();
            case "sum": return getSum();
            case "count": return getCount();
            case "avg": return getAvg();
            case "var": return getVariance();
            case "stddev": return getStdDev();
        }
        throw new RuntimeException(name + " not found");
    }
    
    @Override
    public String toString() {
        return getCount() == 0 ? "no statistics b.c. count:0"
                : "min:" + min + "/avg:" + String.format(Locale.US, "%.3f", getAvg()) + "/max:" + max + "/s.d.:" + String.format(Locale.US, "%.3f", getStdDev()) + "/sum:" + getSum() + "/count:" + getCount();
    }
    
    public String toStringAvgSD(int decimalPoint) {
        return String.format(Locale.US, "%."+decimalPoint+"f", getAvg()) + "±" + String.format(Locale.US, "%."+decimalPoint+"f", getStdDev());
    }
    
    public String toStringAvgSDLatex(int decimalPoint) {
        return "$" + String.format(Locale.US, "%."+decimalPoint+"f", getAvg()) + "\\pm" + String.format(Locale.US, "%."+decimalPoint+"f", getStdDev()) + "$";
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getSum() {
        return sum;
    }

    public double getCount() {
        return count;
    }

    public double getAvg() {
        return sum / (double) count;
    }

    public double getVariance() {
        if (count <= 1) {
            return 0;
        }

        double mean = getAvg();
        double temp = 0;
        for (double a : ns) {
            temp += (a - mean) * (a - mean);
        }
        return temp / (float) (count - 1);
    }

    public double getStdDev() {
        return Math.sqrt(getVariance());
    }

    public String toCSV() {
        return min + "," + getAvg() + "," + max + "," + getStdDev();
    }

    public String toCSVHeader(String prefix) {
        return prefix + "-min" + "," + prefix + "-avg" + "," + prefix + "-max" + "," + prefix + "-stddev";
    }

    public Histogram getHistogram() {
        return histogram;
    }

    public void println(String prefix) {
        System.out.println(prefix + toString());
    }

    public void println(String prefix, String postfix) {
        System.out.println(prefix + toString() + postfix);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();

        m.put("count", getCount());

        if (count == 0) {
            return m;
        }

        m.put("min", getMin());
        m.put("avg", getAvg());
        m.put("max", getMax());
        m.put("sd", getStdDev());
        m.put("sum", getSum());

        return m;
    }

}
