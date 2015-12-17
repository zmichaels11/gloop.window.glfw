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
public interface GLKeyCharListener {
    void charTypePerformed(GLWindow window, char charCode);
    
    default void glfwCharCallback(long hwnd, int charCode) {
        this.charTypePerformed(GLWindow.WINDOWS.get(hwnd), (char) charCode);
    }
}
