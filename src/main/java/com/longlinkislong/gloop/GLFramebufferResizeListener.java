/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.longlinkislong.gloop;

import com.longlinkislong.gloop.GLViewport;

/**
 *
 * @author zmichaels
 */
public interface GLFramebufferResizeListener {
    void framebufferResizedActionPerformed(GLWindow window, GLViewport viewport);
    default void glfwFramebufferResizeCallback(final long hwnd, final int width, final int height) {
        final GLWindow window = GLWindow.WINDOWS.get(hwnd);
        final GLViewport newport = new GLViewport(window.getGLThread(), 0, 0, width, height);
        
        this.framebufferResizedActionPerformed(window, newport);
    }
}
