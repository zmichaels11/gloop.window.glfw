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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.lwjgl.glfw.GLFW;
import static org.lwjgl.glfw.GLFW.GLFW_ALPHA_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_BLUE_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_DEPTH_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_GREEN_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RED_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_REFRESH_RATE;
import static org.lwjgl.glfw.GLFW.GLFW_SAMPLES;
import static org.lwjgl.glfw.GLFW.GLFW_STENCIL_BITS;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWCursorEnterCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.system.MemoryUtil.NULL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.runouw.util.Lazy;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * A GLWindow represents a window that handles OpenGL drawing.
 *
 * @author zmichaels
 * @since 15.06.24
 */
public class GLWindow {

    private static final Logger LOGGER = LoggerFactory.getLogger("GLWindow");
    private static final Logger GLFW_LOGGER = LoggerFactory.getLogger("GLFW");
    private static final Marker GLFW_MARKER = MarkerFactory.getMarker("GLFW");
    private static final Marker GLOOP_MARKER = MarkerFactory.getMarker("GLOOP");

    public static final int OPENGL_VERSION_MAJOR;
    public static final int OPENGL_VERSION_MINOR;
    public static final int OPENGL_SWAP_INTERVAL;
    public static final int OPENGL_SAMPLES;
    public static final int OPENGL_RED_BITS;
    public static final int OPENGL_BLUE_BITS;
    public static final int OPENGL_GREEN_BITS;
    public static final int OPENGL_ALPHA_BITS;
    public static final int OPENGL_DEPTH_BITS;
    public static final int OPENGL_STENCIL_BITS;
    public static final int OPENGL_REFRESH_RATE;

    private static final long INVALID_WINDOW_ID = -1L;
    protected volatile long window = INVALID_WINDOW_ID;
    private final int width;
    private final int height;
    private final String title;
    private GLThread thread = null;
    private final GLWindow shared;

    protected final List<GLKeyListener> keyListeners = new ArrayList<>();
    protected final List<GLMousePositionListener> mousePositionListeners = new ArrayList<>();
    protected final List<GLMouseButtonListener> mouseButtonListeners = new ArrayList<>();
    protected final List<GLMouseEnteredListener> mouseEnteredListeners = new ArrayList<>();
    protected final List<GLMouseScrollListener> mouseScrollListeners = new ArrayList<>();
    protected final List<GLKeyCharListener> charListeners = new ArrayList<>();

    private final Lazy<GLFWCharCallback> charCallback = new Lazy<>(() -> {
        final GLFWCharCallback callback = GLFWCharCallback.create((hwnd, charCode) -> {
            this.charListeners.forEach(listener -> listener.glfwCharCallback(hwnd, charCode));
        });

        LOGGER.trace(GLFW_MARKER, "GLWindow[{}].charCallback is initialized!", GLWindow.this.title);
        return callback;
    });

    private final Lazy<GLFWKeyCallback> keyCallback = new Lazy<>(() -> {
        final GLFWKeyCallback callback = GLFWKeyCallback.create((hwnd, key, scancode, action, mods) -> {
            keyListeners.forEach(listener -> listener.glfwCallback(hwnd, key, scancode, action, mods));
        });

        LOGGER.trace(GLFW_MARKER, "GLWindow[{}].keyCallback is initialized!", GLWindow.this.title);
        return callback;
    });

    private final Lazy<GLFWMouseButtonCallback> mouseButtonCallback = new Lazy<>(() -> {
        final GLFWMouseButtonCallback callback = GLFWMouseButtonCallback.create((hwnd, button, action, mods) -> {
            this.mouseButtonListeners.forEach(listener -> listener.glfwMouseButtonCallback(hwnd, button, action, mods));
        });

        LOGGER.trace(GLFW_MARKER, "GLWindow[{}].mouseButtonCallback is initialized!", GLWindow.this.title);
        return callback;
    });

    private final Lazy<GLFWCursorPosCallback> cursorPosCallback = new Lazy<>(() -> {
        final GLFWCursorPosCallback callback = GLFWCursorPosCallback.create((hwnd, x, y) -> {
            this.mousePositionListeners.forEach(listener -> listener.glfwCursorPosCallback(hwnd, x, y));
        });

        LOGGER.trace(GLFW_MARKER, "GLWindow[{}].cursorPosCallback is initialized!", GLWindow.this.title);
        return callback;
    });

    private final Lazy<GLFWScrollCallback> scrollCallback = new Lazy<>(() -> {
        final GLFWScrollCallback callback = GLFWScrollCallback.create((hwnd, x, y) -> {
            this.mouseScrollListeners.forEach(listener -> listener.glfwScrollCallback(hwnd, x, y));
        });

        LOGGER.trace(GLFW_MARKER, "GLWindow[{}].scrollCallback is initialized!", GLWindow.this.title);
        return callback;
    });

    private final Lazy<GLFWCursorEnterCallback> cursorEnterCallback = new Lazy<>(() -> {
        final GLFWCursorEnterCallback callback = GLFWCursorEnterCallback.create((hwnd, status) -> {
            this.mouseEnteredListeners.forEach(listener -> listener.glfwCursorEnteredCallback(hwnd, status));
        });

        LOGGER.trace(GLFW_MARKER, "GLWindow[{}].cursorEnterCallback is initialized!", GLWindow.this.title);
        return callback;
    });

    private Optional<GLFWFramebufferSizeCallback> resizeCallback = Optional.empty();
    private Optional<Runnable> onClose = Optional.empty();
    private final long monitor;
    private volatile boolean hasInitialized = false;
    private final List<Runnable> cleanupTasks = new ArrayList<>();

    protected static final Map<Long, GLWindow> WINDOWS = new TreeMap<>(Long::compareTo);
    private static final List<GLGamepad> GAMEPADS;

