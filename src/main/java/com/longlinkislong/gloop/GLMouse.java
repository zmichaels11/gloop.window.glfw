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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.lwjgl.glfw.GLFW;

/**
 * The mouse associated with a window.
 *
 * @author zmichaels
 * @since 15.06.07
 */
public class GLMouse implements GLMouseEnteredListener, GLMousePositionListener, GLMouseButtonListener, GLMouseScrollListener {

    private final GLWindow window;
    private final List<GLMouseEnteredListener> mouseEnteredListeners = new ArrayList<>();
    private final List<GLMousePositionListener> mousePositionListeners = new ArrayList<>();
    private final List<GLMouseButtonListener> mouseButtonListeners = new ArrayList<>();
    private final List<GLMouseScrollListener> mouseScrollListeners = new ArrayList<>();        

    protected GLMouse(final GLWindow window) {
        this.window = window;
    }

    /**
     * Sets the cursor position of the mouse.
     *
     * @param x the new x-location
     * @param y the new y-location
     * @throws GLFWException if the window is not initialized.
     * @since 15.06.07
     */
    public void setMousePosition(final double x, final double y)
            throws GLFWException {

        if (!this.window.isValid()) {
            throw new GLFWException("Invalid window!");
        }

        GLFW.glfwSetCursorPos(this.window.window, x, y);
    }

    @Override
    public void mouseButtonActionPerformed(
            final GLWindow window,
            final int button,
            final GLMouseButtonAction action,
            final Set<GLKeyModifier> modifiers) {

        this.mouseButtonListeners
                .forEach(
                        l -> l.mouseButtonActionPerformed(
                                window,
                                button,
                                action,
                                modifiers));
    }    

    /**
     * Retrieves the current cursor position
     *
     * @return the current cursor position
     * @throws GLFWException if the window is not initialized.
     * @since 15.06.07
     */
    public GLVec2D getMousePosition() throws GLFWException {
        if (!this.window.isValid()) {
            throw new GLFWException("Invalid GLWindow!");
        }

        final ByteBuffer xPos = NativeTools.getInstance().nextDWord();
        final ByteBuffer yPos = NativeTools.getInstance().nextDWord();

        GLFW.glfwGetCursorPos(this.window.window, xPos, yPos);

        return GLVec2D.create(xPos.getDouble(), yPos.getDouble());

    }

    /**
     * Retrieves the last set value of the specified mouse button.
     *
     * @param button the mouse button
     * @return the value
     * @throws GLFWException if the window is not initialized.
     * @since 15.06.07
     */
    public GLMouseButtonAction getMouseButton(final int button)
            throws GLFWException {

        if (!this.window.isValid()) {
            throw new GLFWException("Invalid GLWindow!");
        }

        return GLMouseButtonAction.valueOf(
                GLFW.glfwGetMouseButton(this.window.window, button));
    }

    /**
     * Adds a GLMouseScrollListener to the mouse object.
     * @param listener the listener to add
     * @return true if the listener was added
     * @since 15.06.05
     */
    public boolean addScrollListener(final GLMouseScrollListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null!");
        return this.mouseScrollListeners.add(listener);
    }
    
    /**
     * Attempts to remove the GLMouseScrollListener from the GLMouse object.
     * @param listener the listener to remove.
     * @return true if the listener was removed.
     * @since 15.06.05
     */
    public boolean removeScrollListener(final GLMouseScrollListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null!");
        return this.mouseScrollListeners.remove(listener);
    }        
    
    /**
     * Adds a GLMouseEnteredListener to the mouse object.
     *
     * @param listener the listener.
     * @return true if the listener was added.
     * @since 15.06.07
     */
    public boolean addEnteredListener(final GLMouseEnteredListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null!");
        return this.mouseEnteredListeners.add(listener);
    }

    /**
     * Attempts to remove the listener from the mouse object.
     *
     * @param listener the listener to remove.
     * @return true if the listener was removed.
     * @since 15.06.07
     */
    public boolean removeEnteredListener(final GLMouseEnteredListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null!");
        return this.mouseEnteredListeners.remove(listener);
    }

    /**
     * Removes all attached listeners from the GLMouse object.
     *
     * @since 15.06.07
     */
    public void clearListeners() {
        this.mouseEnteredListeners.clear();
        this.mouseButtonListeners.clear();
        this.mousePositionListeners.clear();
        this.mouseScrollListeners.clear();
    }

    /**
     * Adds a GLMousePositionListener to the GLMouse.
     *
     * @param listener the listener to add.
     * @return true if the listener was added.
     * @since 15.06.07
     */
    public boolean addPositionListener(final GLMousePositionListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null!");
        return this.mousePositionListeners.add(listener);
    }

    /**
     * Attempts to remove a GLMousePositionListener from the GLMouse.
     *
     * @param listener the listener to remove.
     * @return true if the listener was removed.
     * @since 15.06.07
     */
    public boolean removePositionListener(final GLMousePositionListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null!");
        return this.mousePositionListeners.remove(listener);
    }

    /**
     * Adds a GLMouseButtonListener to the GLMouse.
     *
     * @param listener the listener to add.
     * @return true if the listener was added.
     * @since 15.06.07
     */
    public boolean addButtonListener(final GLMouseButtonListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null!");
        return this.mouseButtonListeners.add(listener);
    }

    /**
     * Removes a GLMouseButtonListener from the GLMouse.
     *
     * @param listener the listener to remove.
     * @return true if the listener was removed.
     * @since 15.06.07
     */
    public boolean removeButtonListener(final GLMouseButtonListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null!");
        return this.mouseButtonListeners.remove(listener);
    }

    @Override
    public void mouseEnteredActionPerformed(
            final GLWindow window, final GLMouseEnteredStatus status) {

        this.mouseEnteredListeners
                .forEach(
                        l -> l.mouseEnteredActionPerformed(window, status));
    }

    @Override
    public void mousePositionActionPerformed(
            final GLWindow window, final double x, final double y) {

        this.mousePositionListeners.forEach(
                l -> l.mousePositionActionPerformed(window, x, y));
    }

    @Override
    public void mouseScrollActionPerformed(GLWindow window, double xOffset, double yOffset) {
        this.mouseScrollListeners.forEach(
                l -> l.mouseScrollActionPerformed(window, xOffset, yOffset));
    }

}
