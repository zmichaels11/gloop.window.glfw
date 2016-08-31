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
 *
 * @author zmichaels
 */
public interface GLKeyListener {

    void keyActionPerformed(
            GLWindow window,
            int key, int scancode,
            GLKeyAction action,
            Set<GLKeyModifier> mods);

    default void glfwCallback(long hwnd, int key, int scancode, int action, int mods) {
        final GLWindow window = GLWindow.WINDOWS.get(hwnd);

        this.keyActionPerformed(
                window,
                key, scancode,
                GLKeyAction.valueOf(action),
                GLKeyModifier.parseModifiers(mods));
    }

    /**
     * Creates a new GLKeyListener for the specified key and modifier. When
     * triggererd, the window fullscreen mode is toggled.
     *
     * @param key the key to listen for.
     * @param mods the modifiers to listen for.
     * @return the GLKeyListener.
     * @since 16.08.31
     */
    public static GLKeyListener newFullscreenToggleListener(final int key, final Set<GLKeyModifier> mods) {
        return new GLKeyListener() {
            private boolean isFullscreen = false;

            @Override
            public void keyActionPerformed(GLWindow _window, int _key, int scancode, GLKeyAction _action, Set<GLKeyModifier> _mods) {
                if (_action == GLKeyAction.KEY_RELEASE) {
                    if (_key == key && _mods.containsAll(mods)) {
                        this.isFullscreen = !this.isFullscreen;
                        _window.setFullscreen(isFullscreen);
                    }
                }
            }

        };
    }
}
