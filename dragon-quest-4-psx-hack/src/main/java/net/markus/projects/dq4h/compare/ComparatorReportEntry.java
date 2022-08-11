
package net.markus.projects.dq4h.compare;

/**
 * An entry in a {@link ComparatorReport} if something differs.
 */
public class ComparatorReportEntry<T> {

    private T expectedObject;
    private Object expectedValue;
    
    private T actualObject;
    private Object actualValue;
    
    private String message;

    public ComparatorReportEntry() {
    }

    public ComparatorReportEntry(T expectedObject, Object expectedValue, T actualObject, Object actualValue) {
        this.expectedObject = expectedObject;
        this.expectedValue = expectedValue;
        this.actualObject = actualObject;
        this.actualValue = actualValue;
    }

    public ComparatorReportEntry(T expectedObject, Object expectedValue, T actualObject, Object actualValue, String message) {
        this.expectedObject = expectedObject;
        this.expectedValue = expectedValue;
        this.actualObject = actualObject;
        this.actualValue = actualValue;
        this.message = message;
    }

    public T getExpectedObject() {
        return expectedObject;
    }

    public void setExpectedObject(T expectedObject) {
        this.expectedObject = expectedObject;
    }

    public Object getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(Object expectedValue) {
        this.expectedValue = expectedValue;
    }

    public T getActualObject() {
        return actualObject;
    }

    public void setActualObject(T actualObject) {
        this.actualObject = actualObject;
    }

    public Object getActualValue() {
        return actualValue;
    }

    public void setActualValue(Object actualValue) {
        this.actualValue = actualValue;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ComparatorReportEntry{").append("\n");
        sb.append("expectedObject=").append(expectedObject).append(",\n");
        sb.append(" expectedValue=").append(expectedValue).append(",\n");
        sb.append("  actualObject=").append(actualObject).append(",\n");
        sb.append("   actualValue=").append(actualValue).append(",\n");
        sb.append("       message=").append(message).append("\n");
        sb.append('}');
        return sb.toString();
    }

    
    
    
}
