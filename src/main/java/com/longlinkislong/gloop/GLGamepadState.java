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
public enum GLGamepadState {
    PRESSED(GLFW.GLFW_PRESS),
    RELEASED(GLFW.GLFW_RELEASE);
    
    final int value;
    
    GLGamepadState(final int value) {
        this.value = value;
    }
    
    public static GLGamepadState valueOf(final int value) {
        for(GLGamepadState state : values()) {
            if(state.value == value) {
                return state;
            }
        }
        
        return null;
    }
}
