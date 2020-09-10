package net.markus.projects.dh4.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 */
public class Histogram<T> {

    private int count = 0;
    private Map<Object, Integer> value2count;
    private Map<Object, List<T>> value2ses; //use second add for it

    public Histogram() {
        value2count = new HashMap<>();
        value2ses = new HashMap<>();
    }

    public static <T> Histogram fromList(List<T> l) {
        Histogram h = new Histogram();
        for (Object o : l) {
            h.add(o);
        }
        return h;
    }

    /**
     * Histogram of index of list to count of inner list.
     *
     * @param <T>
     * @param ll
     * @return
     */
    public static <T> Histogram fromListOfList(List<List<T>> ll) {
        Histogram h = new Histogram();
        int index = 0;
        for (List<T> l : ll) {
            for (Object o : l) {
                h.add(index);
            }
            index++;
        }
        return h;
    }

    public void add(Object value) {
        if (value2count.containsKey(value)) {
            value2count.put(value, value2count.get(value) + 1);
        } else {
            value2count.put(value, 1);
        }
        count++;
    }

    public void add(Object value, T relatedStringEntity) {
        add(value);
        if (value2ses.containsKey(value)) {
            value2ses.get(value).add(relatedStringEntity);
        } else {
            value2ses.put(value, new LinkedList<>(Arrays.asList(relatedStringEntity)));
        }
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean desc) {
        return toString((o1, o2) -> {
            return (desc ? -1 : 1) * Integer.compare(o1.getValue(), o2.getValue()); //sort by count
        });
    }

    public String toStringSortKey(boolean desc) {
        return toString((o1, o2) -> {
            Comparable c1 = (Comparable) o1.getKey();
            Comparable c2 = (Comparable) o2.getKey();
            return (desc ? -1 : 1) * c1.compareTo(c2);
        });
    }

    public String toString(Comparator<Entry<Object, Integer>> comparator) {
        StringBuilder sb = new StringBuilder();

        sb
                .append("count") //count
                
                .append("\t")
                .append("prop%") //proportion
                
                .append("\t")
                .append("key") //key
                
                .append("\n");

        value2count.entrySet().stream().sorted(comparator).forEach(e -> sb

                .append(e.getValue()) //count

                .append("\t")
                .append(String.format("%.3f%%", (e.getValue() / (double) count) * 100)) //proportion

                .append("\t")
                .append(normalize(e.getKey())) //key
                
                .append("\n")
        );

        sb.append("=\t=\n");
        sb.append(count + "\t100%\n");
        return sb.toString();
    }
    
    private String normalize(Object key) {
        if(key == null)
            return "null";
        return key.toString().replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r");
    }

    public Map<Object, Integer> getValue2Count() {
        return value2count;
    }
    
    public <T> List<T> top(int k, Class<T> type) {
        boolean desc = true;

        List<T> l = new ArrayList<>(k);

        value2count.entrySet().stream().sorted((o1, o2) -> {
            return (desc ? -1 : 1) * Integer.compare(o1.getValue(), o2.getValue()); //sort by count
        }).limit(k).forEach(e -> l.add((T) e.getKey()));

        return l;
    }

}
