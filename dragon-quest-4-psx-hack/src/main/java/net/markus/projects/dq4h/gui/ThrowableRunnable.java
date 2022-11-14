
package net.markus.projects.dq4h.gui;

/**
 * A runnable interface which is able to throw an exception.
 * We use this to catch the exception and show it in the GUI.
 */
@FunctionalInterface
public interface ThrowableRunnable {
    void run() throws Exception;
}
