package me.spica.spicaweather3.utils.thanos_effect

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Choreographer
import android.view.TextureView

@SuppressLint("ViewConstructor") // 仅通过 AndroidView 创建，不参与 XML 布局加载。
internal class ThanosEffectTextureView(
    context: Context,
    private val snapshot: ThanosEffectSnapshot,
    private val config: ThanosEffectConfig,
    private val density: Float,
    private val onFirstFrame: () -> Unit,
    private val onComplete: () -> Unit,
) : TextureView(context), TextureView.SurfaceTextureListener {
    private var renderThread: ThanosEffectRenderThread? = null
    private var released = false
    private var firstFramePresented = false

    init {
        isOpaque = false
        isClickable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (released) return
        renderThread = ThanosEffectRenderThread(surface, width, height, snapshot, config, density) {
            post { onComplete() }
        }.also { it.start() }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        // 截图使用原窗口坐标，窗口尺寸变化后结束动画，避免位置错位。
        renderThread?.requestStop(releaseSurface = false)
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // 等待 TextureView 实际呈现首帧后再隐藏原内容，eglSwapBuffers 仅表示已提交缓冲区。
        if (!released && !firstFramePresented) {
            firstFramePresented = true
            onFirstFrame()
        }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        val thread = renderThread ?: return true
        thread.requestStop(releaseSurface = true)
        renderThread = null
        return false // GL 线程解绑 EGL 后负责释放 Surface。
    }

    fun release() {
        if (released) return
        released = true
        renderThread?.requestStop(releaseSurface = false)
    }

    override fun onDetachedFromWindow() {
        release()
        super.onDetachedFromWindow()
    }
}

/** 每个线程独占一次动画和一个 Surface，GL 操作和资源清理均在该线程执行。 */
private class ThanosEffectRenderThread(
    private val surface: SurfaceTexture,
    private val width: Int,
    private val height: Int,
    private val snapshot: ThanosEffectSnapshot,
    private val config: ThanosEffectConfig,
    private val density: Float,
    private val onComplete: () -> Unit,
) : HandlerThread("ThanosEffectRender"), Choreographer.FrameCallback {
    @Volatile private var stopRequested = false
    @Volatile private var renderHandler: Handler? = null
    private val surfaceLock = Any()
    private var finished = false
    private var releaseSurfaceOnFinish = false
    private var surfaceReleased = false
    private var choreographer: Choreographer? = null
    private var startTimeNanos = 0L
    private var renderer: ThanosEffectRenderer? = null
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    fun requestStop(releaseSurface: Boolean) {
        synchronized(surfaceLock) {
            stopRequested = true
            if (releaseSurface && !surfaceReleased) {
                if (finished) {
                    surface.release()
                    surfaceReleased = true
                } else {
                    releaseSurfaceOnFinish = true
                }
            }
        }
        renderHandler?.post { finishRendering() }
    }

    override fun onLooperPrepared() {
        renderHandler = Handler(looper)
        choreographer = Choreographer.getInstance()
        if (stopRequested) finishRendering() else choreographer!!.postFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (stopRequested) {
            finishRendering()
            return
        }
        try {
            if (renderer == null) {
                initializeEgl()
                val scene = ThanosEffectRenderer(snapshot, config, density)
                renderer = scene
                scene.initialize()
            }
            if (stopRequested) {
                finishRendering()
                return
            }
            if (startTimeNanos == 0L) startTimeNanos = frameTimeNanos
            val running = renderer!!.draw(width, height, (frameTimeNanos - startTimeNanos) / 1_000_000f)
            checkEgl(EGL14.eglSwapBuffers(eglDisplay, eglSurface), "eglSwapBuffers")
            if (running) choreographer!!.postFrameCallback(this) else finishRendering()
        } catch (error: RuntimeException) {
            Log.w("ThanosEffect", "Dust rendering failed; completing without animation", error)
            finishRendering()
        }
    }

    private fun initializeEgl() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        checkEgl(eglDisplay != EGL14.EGL_NO_DISPLAY, "eglGetDisplay")
        checkEgl(EGL14.eglInitialize(eglDisplay, null, 0, null, 0), "eglInitialize")
        val attributes = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        checkEgl(
            EGL14.eglChooseConfig(eglDisplay, attributes, 0, configs, 0, 1, count, 0) && count[0] > 0,
            "eglChooseConfig",
        )
        val config = checkNotNull(configs[0])
        eglContext = EGL14.eglCreateContext(
            eglDisplay, config, EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE), 0,
        )
        checkEgl(eglContext != EGL14.EGL_NO_CONTEXT, "eglCreateContext")
        eglSurface = EGL14.eglCreateWindowSurface(
            eglDisplay, config, surface, intArrayOf(EGL14.EGL_NONE), 0,
        )
        checkEgl(eglSurface != EGL14.EGL_NO_SURFACE, "eglCreateWindowSurface")
        checkEgl(EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext), "eglMakeCurrent")
    }

    private fun checkEgl(success: Boolean, operation: String) {
        check(success) { "$operation failed: 0x${Integer.toHexString(EGL14.eglGetError())}" }
    }

    private fun finishRendering() {
        synchronized(surfaceLock) { if (finished) return }
        choreographer?.removeFrameCallback(this)
        try {
            releaseGraphics()
        } finally {
            synchronized(surfaceLock) {
                finished = true
                if (releaseSurfaceOnFinish && !surfaceReleased) {
                    surface.release()
                    surfaceReleased = true
                }
            }
            quitSafely()
            onComplete()
        }
    }

    private fun releaseGraphics() {
        try {
            renderer?.release()
        } finally {
            renderer = null
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(
                    eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT,
                )
                if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(eglDisplay, eglSurface)
                if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(eglDisplay, eglContext)
                EGL14.eglTerminate(eglDisplay)
                EGL14.eglReleaseThread()
            }
            eglSurface = EGL14.EGL_NO_SURFACE
            eglContext = EGL14.EGL_NO_CONTEXT
            eglDisplay = EGL14.EGL_NO_DISPLAY
        }
    }
}
