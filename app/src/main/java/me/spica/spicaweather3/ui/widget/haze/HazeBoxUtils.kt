package me.spica.spicaweather3.ui.widget.haze

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.os.SystemClock
import android.util.Log
import android.view.TextureView
import android.view.ViewGroup
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLContext

/**
 * 烟雾效果 TextureView
 * 使用 OpenGL ES 2.0 渲染高斯雾团升腾效果
 */
class HazeTextureView(
  context: Context,
  private val renderer: HazeRenderer,
) : TextureView(context), TextureView.SurfaceTextureListener {

  private var renderThread: HazeRenderThread? = null

  init {
    isOpaque = false
    surfaceTextureListener = this
    // 确保 Compose AndroidView 将 TextureView 撑满父容器
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT,
    )
  }

  override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
    renderer.pendingWidth = width.coerceAtLeast(1)
    renderer.pendingHeight = height.coerceAtLeast(1)
    renderThread = HazeRenderThread(surface, renderer).also { it.start() }
  }

  override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
    renderer.pendingWidth = width.coerceAtLeast(1)
    renderer.pendingHeight = height.coerceAtLeast(1)
  }

  override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
    renderThread?.let {
      it.stopRendering()
      it.join(2_000)
    }
    renderThread = null
    return true
  }

  override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

  fun onResume() {
    renderThread?.paused = false
  }

  fun onPause() {
    renderThread?.paused = true
  }
}

/**
 * OpenGL 渲染线程，负责 EGL 上下文管理和帧循环
 */
private class HazeRenderThread(
  private val surfaceTexture: SurfaceTexture,
  private val renderer: HazeRenderer,
) : Thread("HazeRenderThread") {

  @Volatile var paused: Boolean = false
  @Volatile private var running = true

  private lateinit var egl: EGL10
  private var eglDisplay: javax.microedition.khronos.egl.EGLDisplay? = null
  private var eglContext: javax.microedition.khronos.egl.EGLContext? = null
  private var eglSurface: javax.microedition.khronos.egl.EGLSurface? = null

  override fun run() {
    if (!setupEGL()) return
    renderer.onSurfaceCreated()
    while (running) {
      if (!paused) {
        renderer.onDrawFrame()
        egl.eglSwapBuffers(eglDisplay, eglSurface)
      }
      sleep(16L)
    }
    cleanupEGL()
  }

  private fun setupEGL(): Boolean {
    return try {
      egl = EGLContext.getEGL() as EGL10
      eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
      egl.eglInitialize(eglDisplay, IntArray(2))

      val configAttribs = intArrayOf(
        EGL10.EGL_RED_SIZE, 8,
        EGL10.EGL_GREEN_SIZE, 8,
        EGL10.EGL_BLUE_SIZE, 8,
        EGL10.EGL_ALPHA_SIZE, 8,
        EGL10.EGL_RENDERABLE_TYPE, 4, // EGL_OPENGL_ES2_BIT
        EGL10.EGL_NONE,
      )
      val configs = arrayOfNulls<javax.microedition.khronos.egl.EGLConfig>(1)
      val numConfigs = IntArray(1)
      egl.eglChooseConfig(eglDisplay, configAttribs, configs, 1, numConfigs)
      if (numConfigs[0] == 0 || configs[0] == null) return false

      val contextAttribs = intArrayOf(
        0x3098, 2, // EGL_CONTEXT_CLIENT_VERSION = 2
        EGL10.EGL_NONE,
      )
      eglContext = egl.eglCreateContext(eglDisplay, configs[0], EGL10.EGL_NO_CONTEXT, contextAttribs)
      eglSurface = egl.eglCreateWindowSurface(eglDisplay, configs[0], surfaceTexture, null)
      egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
      true
    } catch (e: Exception) {
      false
    }
  }

  private fun cleanupEGL() {
    if (!::egl.isInitialized) return
    egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
    egl.eglDestroySurface(eglDisplay, eglSurface)
    egl.eglDestroyContext(eglDisplay, eglContext)
    egl.eglTerminate(eglDisplay)
  }

  fun stopRendering() {
    running = false
  }
}

/**
 * 烟雾 OpenGL ES 渲染器
 * 基于三倍频 Simplex 噪声实现自然流动的雾气，半透明叠加在天气背景渐变之上
 */
class HazeRenderer {

  @Volatile var fogColorR: Float = 1f
  @Volatile var fogColorG: Float = 1f
  @Volatile var fogColorB: Float = 1f
  @Volatile var pendingWidth: Int = 1
  @Volatile var pendingHeight: Int = 1

