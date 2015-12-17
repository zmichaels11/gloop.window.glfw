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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.lwjgl.glfw.GLFW;

/**
 *
 * @author zmichaels
 */
public class GLKeyboard implements GLKeyListener, GLKeyCharListener {

    private final List<GLKeyListener> keyListeners = new ArrayList<>();
    private final List<GLKeyCharListener> charListeners = new ArrayList<>();
    private final GLWindow window;

    protected GLKeyboard(final GLWindow window) {
        this.window = window;
    }

    public void addKeyListener(final GLKeyListener listener) {
        this.keyListeners.add(listener);
    }

    public void addCharListener(final GLKeyCharListener listener) {
        this.charListeners.add(listener);
    }

    public boolean removeKeyListener(final GLKeyListener listener) {
        return this.keyListeners.remove(listener);
    }

    public boolean removeCharListener(final GLKeyCharListener listener) {
        return this.charListeners.remove(listener);
    }

    public void addAllKeyListeners(final Collection<? extends GLKeyListener> listeners) {
        this.keyListeners.addAll(listeners);
    }

    public void addAllCharListeners(final Collection<? extends GLKeyCharListener> listeners) {
        this.charListeners.addAll(listeners);
    }

    public void removeAllKeyListeners() {
        this.keyListeners.clear();
    }

    public void removeAllCharListeners() {
        this.charListeners.clear();
    }

    public List<GLKeyListener> getKeyListeners() {
        return Collections.unmodifiableList(this.keyListeners);
    }

    public List<GLKeyCharListener> getCharListeners() {
        return Collections.unmodifiableList(this.charListeners);
    }

    public GLKeyAction getKey(final int keyId) {
        if (!this.window.isValid()) {
            throw new GLFWException("Invalid GLWindow!");
        }

        return GLKeyAction.valueOf(GLFW.glfwGetKey(this.window.window, keyId));
    }

    @Override
    public void keyActionPerformed(GLWindow window, int key, int scancode, GLKeyAction action, Set<GLKeyModifier> mods) {
        this.keyListeners.forEach(listener -> listener.keyActionPerformed(window, key, scancode, action, mods));        
    }

    @Override
    public void charTypePerformed(GLWindow window, char charCode) {
        this.charListeners.forEach(listener -> listener.charTypePerformed(window, charCode));
    }

}
