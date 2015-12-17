/* 
 * Copyright (c) 2015, Zachary Michaels
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
