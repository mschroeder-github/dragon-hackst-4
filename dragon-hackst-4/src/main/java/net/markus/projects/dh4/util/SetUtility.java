package net.markus.projects.dh4.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class SetUtility {

    /**
     * pow(2, input.size) subsets.
     *
     * @param <T>
     * @param input
     * @return
     */
    public static <T> List<List<T>> subsetsAsList(List<T> input) {
        int allMasks = 1 << input.size();
        List<List<T>> output = new ArrayList<>();
        for (int i = 0; i < allMasks; i++) {
            List<T> sub = new ArrayList<>();
            for (int j = 0; j < input.size(); j++) {
                if ((i & (1 << j)) > 0) {
                    sub.add(input.get(j));
                }
            }
            output.add(sub);
        }
        return output;
    }

    public static <T> List<Set<T>> subsetsAsSet(List<T> input) {
        int allMasks = 1 << input.size();
        List<Set<T>> output = new ArrayList<>();
        for (int i = 0; i < allMasks; i++) {
            Set<T> sub = new HashSet<>();
            for (int j = 0; j < input.size(); j++) {
                if ((i & (1 << j)) > 0) {
                    sub.add(input.get(j));
                }
            }
            output.add(sub);
        }
        return output;
    }

    public static <T> Set<T> intersection(Set<T> a, Set<T> b) {
        a = new HashSet<>(a);
        a.retainAll(b);
        return a;
    }

    public static <T> Set<T> subtract(Set<T> a, Set<T> b) {
        a = new HashSet<>(a);
        a.removeAll(b);
        return a;
    }

    public static <T> Set<T> union(Set<T> a, Set<T> b) {
        a = new HashSet<>(a);
        a.addAll(b);
        return a;
    }

}
