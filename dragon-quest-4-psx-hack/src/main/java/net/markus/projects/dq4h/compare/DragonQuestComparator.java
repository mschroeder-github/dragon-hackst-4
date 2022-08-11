package net.markus.projects.dq4h.compare;

/**
 * An interface to compare two objects in dragon quest data.
 * @param <T> type of the object to compare
 */
public interface DragonQuestComparator<T> {
    
    /**
     * Compares this object with another object and reports differences.
     * @param other
     * @param report 
     */
    public void compare(T other, ComparatorReport report);
    
}
