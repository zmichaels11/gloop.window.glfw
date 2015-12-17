/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.longlinkislong.gloop;

/**
 *
 * @author zmichaels
 */
@FunctionalInterface
public interface GLMouseScrollListener {
    void mouseScrollActionPerformed(
            GLWindow window, 
            double xOffset, double yOffset);
    
    default void glfwScrollCallback(
            final long hwnd, 
            final double xOffset, final double yOffset) {
        
        final GLWindow window = GLWindow.WINDOWS.get(hwnd);
        
        this.mouseScrollActionPerformed(window, xOffset, yOffset);
    }
}
