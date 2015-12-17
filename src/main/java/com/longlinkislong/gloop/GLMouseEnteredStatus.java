/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.longlinkislong.gloop;

import org.lwjgl.opengl.GL11;

/**
 *
 * @author zmichaels
 */
public enum GLMouseEnteredStatus {

    ENTERED(GL11.GL_TRUE),
    EXITED(GL11.GL_FALSE);

    final int value;

    GLMouseEnteredStatus(final int value) {
        this.value = value;
    }
    
    public static GLMouseEnteredStatus valueOf(final int value) {
        for(GLMouseEnteredStatus status : values()) {
            if(status.value == value) {
                return status;
            }
        }
        
        return null;
    }
}