  private var program = 0
  private var positionHandle = 0
  private var texCoordHandle = 0
  private var resolutionHandle = 0
  private var animTimeHandle = 0
  private var fogColorHandle = 0
  private var surfaceWidth = 1
  private var surfaceHeight = 1
  private var startTimeMs = 0L

  // 全屏四边形顶点：(position.xy, texCoord.xy)
  // GL NDC: 左下=(-1,-1), 右上=(1,1)   TexCoord: 左上=(0,0), 右下=(1,1)
  private val vertexBuffer: FloatBuffer = ByteBuffer
    .allocateDirect(FULLSCREEN_VERTICES.size * 4)
    .order(ByteOrder.nativeOrder())
    .asFloatBuffer()
    .apply { put(FULLSCREEN_VERTICES); position(0) }

  fun onSurfaceCreated() {
    program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
    positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
    texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
    resolutionHandle = GLES20.glGetUniformLocation(program, "uResolution")
    animTimeHandle = GLES20.glGetUniformLocation(program, "uAnimTime")
    fogColorHandle = GLES20.glGetUniformLocation(program, "uFogColor")
    startTimeMs = SystemClock.elapsedRealtime()

    GLES20.glDisable(GLES20.GL_DEPTH_TEST)
    GLES20.glDisable(GLES20.GL_CULL_FACE)
    // 分离 RGB/Alpha 混合：RGB 用标准 src-alpha，Alpha 通道直接写入以供 TextureView 正确合成
    GLES20.glEnable(GLES20.GL_BLEND)
    GLES20.glBlendFuncSeparate(
      GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA,
      GLES20.GL_ONE,       GLES20.GL_ONE_MINUS_SRC_ALPHA,
    )
    GLES20.glClearColor(0f, 0f, 0f, 0f)
  }

  fun updateFogColor(r: Float, g: Float, b: Float) {
    fogColorR = r
    fogColorG = g
    fogColorB = b
  }

  fun onDrawFrame() {
    if (program == 0) return

    if (surfaceWidth != pendingWidth || surfaceHeight != pendingHeight) {
      surfaceWidth = pendingWidth
      surfaceHeight = pendingHeight
      GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
    }

    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    GLES20.glUseProgram(program)

    vertexBuffer.position(0)
    GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, STRIDE_BYTES, vertexBuffer)
    GLES20.glEnableVertexAttribArray(positionHandle)

