
package net.markus.projects.dq4h.gui;

/**
 * Listens for progress and presents it in the GUI.
 */
public interface ProjectListener {

    /**
     * Shows the text in a progress bar.
     * @param text 
     */
    void setProgressText(String text);
    
    /**
     * Sets the maximum step for the progress.
     * @param max 
     */
    void setProgressMax(int max);
    
    /**
     * Sets the current step in the progress.
     * @param value 
     */
    void setProgressValue(int value);
    
}
