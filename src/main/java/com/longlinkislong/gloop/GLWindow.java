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

import com.runouw.util.Lazy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.lwjgl.glfw.GLFW;
import static org.lwjgl.glfw.GLFW.GLFW_ALPHA_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_BLUE_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_CLIENT_API;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_CREATION_API;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_DEPTH_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_EGL_CONTEXT_API;
import static org.lwjgl.glfw.GLFW.GLFW_GREEN_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_NO_API;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_API;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_ES_API;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RED_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_REFRESH_RATE;
import static org.lwjgl.glfw.GLFW.GLFW_SAMPLES;
import static org.lwjgl.glfw.GLFW.GLFW_STENCIL_BITS;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWCursorEnterCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.lwjgl.glfw.GLFWWindowIconifyCallback;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import org.lwjgl.opengles.GLES;
import static org.lwjgl.system.MemoryUtil.NULL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * A GLWindow represents a window that handles OpenGL drawing.
 *
 * @author zmichaels
 * @since 15.06.24
 */
public class GLWindow {
    
    private static final boolean USE_EGL = Boolean.getBoolean("com.longlinkislong.gloop.opengl.use_egl");
    private static final Logger LOGGER = LoggerFactory.getLogger("GLWindow");
    private static final Logger GLFW_LOGGER = LoggerFactory.getLogger("GLFW");
    private static final Marker GLFW_MARKER = MarkerFactory.getMarker("GLFW");
    private static final Marker GLOOP_MARKER = MarkerFactory.getMarker("GLOOP");

    public enum ClientAPI {
        VULKAN,
        OPENGL,
        OPENGLES
    }

    public static final ClientAPI CLIENT_API;
    public static final int VERSION_MAJOR;
    public static final int VERSION_MINOR;
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

    protected final List<GLKeyListener> keyListeners = new ArrayList<>(0);
    protected final List<GLMousePositionListener> mousePositionListeners = new ArrayList<>(0);
    protected final List<GLMouseButtonListener> mouseButtonListeners = new ArrayList<>(0);
    protected final List<GLMouseEnteredListener> mouseEnteredListeners = new ArrayList<>(0);
    protected final List<GLMouseScrollListener> mouseScrollListeners = new ArrayList<>(0);
    protected final List<GLKeyCharListener> charListeners = new ArrayList<>(0);

    private final Lazy<GLFWCharCallback> charCallback = new Lazy<>(() -> {
        final GLFWCharCallback callback = GLFWCharCallback.create((hwnd, charCode) -> {
            this.charListeners.forEach(listener -> listener.glfwCharCallback(hwnd, charCode));
        });
        
        return callback;
    });

    private final Lazy<GLFWKeyCallback> keyCallback = new Lazy<>(() -> {
        final GLFWKeyCallback callback = GLFWKeyCallback.create((hwnd, key, scancode, action, mods) -> {
            keyListeners.forEach(listener -> listener.glfwCallback(hwnd, key, scancode, action, mods));
        });

        return callback;
    });

    private final Lazy<GLFWMouseButtonCallback> mouseButtonCallback = new Lazy<>(() -> {
        final GLFWMouseButtonCallback callback = GLFWMouseButtonCallback.create((hwnd, button, action, mods) -> {
            this.mouseButtonListeners.forEach(listener -> listener.glfwMouseButtonCallback(hwnd, button, action, mods));
        });

        return callback;
    });

    private final Lazy<GLFWCursorPosCallback> cursorPosCallback = new Lazy<>(() -> {
        final GLFWCursorPosCallback callback = GLFWCursorPosCallback.create((hwnd, x, y) -> {
            this.mousePositionListeners.forEach(listener -> listener.glfwCursorPosCallback(hwnd, x, y));
        });
        
        return callback;
    });

