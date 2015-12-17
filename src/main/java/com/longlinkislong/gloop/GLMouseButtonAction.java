/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.longlinkislong.gloop;

import org.lwjgl.glfw.GLFW;

/**
 *
 * @author zmichaels
 */
public enum GLMouseButtonAction {
    PRESSED(GLFW.GLFW_PRESS),
    RELEASED(GLFW.GLFW_RELEASE);
    
    final int value;
    GLMouseButtonAction(final int value) {
        this.value = value;
    }
    
    public static GLMouseButtonAction valueOf(final int value) {
        for(GLMouseButtonAction action : values()) {
            if(action.value == value) {
                return action;
            }
        }
        
        return null;
    }
}
