/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.longlinkislong.gloop;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.lwjgl.glfw.GLFW;

/**
 * A wrapper object that exposes the GLFW gamepad as an object.
 *
 * @author zmichaels
 * @since 15.11.11
 */
public class GLGamepad {

    private final String name;
    private final int id;
    private final float[] axes;
    private final GLGamepadState buttons[];
    private final int buttonCount;
    private final int axesCount;

    protected GLGamepad(final int id) {
        this.name = GLFW.glfwGetJoystickName(this.id = id);
        final FloatBuffer axesData = GLFW.glfwGetJoystickAxes(this.id);
        final ByteBuffer buttonData = GLFW.glfwGetJoystickButtons(this.id);

        axesData.get(this.axes = new float[axesData.limit()]);
        this.buttonCount = buttonData.limit();
        this.axesCount = this.axes.length;

        this.buttons = new GLGamepadState[this.buttonCount];

        for (int i = 0; i < this.buttonCount; i++) {
            this.buttons[i] = GLGamepadState.valueOf(buttonData.get(i));
        }
    }

    /**
     * Retrieves the name of the gamepad.
     *
     * @return the name.
     * @since 15.11.11
     */
    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return "Gamepad[" + this.id + "]: name=" + this.name;
    }

    /**
     * Retrieves the number of buttons.
     *
     * @return the number of buttons.
     * @since 15.11.11
     */
    public int getButtonCount() {
        return this.buttonCount;
    }

    /**
     * Retrieves the number of axes.
     *
     * @return the number of axes.
     * @since 15.11.11
     */
    public int getAxesCount() {
        return this.axesCount;
    }

    /**
     * Retrieves the state of the specified button.
     *
     * @param buttonId the button to check.
     * @return the state of the button.
     * @since 15.11.11
     */
    public GLGamepadState getButtonState(final int buttonId) {
        return this.buttons[buttonId];
    }

    /**
     * Requests how far a single axis is pushed.
     *
     * @param axes the axis to check.
     * @return value between [-1.0, 1.0] that indicates the direction and
     * magnitude of an analog axis.
     * @since 15.11.11
     */
    public float getAxesState(final int axes) {
        return this.axes[axes];
    }

    /**
     * Requests that the gamepad is polled.
     *
     * @since 15.11.11
     */
    public final void update() {
        final FloatBuffer axesData = GLFW.glfwGetJoystickAxes(this.id);
        final ByteBuffer buttonData = GLFW.glfwGetJoystickButtons(this.id);

        axesData.get(this.axes);
        for (int i = 0; i < this.buttonCount; i++) {
            this.buttons[i] = GLGamepadState.valueOf(buttonData.get(i));
        }
    }
}
