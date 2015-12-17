/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.longlinkislong.gloop;

/**
 * A functional interface that describes an action to execute when a mouse
 * enters or exits the window.
 *
 * @author zmichaels
 * @since 15.06.07
 */
@FunctionalInterface
public interface GLMouseEnteredListener {

    /**
     * The function to call when a mouse cursor enters or exits the window.
     * @param window the window that owns the GLMouse.
     * @param status the new status.
     * @since 15.06.07
     */
    void mouseEnteredActionPerformed(
            final GLWindow window, 
            final GLMouseEnteredStatus status);
    
    /**
     * The GLFW callback that wraps mouseEnteredActionPerformed.
     * @param hwnd the window handle.
     * @param status the new status value.
     * @since 15.06.07
     */
    default void glfwCursorEnteredCallback(long hwnd, int status) {
        final GLWindow window = GLWindow.WINDOWS.get(hwnd);

        this.mouseEnteredActionPerformed(window, GLMouseEnteredStatus.valueOf(status));
    }
}
