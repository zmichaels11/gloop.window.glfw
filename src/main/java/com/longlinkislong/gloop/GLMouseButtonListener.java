/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.longlinkislong.gloop;

import java.util.Set;

/**
 * A functional interface that describes an action to execute after the state of
 * a mouse button changes.
 *
 * @author zmichaels
 * @since 15.06.07
 */
@FunctionalInterface
public interface GLMouseButtonListener {

    /**
     * The function to execute when the state of a mouse button changes.
     * @param window the window that owns the GLMouse object.
     * @param button the button involved.
     * @param action the new state of the button.
     * @param modifiers any key modifiers that are also pressed.
     * @since 15.06.07
     */
    void mouseButtonActionPerformed(
            GLWindow window,
            int button,
            GLMouseButtonAction action,
            Set<GLKeyModifier> modifiers);

    /**
     * The GLFW callback that wraps mouseButtonActionPerformed.
     * @param hwnd the handle to the window that owns the mouse.
     * @param button the button involved.
     * @param action the new button status value.
     * @param mods a bitfield containing all modifiers pressed.
     * @since 15.06.07
     */
    default void glfwMouseButtonCallback(long hwnd, int button, int action, int mods) {
        final GLWindow window = GLWindow.WINDOWS.get(hwnd);

        this.mouseButtonActionPerformed(
                window,
                button,
                GLMouseButtonAction.valueOf(action),
                GLKeyModifier.parseModifiers(mods));
    }
}
