/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.longlinkislong.gloop;

/**
 * The GLMousePositionListener is a functional interface that describes an
 * action to execute when the location of a mouse cursor changes.
 *
 * @author zmichaels
 * @since 15.06.07
 */
@FunctionalInterface
public interface GLMousePositionListener {

    /**
     * The function to execute every time that the location of the mouse cursor changes.
     * @param window the window that owns the GLMouse.
     * @param x the x-location of the mouse.
     * @param y the y-location of the mouse.
     * @since 15.06.07
     */
    void mousePositionActionPerformed(GLWindow window, double x, double y);

    /**
     * The GLFW callback that wraps the mousePositionActionPerformed function.
     * @param hwnd the handle for the window.
     * @param x the new x-location.
     * @param y the new y-location
     * @since 15.06.07
     */
    default void glfwCursorPosCallback(long hwnd, double x, double y) {
        final GLWindow window = GLWindow.WINDOWS.get(hwnd);

        this.mousePositionActionPerformed(window, x, y);
    }
}