    static {
        NativeTools.getInstance().autoLoad();
        final String glVersion = System.getProperty("gloop.opengl.version", "3.2");
        OPENGL_VERSION_MAJOR = Integer.parseInt(glVersion.substring(0, glVersion.indexOf(".")));
        OPENGL_VERSION_MINOR = Integer.parseInt(glVersion.substring(glVersion.indexOf(".") + 1));
        OPENGL_REFRESH_RATE = Integer.getInteger("gloop.opengl.refresh_rate", -1);
        OPENGL_SWAP_INTERVAL = Integer.getInteger("gloop.opengl.swap_interval", 1);
        OPENGL_SAMPLES = Integer.getInteger("gloop.opengl.msaa", -1);
        OPENGL_RED_BITS = Integer.getInteger("gloop.opengl.red_bits", 8);
        OPENGL_GREEN_BITS = Integer.getInteger("gloop.opengl.green_bits", 8);
        OPENGL_BLUE_BITS = Integer.getInteger("gloop.opengl.blue_bits", 8);
        OPENGL_ALPHA_BITS = Integer.getInteger("gloop.opengl.alpha_bits", 8);
        OPENGL_DEPTH_BITS = Integer.getInteger("gloop.opengl.depth_bits", 24);
        OPENGL_STENCIL_BITS = Integer.getInteger("gloop.opengl.stencil_bits", 8);

        if (GLFW.glfwInit() != GL_TRUE) {
            throw new GLFWException("Could not initialize GLFW!");
        } else {
            LOGGER.trace(GLFW_MARKER, "GLFW successfully initialized!");
        }

        final GLFWErrorCallback errCallback = GLFWErrorCallback.create((error, desc) -> {
            final String msg = GLFWErrorCallback.getDescription(desc);

            GLFW_LOGGER.error(GLFW_MARKER, "GLFW Error #{}: {}", error, msg);
        });

        GLFW_LOGGER.trace(GLFW_MARKER, "glfwSetErrorCallback({})", errCallback);

        GLFW.glfwSetErrorCallback(errCallback);

        final List<GLGamepad> gamepads = new ArrayList<>();
        for (int i = 0; i < GLFW.GLFW_JOYSTICK_LAST; i++) {
            if (GLFW.glfwJoystickPresent(i) == GL_TRUE) {
                final GLGamepad gamepad = new GLGamepad(i);

                gamepads.add(gamepad);
                LOGGER.trace(GLFW_MARKER, "Registered gamepad: {}!", gamepad.getName());
            }
        }

        GAMEPADS = Collections.unmodifiableList(gamepads);
    }

    /**
     * Retrieves the list of gamepads
     *
     * @return the list of gamepads
     * @since 15.06.07
     */
    public static List<GLGamepad> listGamepads() {
        return GAMEPADS;
    }

    /**
     * Returns a list of active GLWindow objects.
     *
     * @return the list of windows.
     * @since 15.06.07
     */
    public static List<GLWindow> listActiveWindows() {
        final List<GLWindow> windows = new ArrayList<>();

        windows.addAll(WINDOWS.values());

        return Collections.unmodifiableList(windows);
    }

    /**
     * Registers a callback to run when the window is closed.
     *
     * @param onCloseCallback the callback to run
     * @since 15.06.24
     */
    public void setOnClose(final Runnable onCloseCallback) {
        LOGGER.trace(GLFW_MARKER, "GLWindow[{}].onClose = {}", this.title, onCloseCallback);
        this.onClose = Optional.ofNullable(onCloseCallback);
    }

    /**
     * Constructs a new GLWindow with all default parameters.
     *
     * @since 15.06.07
     */
    public GLWindow() {
        this(640, 480, "GLOOP App", null);
    }

    /**
     * Constructs a new GLWindow with the supplied size, default title, and no
     * shared context.
     *
     * @param width the width of the window
     * @param height the height of the window
     * @since 15.06.07
     */
    public GLWindow(final int width, final int height) {
        this(width, height, "GLOOP App", null);
    }

    /**
     * Constructs a new GLWindow with the supplied size, title, and no shared
     * context.
     *
     * @param width the width of the window
     * @param height the height of the window
     * @param title the title for the window
     * @since 15.06.07
     */
    public GLWindow(
            final int width, final int height,
            final CharSequence title) {

        this(width, height, title, null);
    }

    /**
     * Constructs a new GLWindow with the supplied size, title, and shared
     * context.
     *
     * @param width the width of the window
     * @param height the height of the window
     * @param title the title of the window
     * @param shared the window to retrieve a context from for sharing. Null
     * specifies no shared context.
     * @since 15.06.07
     */
    public GLWindow(
            final int width, final int height,
            final CharSequence title,
            final GLWindow shared) {

        this.width = width;
        this.height = height;
        this.title = title.toString();
        this.shared = shared;

        this.thread = GLThread.create();

        LOGGER.debug(GLFW_MARKER, "Constructed GLWindow! [width={}, height={}, title={}, parent={}]", width, height, title, shared != null ? shared.title : "null");
        LOGGER.trace(GLFW_MARKER, "Constructed GLWindow.thread = {}", this.thread);

        this.thread.submitGLTask(new InitTask());
        this.monitor = NULL;
    }

    /**
     * Checks if the GLWindow is valid. A window is determined to be valid if it
     * has been initialized and not closed.
     *
     * @return true if the window is initialized.
     * @since 15.06.07
     */
    public boolean isValid() {
        return (this.hasInitialized && this.window != INVALID_WINDOW_ID);
    }

    private final Lazy<GLMouse> mouse = new Lazy<>(() -> {
        final GLMouse ms = new GLMouse(this);

        this.mouseButtonListeners.add(ms);
        this.mouseScrollListeners.add(ms);
        this.mousePositionListeners.add(ms);
        this.mouseEnteredListeners.add(ms);

        LOGGER.trace(GLFW_MARKER, "GLWindow[{}].mouse is initialized!", GLWindow.this.title);

        return ms;
    });