    vertexBuffer.position(2)
    GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, STRIDE_BYTES, vertexBuffer)
    GLES20.glEnableVertexAttribArray(texCoordHandle)

    val elapsed = (SystemClock.elapsedRealtime() - startTimeMs) % 3_600_000L
    GLES20.glUniform2f(resolutionHandle, surfaceWidth.toFloat(), surfaceHeight.toFloat())
    GLES20.glUniform1f(animTimeHandle, elapsed / 1000f)
    GLES20.glUniform3f(fogColorHandle, fogColorR, fogColorG, fogColorB)

    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

    GLES20.glDisableVertexAttribArray(positionHandle)
    GLES20.glDisableVertexAttribArray(texCoordHandle)
  }

  private fun createProgram(vertexSource: String, fragmentSource: String): Int {
    val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
    val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
    return GLES20.glCreateProgram().also {
      GLES20.glAttachShader(it, vs)
      GLES20.glAttachShader(it, fs)
      GLES20.glLinkProgram(it)
      GLES20.glDeleteShader(vs)
      GLES20.glDeleteShader(fs)
    }
  }

  private fun compileShader(type: Int, source: String): Int {
    val shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, source)
    GLES20.glCompileShader(shader)
    val status = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
    if (status[0] == 0) {
      Log.e("HazeRenderer", "Shader compile error: ${GLES20.glGetShaderInfoLog(shader)}")
      GLES20.glDeleteShader(shader)
      return 0
    }
    return shader
  }

  companion object {
    private const val STRIDE_BYTES = 4 * 4

    private val FULLSCREEN_VERTICES = floatArrayOf(
      -1f, -1f, 0f, 1f, // 左下：position(-1,-1), texCoord(0,1)
       1f, -1f, 1f, 1f, // 右下：position(1,-1),  texCoord(1,1)
      -1f,  1f, 0f, 0f, // 左上：position(-1,1),  texCoord(0,0)
       1f,  1f, 1f, 0f, // 右上：position(1,1),   texCoord(1,0)
    )

    private val VERTEX_SHADER = """
      attribute vec2 aPosition;
      attribute vec2 aTexCoord;
      varying vec2 vTexCoord;
      void main() {
          vTexCoord = aTexCoord;
          gl_Position = vec4(aPosition, 0.0, 1.0);
      }
    """.trimIndent()

    // 高斯雾团升腾着色器 v3
    // 黄金比例相位偏移 + 双频正弦摇曳 + 各团独立周期 → 自然飘散效果
    private val FRAGMENT_SHADER = """
      precision highp float;

      varying vec2  vTexCoord;
      uniform vec2  uResolution;
      uniform float uAnimTime;
      uniform vec3  uFogColor;

      // cx      : 水平出生点 [0,1]
      // phOff   : 黄金比例相位偏移，打破 Z 形规律
      // period  : 该团从底到顶的时间（秒），各团不同，避免同步
      // szMax   : 最大半径
      // swayAmp : 横向摇曳幅度
      // asp     : 宽高比 W/H
      float puff(vec2 uv, float cx, float phOff, float period,
                 float szMax, float swayAmp, float asp) {
          float ph = fract(uAnimTime / period + phOff);

          // 垂直：smoothstep 缓动，底部(1.0) → 顶部(0.0)
          float cy = 1.0 - ph * ph * (3.0 - 2.0 * ph);

          // 双频正弦横向摇曳：随高度增大，模拟在上升气流中漂移
          // 两频率互质（比值≈0.71）产生非周期性自然路径
          float st = uAnimTime * 0.38;
          float sway = (sin(st          + phOff * 6.28) * 1.0
                      + sin(st * 0.71   + phOff * 9.42) * 0.45) * swayAmp * ph;

          // 从底部极小团膨胀到顶部大而稀薄
          float sz = mix(szMax * 0.10, szMax * 1.25, ph);

          float dx = (uv.x - cx - sway) / sz;
          float dy = (uv.y - cy) / (sz * asp);
          float density = exp(-(dx * dx + dy * dy) * 2.0);

          // 快速淡入，在半途即开始消散（ph=0.42 开始淡出）→ 飘散效果
          float env = smoothstep(0.0, 0.10, ph) * (1.0 - smoothstep(0.42, 1.0, ph));

          return density * env;
      }

      void main() {
          float asp = max(uResolution.x / uResolution.y, 0.1);
          vec2  uv  = vTexCoord;

          // 屏幕叠加（Screen Blend）：inv = Π(1 - puff_i × weight)
          // 每增加一个雾团只是让不透明度逐渐趋近 1.0，不会产生截断边界，
          // 因此永远不会出现"光圈"—— 任何位置的密度都是连续平滑变化的。
          float inv = 1.0;
          inv *= (1.0 - puff(uv, 0.12, 0.000, 7.0, 0.32, 0.08, asp) * 0.54);
          inv *= (1.0 - puff(uv, 0.35, 0.618, 8.5, 0.28, 0.07, asp) * 0.48);
          inv *= (1.0 - puff(uv, 0.58, 0.236, 6.5, 0.36, 0.09, asp) * 0.58);
          inv *= (1.0 - puff(uv, 0.80, 0.854, 9.0, 0.26, 0.06, asp) * 0.46);
          inv *= (1.0 - puff(uv, 0.22, 0.472, 7.8, 0.31, 0.08, asp) * 0.52);
          inv *= (1.0 - puff(uv, 0.68, 0.090, 8.2, 0.34, 0.10, asp) * 0.56);
          inv *= (1.0 - puff(uv, 0.45, 0.708, 6.8, 0.27, 0.07, asp) * 0.48);
          inv *= (1.0 - puff(uv, 0.88, 0.326, 9.3, 0.30, 0.08, asp) * 0.52);
          inv *= (1.0 - puff(uv, 0.05, 0.944, 7.4, 0.29, 0.06, asp) * 0.46);
          inv *= (1.0 - puff(uv, 0.52, 0.562, 8.7, 0.33, 0.09, asp) * 0.54);
          inv *= (1.0 - puff(uv, 0.75, 0.180, 6.3, 0.28, 0.07, asp) * 0.50);
          inv *= (1.0 - puff(uv, 0.30, 0.798, 9.6, 0.37, 0.10, asp) * 0.58);

          // Screen blend 结果自然在 [0,1]，无需截断 → 无光圈
          float a = 1.0 - inv;
          gl_FragColor = vec4(uFogColor, a);
      }
    """.trimIndent()
  }
}