    private final Lazy<GLFWScrollCallback> scrollCallback = new Lazy<>(() -> {
        final GLFWScrollCallback callback = GLFWScrollCallback.create((hwnd, x, y) -> {
            this.mouseScrollListeners.forEach(listener -> listener.glfwScrollCallback(hwnd, x, y));
        });

        return callback;
    });

    private final Lazy<GLFWCursorEnterCallback> cursorEnterCallback = new Lazy<>(() -> {
        final GLFWCursorEnterCallback callback = GLFWCursorEnterCallback.create((hwnd, status) -> {
            final int iStatus = status ? 1 : 0;

            this.mouseEnteredListeners.forEach(listener -> listener.glfwCursorEnteredCallback(hwnd, iStatus));
        });

        return callback;
    });

    private final Lazy<GLFWWindowCloseCallback> windowCloseCallback = new Lazy<>(() -> {
        final GLFWWindowCloseCallback callback = GLFWWindowCloseCallback.create((hwnd) -> {
            this.beforeClose.ifPresent(Runnable::run);
        });
        
        return callback;
    });

    private final Lazy<GLFWWindowIconifyCallback> windowIconifyCallback = new Lazy<>(() -> {
        final GLFWWindowIconifyCallback callback = GLFWWindowIconifyCallback.create((long hwnd, boolean iconified) -> {
            if (iconified) {
                this.onMinimize.ifPresent(Runnable::run);
            } else {
                this.onRestore.ifPresent(Runnable::run);
            }
            // TODO: call resize callbacks?

        });

        return callback;
    });

    /**
     * Tells the window to close or not.
     *
     * @param shouldClose
     */
    public void setShouldClose(boolean shouldClose) {
        GLFW.glfwSetWindowShouldClose(window, shouldClose);
    }

    private Optional<GLFWFramebufferSizeCallback> resizeCallback = Optional.empty();
    private Optional<Runnable> beforeClose = Optional.empty();
    private Optional<Runnable> onClose = Optional.empty();
    private Optional<Runnable> onMinimize = Optional.empty();
    private Optional<Runnable> onRestore = Optional.empty();
    private final long monitor;
    private volatile boolean hasInitialized = false;
    private final List<Runnable> cleanupTasks = new ArrayList<>(0);

    protected static final Map<Long, GLWindow> WINDOWS = new TreeMap<>(Long::compareTo);
    private static final List<GLGamepad> GAMEPADS;

