/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.longlinkislong.gloop;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.lwjgl.glfw.GLFW;

/**
 *
 * @author zmichaels
 */
public enum GLKeyModifier {
    SHIFT(GLFW.GLFW_MOD_SHIFT),
    CONTROL(GLFW.GLFW_MOD_CONTROL),
    ALT(GLFW.GLFW_MOD_ALT),
    SUPER(GLFW.GLFW_MOD_SUPER);
        final int value;

    GLKeyModifier(final int value) {
        this.value = value;
    }
    
    public static GLKeyModifier valueOf(final int value) {
        for(GLKeyModifier mod : values()) {
            if(mod.value == value) {
                return mod;
            }
        }
        
        return null;
    }
    
    public static Set<GLKeyModifier> parseModifiers(final int value) {
        final Set<GLKeyModifier> modSet = new HashSet<>();
        
        for(GLKeyModifier mod : values()) {
            if((value & mod.value) == mod.value) {
                modSet.add(mod);
            }
        }
        
        return Collections.unmodifiableSet(modSet);
    }
}
