/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
}