    static {        
        final String glVersion = System.getProperty("com.longlinkislong.gloop.opengl.version", "1.0");
        VERSION_MAJOR = Integer.parseInt(glVersion.substring(0, glVersion.indexOf(".")));
        VERSION_MINOR = Integer.parseInt(glVersion.substring(glVersion.indexOf(".") + 1));
        OPENGL_REFRESH_RATE = Integer.getInteger("com.longlinkislong.gloop.opengl.refresh_rate", -1);
        OPENGL_SWAP_INTERVAL = Integer.getInteger("com.longlinkislong.gloop.opengl.swap_interval", 1);
        OPENGL_SAMPLES = Integer.getInteger("com.longlinkislong.gloop.opengl.msaa", -1);
        OPENGL_RED_BITS = Integer.getInteger("com.longlinkislong.gloop.opengl.red_bits", 8);
        OPENGL_GREEN_BITS = Integer.getInteger("com.longlinkislong.gloop.opengl.green_bits", 8);
        OPENGL_BLUE_BITS = Integer.getInteger("com.longlinkislong.gloop.opengl.blue_bits", 8);
        OPENGL_ALPHA_BITS = Integer.getInteger("com.longlinkislong.gloop.opengl.alpha_bits", 8);
        OPENGL_DEPTH_BITS = Integer.getInteger("com.longlinkislong.gloop.opengl.depth_bits", 24);
        OPENGL_STENCIL_BITS = Integer.getInteger("com.longlinkislong.gloop.opengl.stencil_bits", 8);

        final String apiString = System.getProperty("com.longlinkislong.gloop.client_api", "OpenGL");

        switch (apiString.toLowerCase()) {
            case "vulkan":
                CLIENT_API = ClientAPI.VULKAN;
                break;
            case "ogl":
            case "gl":
            case "opengl":
                CLIENT_API = ClientAPI.OPENGL;
                break;
            case "ogles":
            case "ogl_es":
            case "gles":
            case "opengles":
            case "opengl_es":
                CLIENT_API = ClientAPI.OPENGLES;
                break;
            default:
                throw new UnsupportedOperationException("Unsupported client API: " + apiString);
        }

        if (GLFW.glfwInit()) {
            LOGGER.trace(GLFW_MARKER, "GLFW successfully initialized!");
        } else {
            throw new GLFWException("Could not initialize GLFW!");
        }

        final GLFWErrorCallback errCallback = GLFWErrorCallback.create((error, desc) -> {
            final String msg = GLFWErrorCallback.getDescription(desc);

            GLFW_LOGGER.error(GLFW_MARKER, "GLFW Error #{}: {}", error, msg);
        });
        

        GLFW.glfwSetErrorCallback(errCallback);

        final List<GLGamepad> gamepads = new ArrayList<>(0);
        for (int i = 0; i < GLFW.GLFW_JOYSTICK_LAST; i++) {
            if (GLFW.glfwJoystickPresent(i)) {
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
        return Collections.unmodifiableList(GAMEPADS);
    }

    /**
     * Returns a list of active GLWindow objects.
     *
     * @return the list of windows.
     * @since 15.06.07
     */
    public static List<GLWindow> listActiveWindows() {
        final List<GLWindow> windows = new ArrayList<>(1);

        windows.addAll(WINDOWS.values());

        return Collections.unmodifiableList(windows);
    }

    /**
     * Registers a callback to right before the window is closed. To prevent the
     * window from closing, call setShouldClose() and pass 'false',
     *
     * @param onBeforeCloseCallback the callback to run
     * @since 15.06.24
     */
    public void setOnBeforeClose(final Runnable onBeforeCloseCallback) {       
        this.beforeClose = Optional.ofNullable(onBeforeCloseCallback);
    }

    /**
     * Registers a callback to run when the window is closed.
     *
     * @param onCloseCallback the callback to run
     * @since 15.06.24
     */
    public void setOnClose(final Runnable onCloseCallback) {        
        this.onClose = Optional.ofNullable(onCloseCallback);
    }

    /**
     * Registers a callback to run when the window is minimized (aka,
     * iconified).
     *
     * @param onMinimizeCallback the callback to run
     * @since 15.06.24
     */
    public void setOnMinimize(final Runnable onMinimizeCallback) {        
        this.onMinimize = Optional.ofNullable(onMinimizeCallback);
    }

    /**
     * Registers a callback to run when the window is restored from being
     * minimized.
     *
     * @param onRestoreCallback the callback to run
     * @since 15.06.24
     */
    public void setOnRestore(final Runnable onRestoreCallback) {        
        this.onRestore = Optional.ofNullable(onRestoreCallback);
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

        LOGGER.trace(GLFW_MARKER, "Constructed GLWindow! [width={}, height={}, title={}, parent={}]", width, height, title, shared != null ? shared.title : "null");
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
            if (!GLWindow.this.isValid()) {
                throw new GLFWException("GLWindow is not valid!");
            }

            final long m = GLWindow.this.monitor == 0 ? GLFW.glfwGetPrimaryMonitor() : GLWindow.this.monitor;            
            final long mHandle = GLFW.glfwGetWindowMonitor(m);            
            final GLFWVidMode mode = GLFW.glfwGetVideoMode(mHandle);

            final int[] widthMM = {0};
            final int[] heightMM = {0};            

            GLFW.glfwGetMonitorPhysicalSize(mHandle, widthMM, heightMM);

            final int vWidth = mode.width();
            final double dpi = (vWidth / (widthMM[0] / 25.4 /* mm to in */));            

            return dpi;
        }
    }

    private class InitTask extends GLTask {

        @Override
        public void run() {            
            GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_VISIBLE, GL_FALSE)");
            glfwWindowHint(GLFW.GLFW_VISIBLE, GL_FALSE);
            GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_RESIZABLE, GL_TRUE)");
            glfwWindowHint(GLFW.GLFW_RESIZABLE, GL_TRUE);

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, {})", VERSION_MAJOR);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, VERSION_MAJOR);
            GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, {})", VERSION_MINOR);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, VERSION_MINOR);

            if (USE_EGL) {
                GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_EGL_CONTEXT_API)");
                glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_EGL_CONTEXT_API);
            }

            switch (CLIENT_API) {
                case VULKAN:
                    GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)");
                    glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
                    break;
                case OPENGL:
                    GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API)");
                    glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);

                    if (VERSION_MAJOR == 3) {
                        if (VERSION_MINOR == 2 || VERSION_MINOR == 3) {
                            GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)");
                            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
                        }
                    } else if (VERSION_MAJOR > 3) {
                        GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)");
                        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
                    }                    
                    break;
                case OPENGLES:
                    GLFW_LOGGER.trace(GLFW_MARKER, "glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_ES_API)");
                    glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_ES_API);
                    break;
                default:
                    throw new IllegalStateException("Unsupported client API: " + CLIENT_API);
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
            
            GLFW.glfwMakeContextCurrent(GLWindow.this.window);

            switch (CLIENT_API) {
                case OPENGL:
                    GL.createCapabilities();
                    break;
                case OPENGLES:
                    GLES.createCapabilities();
                    break;

            }

            GLFW_LOGGER.trace(GLFW_MARKER, "glfwSwapInterval({})", OPENGL_SWAP_INTERVAL);
            GLFW.glfwSwapInterval(OPENGL_SWAP_INTERVAL);

            final int[] fbWidth = {0};
            final int[] fbHeight = {0};
            
            GLFW.glfwGetFramebufferSize(GLWindow.this.window, fbWidth, fbHeight);

            GLWindow.this.thread.currentViewport = new GLViewport(0, 0, fbWidth[0], fbHeight[0]);
            GLWindow.this.handler.register();

            WINDOWS.put(GLWindow.this.window, GLWindow.this);
            GLWindow.this.hasInitialized = true;

            GLFW.glfwSetKeyCallback(GLWindow.this.window, GLWindow.this.keyCallback.get());
            GLFW.glfwSetMouseButtonCallback(GLWindow.this.window, GLWindow.this.mouseButtonCallback.get());
            GLFW.glfwSetCursorEnterCallback(GLWindow.this.window, GLWindow.this.cursorEnterCallback.get());
            GLFW.glfwSetCursorPosCallback(GLWindow.this.window, GLWindow.this.cursorPosCallback.get());
            GLFW.glfwSetScrollCallback(GLWindow.this.window, GLWindow.this.scrollCallback.get());
            GLFW.glfwSetCharCallback(GLWindow.this.window, GLWindow.this.charCallback.get());
            GLFW.glfwSetWindowCloseCallback(GLWindow.this.window, GLWindow.this.windowCloseCallback.get());
            GLFW.glfwSetWindowIconifyCallback(GLWindow.this.window, GLWindow.this.windowIconifyCallback.get());
        }
    }

    /**
     * Retrieves the width of the primary monitor in pixels.
     *
     * @return the width of the primary monitor in pixels.
     * @since 16.08.31
     */
    public static int getPrimaryMonitorWidth() {
        final long primary = GLFW.glfwGetPrimaryMonitor();
        final GLFWVidMode mode = GLFW.glfwGetVideoMode(primary);

        return mode.width();
    }

    /**
     * Retrieves the height of the primary monitor in pixels.
     *
     * @return the height of the primary monitor.
     * @since 16.08.31
     */
    public static int getPrimaryMonitorHeight() {
        final long primary = GLFW.glfwGetPrimaryMonitor();
        final GLFWVidMode mode = GLFW.glfwGetVideoMode(primary);

        return mode.height();
    }

    /**
     * Sets the fullscreen state for the window. This will use the current
     * window's size for setting the fullscreen. If the window size and monitor
     * size match, windowed fullscreen mode will be used.
     *
     * @param fullscreen the fullscreen settings.
     * @since 16.08.31
     */
    public void setFullscreen(final boolean fullscreen) {
        new SetFullscreenTask(fullscreen, GLFW.GLFW_DONT_CARE, GLFW.GLFW_DONT_CARE).glRun(this.getGLThread());
    }

    /**
     * Sets the fullscreen state for the window. If the width and height match
     * the primary monitor size, then windowed fullscreen is used.
     *
     * @param fullscreen the fullscreen setting.
     * @param preferredWidth the maximized width.
     * @param preferredHeight the maximized height.
     * @since 15.10.30
     */
    public void setFullscreen(
            final boolean fullscreen,
            final int preferredWidth, final int preferredHeight) {

        new SetFullscreenTask(fullscreen, preferredWidth, preferredHeight).glRun(this.getGLThread());
    }

    private final List<Runnable> onContextLost = new ArrayList<>(0);

    /**
     * Adds a callback for when the OpenGL context is lost.
     *
     * @param callback method to run when the OpenGL context is lost.
     * @since 15.10.30
     */
    public void addContextLostListener(final Runnable callback) {        
        this.onContextLost.add(callback);
    }

    /**
     * Remove a context lost callback.
     *
     * @param callback the callback to remove.
     * @since 15.10.30
     */
    public void removeContextLostListener(final Runnable callback) {        
        this.onContextLost.remove(callback);
    }

    private class SetFullscreenTask extends GLTask {

        private final boolean isFullscreen;
        private final int preferredWidth;
        private final int preferredHeight;

        SetFullscreenTask(boolean useFS, final int preferredWidth, final int preferredHeight) {
            this.isFullscreen = useFS;
            this.preferredWidth = (preferredWidth == GLFW.GLFW_DONT_CARE) ? GLWindow.this.width : preferredWidth;
            this.preferredHeight = (preferredHeight == GLFW.GLFW_DONT_CARE) ? GLWindow.this.height : preferredHeight;
        }

        @Override
        public void run() {            
            final long monitor = isFullscreen ? GLFW.glfwGetPrimaryMonitor() : NULL;

            if (monitor != NULL) {
                final GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);

                glfwWindowHint(GLFW_RED_BITS, mode.redBits());
                glfwWindowHint(GLFW_BLUE_BITS, mode.blueBits());
                glfwWindowHint(GLFW_GREEN_BITS, mode.greenBits());
                glfwWindowHint(GLFW_REFRESH_RATE, mode.refreshRate());

                GLFW.glfwSetWindowMonitor(window, monitor, 0, 0, this.preferredWidth, this.preferredHeight, mode.refreshRate());
            } else {
                GLFW.glfwSetWindowMonitor(window, monitor, 0, 0, this.preferredWidth, this.preferredHeight, GLFW.GLFW_DONT_CARE);
            }
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
            if (!GLWindow.this.isValid()) {
                throw new GLFWException("GLWindow is not valid!");
            }

            if (this.visibility) {                
                GLFW.glfwShowWindow(GLWindow.this.window);
            } else {                
                GLFW.glfwHideWindow(GLWindow.this.window);
            }            
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

    /**
     * Sets the position of this window.
     * @param x the x coordinate of the window
     * @param y the y coordinate of the window
     */
    public void setPosition(final int x, final int y){
        new SetWindowPositionTask(x, y).glRun(this.getGLThread());
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
            if (!GLWindow.this.isValid()) {
                throw new GLFWException("GLWindow is not valid!");
            }
            
            GLFW.glfwSetWindowSize(GLWindow.this.window, this.width, this.height);
        }

    }

    public class SetWindowPositionTask extends GLTask {

        final int x;
        final int y;

        public SetWindowPositionTask(final int x, final int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void run() {            
            if (!GLWindow.this.isValid()) {
                throw new GLFWException("GLWindow is not valid!");
            }
            
            GLFW.glfwSetWindowPos(GLWindow.this.window, this.x, this.y);
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
            if (!GLWindow.this.isValid()) {
                throw new GLFWException("GLWindow is not valid!");
            }

            final int[] width = {0};
            final int[] height = {0};
            
            GLFW.glfwGetFramebufferSize(GLWindow.this.window, width, height);

            final int[] size = new int[]{width[0], height[0]};

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

            final int[] x = {0};
            final int[] y = {0};
            
            GLFW.glfwGetWindowPos(GLWindow.this.window, x, y);

            return new int[]{x[0], y[0]};
        }
    }

    public class WindowFrameSizeQuery extends GLQuery<int[]> {

        @Override
        public int[] call() throws Exception {
            if (!GLWindow.this.isValid()) {
                throw new GLFWException("GLWindow is not valid!");
            }

            final int[] l = {0};
            final int[] t = {0};
            final int[] r = {0};
            final int[] b = {0};
            
            GLFW.glfwGetWindowFrameSize(GLWindow.this.window, l, t, r, b);

            final int[] size = new int[]{l[0], t[0], r[0], b[0]};

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
            if (!GLWindow.this.isValid()) {
                throw new GLFWException("GLWindow is not valid!");
            }

            final int[] w = {0};
            final int[] h = {0};

            GLFW.glfwGetWindowSize(GLWindow.this.window, w, h);

            final int[] size = new int[]{w[0], h[0]};

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
            if (GLFW.glfwWindowShouldClose(GLWindow.this.window)) {
                GLWindow.this.cleanup();
            } else {                
                GLFW.glfwSwapBuffers(GLWindow.this.window);                
                GLFW.glfwPollEvents();                
            }            
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
            if (!GLWindow.this.isValid()) {
                throw new GLFWException("GLWindow is not valid!");
            }
            
            GLFW.glfwSetWindowShouldClose(GLWindow.this.window, true);
        }
    }

    private void cleanup() {        
        this.cleanupTasks.forEach(Runnable::run);
        this.cleanupTasks.clear();
        this.workerThreads.forEach(GLWindow::close);
        this.cursorEnterCallback.ifInitialized(GLFWCursorEnterCallback::free);
        this.cursorPosCallback.ifInitialized(GLFWCursorPosCallback::free);
        this.keyCallback.ifInitialized(GLFWKeyCallback::free);
        this.charCallback.ifInitialized(GLFWCharCallback::free);
        this.mouseButtonCallback.ifInitialized(GLFWMouseButtonCallback::free);
        this.scrollCallback.ifInitialized(GLFWScrollCallback::free);
        this.resizeCallback.ifPresent(GLFWFramebufferSizeCallback::free);
        this.windowCloseCallback.ifInitialized(GLFWWindowCloseCallback::free);
        this.windowIconifyCallback.ifInitialized(GLFWWindowIconifyCallback::free);
        
        this.onClose.ifPresent(Runnable::run);

        // stop everything
        this.thread.submit(() -> {            
            GLFW.glfwDestroyWindow(this.window);

            WINDOWS.remove(this.window);            
            this.window = GLWindow.INVALID_WINDOW_ID;
        });
        
        this.thread.shutdown();
    }

    private final List<GLWindow> workerThreads = new ArrayList<>(0);

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
        return this.cleanupTasks.remove(task);
    }

    /**
     * Removes all tasks from the cleanup task queue.
     *
     * @since 15.06.30
     */
    public void clearCleanup() {        
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
