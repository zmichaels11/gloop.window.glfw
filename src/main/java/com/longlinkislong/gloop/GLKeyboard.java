/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.longlinkislong.gloop;

import com.longlinkislong.gloop.GLException;
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
            throw new GLException("Invalid GLWindow!");
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
