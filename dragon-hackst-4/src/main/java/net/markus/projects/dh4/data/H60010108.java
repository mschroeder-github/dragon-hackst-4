
package net.markus.projects.dh4.data;

/**
 * 
 */
public class H60010108 extends HBDBlock {

    public int index;
    
    //always 5
    public int count;
    
    public int part;
    
    public int v12to16;
    
    //always 128
    public int v16to18; 
    
    //always 120
    public int v18to20; 
    
    public int v20to22;
    
    public int trailingFFstartPos;

    @Override
    public String toString() {
        return "H60010108{" + "index=" + index + ", count=" + count + ", part=" + part + ", v12to16=" + v12to16 + ", v16to18=" + v16to18 + ", v18to20=" + v18to20 + ", v20to22=" + v20to22 + '}';
    }
    
    
}
