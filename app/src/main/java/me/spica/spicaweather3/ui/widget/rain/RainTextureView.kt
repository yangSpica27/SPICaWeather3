package me.spica.spicaweather3.ui.widget.rain

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES30
import android.view.TextureView
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 雨滴动画 TextureView（OpenGL ES 3.0 渲染）
 *
 * 架构：
 *  - 继承 TextureView，透明背景叠加在 WeatherBackground 之上
 *  - 在专用渲染线程上运行 EGL14 + OpenGL ES 3.0 渲染循环
 *  - SurfaceTexture 可用时初始化 EGL 并启动渲染；销毁时停止并清理
 *  - 渲染循环目标 60 fps，通过 Thread.sleep 控制帧率
 *
 * 透明度处理：
 *  - EGL 配置使用 EGL_ALPHA_SIZE = 8（支持透明通道）
 *  - 每帧 glClearColor(0,0,0,0) 清除画布为完全透明
 *  - TextureView.isOpaque = false，让系统进行正确的混合合成
 */
class RainTextureView(context: Context) : TextureView(context), TextureView.SurfaceTextureListener {

    private lateinit var simulation: RainSimulation
    private var renderer: RainGLRenderer? = null

    // EGL 句柄
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    // 渲染线程控制
    private val running = AtomicBoolean(false)
    private var renderThread: Thread? = null

    // 视口尺寸（@Volatile 保证跨线程可见）
    @Volatile private var surfaceWidth = 1
    @Volatile private var surfaceHeight = 1

    init {
        isOpaque = false          // 关键：允许透明通道透过 TextureView
        surfaceTextureListener = this
    }

    // ─────────────────── SurfaceTextureListener ───────────────────

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        simulation = RainSimulation(width, height).also { it.init() }
        startRenderLoop(surface)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        stopRenderLoop()   // 阻塞直到渲染线程退出，EGL 资源在线程内释放
        return true        // 返回 true 告知系统我们自己管理 SurfaceTexture 生命周期
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    // ─────────────────── 渲染线程管理 ───────────────────

    private fun startRenderLoop(surface: SurfaceTexture) {
        running.set(true)
        renderThread = Thread({
            // 所有 GL/EGL 操作都在此线程内执行
            try {
                initEGL(surface)
                val r = RainGLRenderer()
                renderer = r
                r.init()

                while (running.get()) {
                    val frameStart = System.nanoTime()

                    // JBox2D 物理步进（固定 1/120s 步长，内部自行管理时间）
                    simulation.update()

                    // 绘制
                    val w = surfaceWidth
                    val h = surfaceHeight
                    GLES30.glViewport(0, 0, w, h)
                    GLES30.glClearColor(0f, 0f, 0f, 0f)
                    GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
                    r.draw(simulation, w, h)
                    EGL14.eglSwapBuffers(eglDisplay, eglSurface)

                    // 控制帧率约 60 fps
                    val elapsed = System.nanoTime() - frameStart
                    val sleepNs = FRAME_INTERVAL_NS - elapsed
                    if (sleepNs > 0L) {
                        Thread.sleep(sleepNs / 1_000_000L, (sleepNs % 1_000_000L).toInt())
                    }
                }

                r.release()
            } finally {
                releaseEGL()
            }
        }, "RainRenderThread")
        renderThread!!.start()
    }

    private fun stopRenderLoop() {
        running.set(false)
        renderThread?.join(3_000L)  // 最多等待 3 秒，渲染线程通常在一帧内退出
        renderThread = null
        renderer = null
    }

    // ─────────────────── EGL 初始化与销毁 ───────────────────

    private fun initEGL(surface: SurfaceTexture) {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        EGL14.eglInitialize(eglDisplay, null, 0, null, 0)

        // 请求支持透明通道的 EGL 配置（ALPHA_SIZE=8）
        val configAttribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE,    EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_RED_SIZE,        8,
            EGL14.EGL_GREEN_SIZE,      8,
            EGL14.EGL_BLUE_SIZE,       8,
            EGL14.EGL_ALPHA_SIZE,      8,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)

        // 创建 OpenGL ES 3.0 上下文
        val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(
            eglDisplay, configs[0]!!, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0
        )

        // 将 SurfaceTexture 作为 EGL 窗口 Surface
        val surfAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0]!!, surface, surfAttribs, 0)

        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
    }

    private fun releaseEGL() {
        EGL14.eglMakeCurrent(
            eglDisplay,
            EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT
        )
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            eglSurface = EGL14.EGL_NO_SURFACE
        }
        if (eglContext != EGL14.EGL_NO_CONTEXT) {
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            eglContext = EGL14.EGL_NO_CONTEXT
        }
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglTerminate(eglDisplay)
            eglDisplay = EGL14.EGL_NO_DISPLAY
        }
    }

    companion object {
        private const val FRAME_INTERVAL_NS = 16_666_667L // ~60 fps
    }
}