    /**
     * Retrieves the mouse object associated with the window. A GLWindow object
     * may own up to one mouse object.
     *
     * @return the mouse object.
     * @throws GLFWException if the window is not initialized.
     * @see
     * <a href="http://www.glfw.org/docs/latest/input.html#input_mouse">GLFW
     * Mouse Input</a>
     * @since 15.06.24
     */
    public GLMouse getMouse() throws GLFWException {
        return new MouseQuery().glCall(this.getGLThread());
    }

    /**
     * A GLQuery that requests for the Mouse object.
     *
     * @since 15.06.30
     */
    public class MouseQuery extends GLQuery<GLMouse> {

        @Override
        public GLMouse call() throws Exception {
            if (!GLWindow.this.isValid()) {
                throw new GLFWException("GLWindow is not valid!");
            }

            return GLWindow.this.mouse.get();
        }
    }

    //private Optional<GLKeyboard> keyboard = Optional.empty();
    private final Lazy<GLKeyboard> keyboard = new Lazy<>(() -> {
        final GLKeyboard kb = new GLKeyboard(this);

        this.keyListeners.add(kb);
        this.charListeners.add(kb);

        LOGGER.trace(GLFW_MARKER, "GLWindow[{}].keyboard is initialized!", GLWindow.this.title);

        return kb;
    });

    /**
     * Retrieves the keyboard object associated with the window.
     *
     * @return the keyboard object
     * @throws GLFWException if the window is not initialized.
     * @see
     * <a href="http://www.glfw.org/docs/latest/input.html#input_keyboard">GLFW
     * Keyboard Input</a>
     * @since 15.06.07
     */
    public GLKeyboard getKeyboard() throws GLFWException {
        return new KeyboardQuery().glCall(this.getGLThread());
    }

    /**
     * A GLQuery that requests for the GLKeyboard object.
     *
     * @since 15.06.30
     */
    public class KeyboardQuery extends GLQuery<GLKeyboard> {

        @Override
        public GLKeyboard call() throws Exception {
            if (!GLWindow.this.isValid()) {
                throw new GLFWException("Invalid GLWindow!");
            }

            return GLWindow.this.keyboard.get();
        }

    }

    /**
     * Sets the clipboard string.
     *
     * @param seq the string to set
     * @throws GLFWException if the window is not initialized.
     * @see <a href="http://www.glfw.org/docs/latest/input.html#clipboard">GLFW
     * Clipboard Input and Output</a>
     * @since 15.06.07
     */
    public void setClipboardString(final CharSequence seq) throws GLFWException {
        if (!this.isValid()) {
            throw new GLFWException("Invalid GLWindow!");
        }

        GLFW_LOGGER.trace(GLFW_MARKER, "glfwSetClipboardString({}, {})", this.window, seq);
        GLFW.glfwSetClipboardString(this.window, seq);
    }

    /**
     * Retrieves the clipboard string
     *
     * @return the clipboard string
     * @throws GLFWException if the window is not initialized.
     * @see <a href="http://www.glfw.org/docs/latest/input.html#clipboard">GLFW
     * Clipboard Input and Output</a>
     * @since 15.06.07
     */
    public String getClipboardString() throws GLFWException {
        if (!this.isValid()) {
            throw new GLFWException("Invalid GLWindow!");
        }

        GLFW_LOGGER.trace(GLFW_MARKER, "glfwGetClipboardString({})", this.window);
        return GLFW.glfwGetClipboardString(this.window);
    }

    /**
     * Retrieves the time in seconds since the start of the application.
     *
     * @return the current time
     * @see <a href="http://www.glfw.org/docs/latest/input.html#time">GLFW Time
     * Input</a>
     * @since 15.06.07
     */
    public static double getTime() {
        return GLFW.glfwGetTime();
    }

    private void setFramebufferResizeCallback(final GLFramebufferResizeListener listener) {
        final GLFWFramebufferSizeCallback callback
                = GLFWFramebufferSizeCallback.create(listener::glfwFramebufferResizeCallback);

        GLFW_LOGGER.trace(GLFW_MARKER, "glfwSetFramebufferSizeCallback({}, {})", this.window, callback);
        GLFW.glfwSetFramebufferSizeCallback(this.window, callback);

        this.resizeCallback = Optional.of(callback);
    }

    /**
     * Retrieves the DPI of the monitor displaying the window.
     *
     * @return the DPI
     * @throws GLFWException if the window has not been initialized.
     * @since 15.06.07
     */
    public double getDPI() throws GLFWException {
        return new DPIQuery().glCall(this.getGLThread());
    }

    public class DPIQuery extends GLQuery<Double> {

        @Override
        public Double call() throws Exception {
            LOGGER.trace(GLOOP_MARKER, "############### Start GLWindow DPI Query ###############");
            LOGGER.trace(GLOOP_MARKER, "\tQuerying GLWindow[{}]", GLWindow.this.window);

            if (!GLWindow.this.isValid()) {
                throw new GLFWException("GLWindow is not valid!");
            }

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwGetWindowMonitor({})", GLWindow.this.monitor);
            final long mHandle = GLFW.glfwGetWindowMonitor(GLWindow.this.monitor);

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwGetVideoMode({})", mHandle);
            final GLFWVidMode mode = GLFW.glfwGetVideoMode(mHandle);
            final ByteBuffer widthMM = NativeTools.getInstance().nextWord();
            final ByteBuffer heightMM = NativeTools.getInstance().nextWord();

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwGetMonitorPhysicalSize({}, {}, {})", mHandle, widthMM, heightMM);
            GLFW.glfwGetMonitorPhysicalSize(mHandle, widthMM, heightMM);

            final int vWidth = mode.width();
            final double dpi = (vWidth / (widthMM.getInt() / 25.4 /* mm to in */));

            LOGGER.trace(GLOOP_MARKER, "GLWindow[{}].dpi = {}", GLWindow.this.window, dpi);
            LOGGER.trace(GLOOP_MARKER, "############### End GLWindow DPI Query ###############");

            return dpi;
        }
    }

    private class InitTask extends GLTask {

        @Override
        public void run() {
            LOGGER.trace(GLOOP_MARKER, "############### Start GLWindow Init task ###############");
            LOGGER.trace(GLOOP_MARKER, "\tInitializing GLWindow[{}]", GLWindow.this.title);

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_VISIBLE, GL_FALSE)");
            glfwWindowHint(GLFW.GLFW_VISIBLE, GL_FALSE);
            GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_RESIZABLE, GL_TRUE)");
            glfwWindowHint(GLFW.GLFW_RESIZABLE, GL_TRUE);

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, {})", OPENGL_VERSION_MAJOR);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, OPENGL_VERSION_MAJOR);
            GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, {})", OPENGL_VERSION_MINOR);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, OPENGL_VERSION_MINOR);

            if (OPENGL_VERSION_MAJOR == 3 && OPENGL_VERSION_MINOR == 2 || OPENGL_VERSION_MAJOR > 3) {
                GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)");
                glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            }

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_SAMPLES, {})", OPENGL_SAMPLES);
            glfwWindowHint(GLFW_SAMPLES, OPENGL_SAMPLES);
            GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_RED_BITS, {})", OPENGL_RED_BITS);
            glfwWindowHint(GLFW_RED_BITS, OPENGL_RED_BITS);
            GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_BLUE_BITS, {})", OPENGL_BLUE_BITS);
            glfwWindowHint(GLFW_BLUE_BITS, OPENGL_BLUE_BITS);
            GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_GREEN_BITS, {})", OPENGL_GREEN_BITS);
            glfwWindowHint(GLFW_GREEN_BITS, OPENGL_GREEN_BITS);
            GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_ALPHA_BITS, {})", OPENGL_ALPHA_BITS);
            glfwWindowHint(GLFW_ALPHA_BITS, OPENGL_ALPHA_BITS);
            GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_DEPTH_BITS, {})", OPENGL_DEPTH_BITS);
            glfwWindowHint(GLFW_DEPTH_BITS, OPENGL_DEPTH_BITS);
            GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_STENCIL_BITS, {})", OPENGL_STENCIL_BITS);
            glfwWindowHint(GLFW_STENCIL_BITS, OPENGL_STENCIL_BITS);
            GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_REFRESH_RATE, {})", OPENGL_REFRESH_RATE);
            glfwWindowHint(GLFW_REFRESH_RATE, OPENGL_REFRESH_RATE);

            final long sharedContextHandle = shared != null ? shared.window : NULL;

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwCreateWindow({}, {}, {}, {}, {})",
                    GLWindow.this.width, GLWindow.this.height,
                    GLWindow.this.title,
                    GLWindow.this.monitor,
                    sharedContextHandle);

            GLWindow.this.window = GLFW.glfwCreateWindow(
                    GLWindow.this.width, GLWindow.this.height,
                    GLWindow.this.title,
                    GLWindow.this.monitor,
                    sharedContextHandle);

            if (GLWindow.this.window == NULL) {
                throw new GLFWException("Failed to create the GLFW window!");
            }

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwMakeContextCurrent({})", GLWindow.this.window);
            GLFW.glfwMakeContextCurrent(GLWindow.this.window);

            GL.createCapabilities();

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwSwapInterval({})", OPENGL_SWAP_INTERVAL);
            GLFW.glfwSwapInterval(OPENGL_SWAP_INTERVAL);

            final ByteBuffer fbWidth = NativeTools.getInstance().nextWord();
            final ByteBuffer fbHeight = NativeTools.getInstance().nextWord();

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwGetFramebufferSize({}, {}, {})", GLWindow.this.window, fbWidth, fbHeight);
            GLFW.glfwGetFramebufferSize(GLWindow.this.window, fbWidth, fbHeight);

            GLWindow.this.thread.currentViewport = new GLViewport(0, 0, fbWidth.getInt(), fbHeight.getInt());
            GLWindow.this.handler.register();

            WINDOWS.put(GLWindow.this.window, GLWindow.this);
            GLWindow.this.hasInitialized = true;

            GLFW.glfwSetKeyCallback(GLWindow.this.window, GLWindow.this.keyCallback.get());
            GLFW.glfwSetMouseButtonCallback(GLWindow.this.window, GLWindow.this.mouseButtonCallback.get());
            GLFW.glfwSetCursorEnterCallback(GLWindow.this.window, GLWindow.this.cursorEnterCallback.get());
            GLFW.glfwSetCursorPosCallback(GLWindow.this.window, GLWindow.this.cursorPosCallback.get());
            GLFW.glfwSetScrollCallback(GLWindow.this.window, GLWindow.this.scrollCallback.get());
            GLFW.glfwSetCharCallback(GLWindow.this.window, GLWindow.this.charCallback.get());

            LOGGER.trace(GLOOP_MARKER, "GLWindow[{}] is initialized!", GLWindow.this.title);
            LOGGER.trace(GLOOP_MARKER, "############### End GLWindow Init Task ###############");
        }
    }

    /**
     * Toggles fullscreen for the window. Note: this will destroy the current
     * OpenGL context and create a new one. Some objects such as Vertex Array
     * Objects will be lost.
     *
     * @param fullscreen the fullscreen setting.
     * @since 15.10.30
     */
    public void setFullscreen(final boolean fullscreen) {
        new SetFullscreenTask(fullscreen).glRun();
    }

    private final List<Runnable> onContextLost = new ArrayList<>();

    /**
     * Adds a callback for when the OpenGL context is lost.
     *
     * @param callback method to run when the OpenGL context is lost.
     * @since 15.10.30
     */
    public void addContextLostListener(final Runnable callback) {
        LOGGER.trace(GLOOP_MARKER, "GLWindow[{}]: Adding callback for contextLost[{}]", this.title, callback);
        this.onContextLost.add(callback);
    }

    /**
     * Remove a context lost callback.
     *
     * @param callback the callback to remove.
     * @since 15.10.30
     */
    public void removeContextLostListener(final Runnable callback) {
        LOGGER.trace(GLOOP_MARKER, "GLWindow[{}]: Removing callback for contextLost[{}]", this.title, callback);
        this.onContextLost.remove(callback);
    }

    private class SetFullscreenTask extends GLTask {

        private final boolean isFullscreen;

        SetFullscreenTask(boolean useFS) {
            this.isFullscreen = useFS;
        }

        @Override
        public void run() {
            LOGGER.trace(GLOOP_MARKER, "############### Start GLWindow Set Fullscreen Task ###############");
            LOGGER.trace(GLOOP_MARKER, "\tApplying to GLWindow[{}]", GLWindow.this.title);
            LOGGER.trace(GLOOP_MARKER, "\tSet fullscreen: {}", this.isFullscreen);

            final long monitor = isFullscreen ? GLFW.glfwGetPrimaryMonitor() : NULL;
            final long newWindow = GLFW.glfwCreateWindow(width, height, title, monitor, GLWindow.this.window);

            if (newWindow == NULL) {
                throw new GLFWException("Failed to create the GLFW window!");
            }

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwDestoryWindow({})", GLWindow.this.window);
            GLFW.glfwDestroyWindow(GLWindow.this.window);

            onContextLost.forEach(Runnable::run);

            WINDOWS.remove(GLWindow.this.window);
            WINDOWS.put(newWindow, GLWindow.this);

            GLWindow.this.window = newWindow;
            GLFW_LOGGER.trace(GLFW_MARKER, "glfwMakeContextCurrent({})", GLWindow.this.window);
            GLFW.glfwMakeContextCurrent(GLWindow.this.window);

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwSwapInterval({})", OPENGL_SWAP_INTERVAL);
            GLFW.glfwSwapInterval(OPENGL_SWAP_INTERVAL);

            final ByteBuffer fbWidth = NativeTools.getInstance().nextWord();
            final ByteBuffer fbHeight = NativeTools.getInstance().nextWord();

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwGetFramebufferSize({}, {}, {})", GLWindow.this.window, fbWidth, fbHeight);
            GLFW.glfwGetFramebufferSize(GLWindow.this.window, fbWidth, fbHeight);
            GLWindow.this.thread.currentViewport = new GLViewport(0, 0, fbWidth.getInt(), fbHeight.getInt());

            GLWindow.this.handler.register();
            GLFW.glfwSetKeyCallback(GLWindow.this.window, GLWindow.this.keyCallback.get());
            GLFW.glfwSetMouseButtonCallback(GLWindow.this.window, GLWindow.this.mouseButtonCallback.get());
            GLFW.glfwSetCursorEnterCallback(GLWindow.this.window, GLWindow.this.cursorEnterCallback.get());
            GLFW.glfwSetCursorPosCallback(GLWindow.this.window, GLWindow.this.cursorPosCallback.get());
            GLFW.glfwSetScrollCallback(GLWindow.this.window, GLWindow.this.scrollCallback.get());
            GLFW.glfwSetCharCallback(GLWindow.this.window, GLWindow.this.charCallback.get());

            LOGGER.trace(GLOOP_MARKER, "############### End GLWindow Set Fullscreen Task ###############");
        }

    }

    /**
     * Retrieves the aspect ratio for the window. This number is the width
     * divided by the height.
     *
     * @return the aspect ratio.
     * @since 15.06.07
     */
    public double getAspectRatio() {
        return (double) this.width / (double) this.height;
    }

    /**
     * Sets the visibility of the window.
     *
     * @param isVisible the visibility flag.
     * @since 15.06.24
     */
    public void setVisible(final boolean isVisible) {
        new SetWindowVisibilityTask(isVisible).glRun(this.getGLThread());
    }

    /**
     * A GLTask that sets the visibility for the window.
     *
     * @since 15.06.24
     */
    public class SetWindowVisibilityTask extends GLTask {

        final boolean visibility;

        /**
         * Constructs a new SetWindowVisibilityTask for setting the visibility
         * to the specified value.
         *
         * @param isVisible the visibility flag.
         * @since 15.06.24
         */
        public SetWindowVisibilityTask(final boolean isVisible) {
            this.visibility = isVisible;
        }

        @Override
        public void run() {
            LOGGER.trace(GLOOP_MARKER, "############### Start GLWindow Set Window Visibility Task ###############");
            LOGGER.trace(GLOOP_MARKER, "\tSetting visibility of GLWindow[{}]", GLWindow.this.title);
            LOGGER.trace(GLOOP_MARKER, "\tVisibility: {}", this.visibility);

            if (!GLWindow.this.isValid()) {
                throw new GLFWException("GLWindow is not valid!");
            }

            if (this.visibility) {
                GLFW_LOGGER.trace(GLFW_MARKER, "glfwShowWindow({})", GLWindow.this.window);
                GLFW.glfwShowWindow(GLWindow.this.window);
            } else {
                GLFW_LOGGER.trace(GLFW_MARKER, "glfwHideWindow({})", GLWindow.this.window);
                GLFW.glfwHideWindow(GLWindow.this.window);
            }

            LOGGER.trace(GLOOP_MARKER, "############### End GLWindow Set Window Visibility Task ###############");
        }
    }

    /**
     * Sets the size of the window.
     *
     * @param width the width of the window
     * @param height the height of the window
     * @throws GLFWException if the window is invalid or an invalid width or
     * height was provided.
     * @since 15.06.07
     */
    public void setSize(final int width, final int height) throws GLFWException {
        new SetWindowSizeTask(width, height).glRun(this.getGLThread());
    }

    public class SetWindowSizeTask extends GLTask {

        final int width;
        final int height;

        public SetWindowSizeTask(final int width, final int height) {
            if ((this.width = width) < 0) {
                throw new GLFWException("Cannot set window width to less than 0!");
            }

            if ((this.height = height) < 0) {
                throw new GLFWException("Cannot set window height to less than 0!");
            }
        }

        @Override
        public void run() {
            LOGGER.trace(GLOOP_MARKER, "############### Start GLWindow Set Window Size Task ###############");
            LOGGER.trace(GLOOP_MARKER, "\tSetting size of GLWindow[{}]", GLWindow.this.title);
            LOGGER.trace(GLOOP_MARKER, "\tSize: <{}, {}>", this.width, this.height);

            if (!GLWindow.this.isValid()) {
                throw new GLFWException("GLWindow is not valid!");
            }

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwSetWindowSize({}, {}, {})", GLWindow.this.window, this.width, this.height);
            GLFW.glfwSetWindowSize(GLWindow.this.window, this.width, this.height);

            LOGGER.trace(GLOOP_MARKER, "############### End GLWindow Set Window Size Task ###############");
        }

    }

    /**
     * Retrieves the width of the back buffer.
     *
     * @return the back buffer width.
     * @throws GLFWException if the window is not initialized.
     * @since 15.06.07
     */
    public final int getFramebufferWidth() throws GLFWException {
        final GLQuery<int[]> sizeQuery = new FramebufferSizeQuery();

        return sizeQuery.glCall(this.getGLThread())[GLTools.WIDTH];
    }

    /**
     * Retrieves the height of the back buffer.
     *
     * @return the back buffer height
     * @throws GLFWException if the window is not initialized.
     * @since 15.06.07
     */
    public final int getFramebufferHeight() throws GLFWException {
        final GLQuery<int[]> sizeQuery = new FramebufferSizeQuery();

        return sizeQuery.glCall(this.getGLThread())[GLTools.HEIGHT];
    }

    /**
     * A GLQuery that requests the size of the GLWindow.
     *
     * @since 15.06.07
     */
    public class FramebufferSizeQuery extends GLQuery<int[]> {

        @Override
        public int[] call() throws Exception {
            LOGGER.trace(GLOOP_MARKER, "############### Start GLWindow Framebuffer Size Query ###############");
            LOGGER.trace(GLOOP_MARKER, "\tQuerying framebuffer size of GLWindow[{}]", GLWindow.this.title);

            if (!GLWindow.this.isValid()) {
                throw new GLFWException("GLWindow is not valid!");
            }

            final ByteBuffer width = NativeTools.getInstance().nextWord();
            final ByteBuffer height = NativeTools.getInstance().nextWord();

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwGetFramebufferSize({}, {}, {})", GLWindow.this.window, width, height);
            GLFW.glfwGetFramebufferSize(GLWindow.this.window, width, height);

            final int[] size = new int[]{width.getInt(), height.getInt()};

            LOGGER.trace(GLOOP_MARKER, "GLWindow[{}] framebuffer size: <{}, {}>", size[0], size[1]);
            LOGGER.trace(GLOOP_MARKER, "############### End GLWindow Framebuffer Size Query ###############");

            return size;
        }

    }

    /**
     * Sets the window's cursor.
     *
     * @param cursorId the GLFW cursor id.
     * @since 15.10.30
     */
    public final void setCursor(final long cursorId) {
        new SetCursorTask(cursorId).glRun(this.getGLThread());
    }

    /**
     * A GLTask that sets the GLWindow's cursor.
     *
     * @since 15.10.30
     */
    public class SetCursorTask extends GLTask {

        final long cursorId;

        public SetCursorTask(final long cursorId) {
            this.cursorId = cursorId;
        }

        @Override
        public void run() {
            GLFW_LOGGER.trace(GLFW_MARKER, "glfwSetCursor({}, {})", GLWindow.this.window, this.cursorId);
            GLFW.glfwSetCursor(GLWindow.this.window, this.cursorId);
        }

    }

    /**
     * Retrieves the x-position of the top-left of the window.
     *
     * @return the top-left x coordinate in screen space.
     * @throws GLFWException if the the window is not initialized.
     * @since 15.06.07
     */
    public int getX() throws GLFWException {
        return new WindowPositionQuery().glCall(this.getGLThread())[GLTools.X];
    }

    /**
     * Retrieves the y-position of the top-left of the window.
     *
     * @return the top-left y coordinate in screen space.
     * @throws GLFWException if the window has not been initialized.
     * @since 15.06.07
     */
    public int getY() throws GLFWException {
        return new WindowPositionQuery().glCall(this.getGLThread())[GLTools.Y];
    }

    /**
     * A GLQuery that requests the position of the window.
     *
     * @since 15.06.30
     */
    public class WindowPositionQuery extends GLQuery<int[]> {

        @Override
        public int[] call() throws Exception {
            if (!GLWindow.this.isValid()) {
                throw new GLFWException("GLWindow is not valid!");
            }

            final ByteBuffer x = NativeTools.getInstance().nextWord();
            final ByteBuffer y = NativeTools.getInstance().nextWord();

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwGetWindowPos({}, {}, {})", GLWindow.this.window, x, y);
            GLFW.glfwGetWindowPos(GLWindow.this.window, x, y);

            return new int[]{x.getInt(), y.getInt()};
        }
    }

    public class WindowFrameSizeQuery extends GLQuery<int[]> {

        @Override
        public int[] call() throws Exception {
            LOGGER.trace(GLOOP_MARKER, "############### Start GLWindow Window Frame Size Query ###############");
            LOGGER.trace(GLOOP_MARKER, "\tQuerying window frame size of GLWindow[{}]", GLWindow.this.title);

            if (!GLWindow.this.isValid()) {
                throw new GLFWException("GLWindow is not valid!");
            }

            final ByteBuffer l = NativeTools.getInstance().nextWord();
            final ByteBuffer t = NativeTools.getInstance().nextWord();
            final ByteBuffer r = NativeTools.getInstance().nextWord();
            final ByteBuffer b = NativeTools.getInstance().nextWord();

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwGetWindowFrameSize({}, {}, {}, {}, {})", GLWindow.this.window, l, t, r, b);
            GLFW.glfwGetWindowFrameSize(GLWindow.this.window, l, t, r, b);

            final int[] size = new int[]{l.getInt(), t.getInt(), r.getInt(), b.getInt()};

            LOGGER.trace(GLOOP_MARKER, "GLWindow[{}].size=<{}, {}, {}, {}>",
                    GLWindow.this.title,
                    size[0], size[1], size[2], size[3]);

            LOGGER.trace(GLOOP_MARKER, "############### End GLWindow Window Size Query ###############");
            return size;
        }
    }

    /**
     * Retrieves the width of the window.
     *
     * @return the window width
     * @throws GLFWException if the window has not been initialized.
     * @since 15.06.05
     */
    public int getWidth() {
        return new WindowSizeQuery().glCall(this.getGLThread())[GLTools.WIDTH];
    }

    public int getWindowFrameLeft() {
        return new WindowFrameSizeQuery().glCall(this.getGLThread())[0];
    }

    public int getWindowFrameTop() {
        return new WindowFrameSizeQuery().glCall(this.getGLThread())[1];
    }

    public int getWindowFrameRight() {
        return new WindowFrameSizeQuery().glCall(this.getGLThread())[2];
    }

    public int getWindowFrameBottom() {
        return new WindowFrameSizeQuery().glCall(this.getGLThread())[3];
    }

    /**
     * Retrieves the height of the window.
     *
     * @return the height of the window.
     * @throws GLFWException if the window has not been initialized.
     * @since 15.06.05
     */
    public int getHeight() throws GLFWException {
        return new WindowSizeQuery().glCall(this.getGLThread())[GLTools.HEIGHT];
    }

    /**
     * A GLTask that requests the size of the window.
     *
     * @since 15.06.30
     */
    public class WindowSizeQuery extends GLQuery<int[]> {

        @Override
        public int[] call() throws Exception {
            LOGGER.trace(GLOOP_MARKER, "############### Start GLWindow Size Query ###############");
            LOGGER.trace(GLOOP_MARKER, "\tQuerying window size of GLWindow[{}]", GLWindow.this.title);

            if (!GLWindow.this.isValid()) {
                throw new GLFWException("GLWindow is not valid!");
            }
            final ByteBuffer w = NativeTools.getInstance().nextWord();
            final ByteBuffer h = NativeTools.getInstance().nextWord();

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwGetWindowSize({}, {}, {})", GLWindow.this.window, w, h);
            GLFW.glfwGetWindowSize(GLWindow.this.window, w, h);

            final int[] size = new int[]{w.getInt(), h.getInt()};

            LOGGER.trace(GLOOP_MARKER, "GLWindow[{}].size=<{}, {}>", GLWindow.this.title, size[0], size[1]);
            LOGGER.trace(GLOOP_MARKER, "############### End GLWindow Size Query ###############");

            return size;
        }

    }

    /**
     * Retrieves the thread owned by the GLWindow.
     *
     * @return the thread owned by the window.
     * @since 15.06.05
     */
    public GLThread getGLThread() {
        return this.thread;
    }

    private final UpdateTask updateTask = new UpdateTask();

    /**
     * Executes an update task on the default thread.
     *
     * @throws GLFWException if the window is invalid.
     * @since 15.06.05
     */
    public void update() throws GLFWException {
        if (!this.isValid()) {
            throw new GLFWException("Invalid GLWindow!");
        }
        this.updateTask.glRun(this.getGLThread());
    }

    /**
     * A task that updates the window and checks for input.
     *
     * @since 15.06.05
     */
    public class UpdateTask extends GLTask {

        int frameCount = 0;

        @Override
        public void run() {
            LOGGER.trace(GLOOP_MARKER, "############### Start GLWindow Update Task ###############");
            LOGGER.trace(GLOOP_MARKER, "\tUpdating GLWindow[{}]", GLWindow.this.title);

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowShouldClose({})", GLWindow.this.window);
            if (GLFW.glfwWindowShouldClose(GLWindow.this.window) == GL_TRUE) {
                GLWindow.this.cleanup();
            } else {
                GLFW_LOGGER.trace(GLFW_MARKER, "glfwSwapBuffers({})", GLWindow.this.window);
                GLFW.glfwSwapBuffers(GLWindow.this.window);
                GLFW_LOGGER.trace(GLFW_MARKER, "glfwPollEvents()");
                GLFW.glfwPollEvents();
                GLFW_LOGGER.trace(GLOOP_MARKER, "--------------- FRAME {} END ---------------", frameCount++);
                GLFW_LOGGER.trace(GLOOP_MARKER, "--------------- FRAME {} START ---------------", frameCount);
            }

            LOGGER.trace(GLOOP_MARKER, "############### End GLWindow Update Task ###############");
        }
    }

    /**
     * Closes the window.
     *
     * @since 15.07.01
     */
    public void close() {
        new CloseTask().glRun(this.getGLThread());
    }

    /**
     * A GLTask that closes the GLWindow.
     *
     * @since 15.07.01
     */
    public class CloseTask extends GLTask {

        @Override
        public void run() {
            LOGGER.trace(GLOOP_MARKER, "############### Start GLWindow Close Task ###############");
            LOGGER.trace(GLOOP_MARKER, "\tClosing GLWindow[{}]", GLWindow.this.window);

            if (!GLWindow.this.isValid()) {
                throw new GLFWException("GLWindow is not valid!");
            }

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwSetWindowShouldClose({}, GL_TRUE)", GLWindow.this.window);
            GLFW.glfwSetWindowShouldClose(GLWindow.this.window, GL_TRUE);

            LOGGER.trace(GLOOP_MARKER, "############### End GLWindow Close Task ###############");
        }
    }

    private void cleanup() {
        LOGGER.trace(GLFW_MARKER, "Cleaning up resources for GLWindow[{}]", this.title);

        LOGGER.trace(GLFW_MARKER, "Removing tasks...");
        this.cleanupTasks.forEach(Runnable::run);
        this.cleanupTasks.clear();

        LOGGER.trace(GLFW_MARKER, "Stopping worker threads...");
        this.workerThreads.forEach(GLWindow::close);

        LOGGER.trace(GLFW_MARKER, "Releasing callbacks...");
        this.cursorEnterCallback.ifInitialized(GLFWCursorEnterCallback::release);
        this.cursorPosCallback.ifInitialized(GLFWCursorPosCallback::release);
        this.keyCallback.ifInitialized(GLFWKeyCallback::release);
        this.charCallback.ifInitialized(GLFWCharCallback::release);
        this.mouseButtonCallback.ifInitialized(GLFWMouseButtonCallback::release);
        this.scrollCallback.ifInitialized(GLFWScrollCallback::release);
        this.resizeCallback.ifPresent(GLFWFramebufferSizeCallback::release);

        LOGGER.trace(GLFW_MARKER, "Firing onClose callback...");
        this.onClose.ifPresent(Runnable::run);

        // stop everything
        this.thread.submit(() -> {
            GLFW_LOGGER.trace(GLFW_MARKER, "glfwDestoryWindow({})", this.window);
            GLFW.glfwDestroyWindow(this.window);

            WINDOWS.remove(this.window);
            LOGGER.trace(GLFW_MARKER, "GLWindow[{}] has been destroyed!", this.title);
            this.window = GLWindow.INVALID_WINDOW_ID;
        });

        LOGGER.trace(GLFW_MARKER, "Shutting down thread...");
        this.thread.shutdown();
    }

    private final List<GLWindow> workerThreads = new ArrayList<>();

    /**
     * Constructs a new OpenGL worker thread. The worker thread will have a
     * shared context with the window.
     *
     * @return the new worker thread.
     * @throws GLFWException if the window is invalid.
     * @since 15.06.05
     */
    public GLThread newWorkerThread() throws GLFWException {
        if (!this.isValid()) {
            throw new GLFWException("Invalid GLWindow!");
        }
        final GLWindow dummy = new GLWindow(0, 0, "WORKER", this);

        LOGGER.trace(GLFW_MARKER, "GLWindow[{}]: Adding worker thread!", this.title);

        this.workerThreads.add(dummy);

        return dummy.getGLThread();
    }

    @Override
    public String toString() {
        return "GLWindow: " + this.window;
    }

    /**
     * Attempts to append a task to the end of the cleanup queue.
     *
     * @param task the task to append.
     * @since 15.06.30
     */
    public void appendToCleanup(final Runnable task) {
        LOGGER.trace(GLOOP_MARKER, "GLWindow[{}]: Adding callback for cleanup: {}", this.title, task);
        this.cleanupTasks.add(task);
    }

    /**
     * Attempts to remove a task from the cleanup queue.
     *
     * @param task the task to remove.
     * @return true if the task was removed.
     * @since 15.06.30
     */
    public boolean removeFromCleanup(final Runnable task) {
        LOGGER.trace(GLOOP_MARKER, "GLWindow[{}]: Removing callback for cleanup: {}", this.title, task);
        return this.cleanupTasks.remove(task);
    }

    /**
     * Removes all tasks from the cleanup task queue.
     *
     * @since 15.06.30
     */
    public void clearCleanup() {
        LOGGER.trace(GLOOP_MARKER, "Removing all cleanup callbacks from GLWindow[{}]!", this.title);
        this.cleanupTasks.clear();
    }

    private final WindowHandler handler = new WindowHandler();

    /**
     * Adds a listener for when the window resizes.
     *
     * @param listener the resize listener.
     * @return true if the listener was registered.
     * @since 15.06.24
     */
    public boolean addWindowResizeListener(final GLFramebufferResizeListener listener) {
        LOGGER.trace(GLOOP_MARKER, "GLWindow[{}]: Adding callback for windowResize[{}]", this.title, listener);
        return this.handler.resizeListeners.add(listener);
    }

    /**
     * Attempts to remove a window resize listener.
     *
     * @param listener the listener to remove.
     * @return true if the listener was removed.
     * @since 15.06.24
     */
    public boolean removeWindowResizeListener(final GLFramebufferResizeListener listener) {
        LOGGER.trace(GLOOP_MARKER, "GLWindow[{}]: Removing callback for windowResize[{}]", this.title, listener);
        return this.handler.resizeListeners.remove(listener);
    }

    /**
     * Removes all window listeners from the window.
     *
     * @since 15.06.24
     */
    public void clearWindowListeners() {
        this.handler.resizeListeners.clear();
    }

    private class WindowHandler implements GLFramebufferResizeListener {

        final List<GLFramebufferResizeListener> resizeListeners = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void framebufferResizedActionPerformed(GLWindow window, GLViewport view) {
            if (!window.getGLThread().viewportStack.isEmpty()) {
                throw new GLFWException("Viewport stack is not empty on Window Resize event!");
            }

            view.applyViewport();

            this.resizeListeners.forEach((listener) -> {
                listener.framebufferResizedActionPerformed(window, view);
            });
        }

        void register() {
            GLWindow.this.setFramebufferResizeCallback(this);
        }
    }
}
