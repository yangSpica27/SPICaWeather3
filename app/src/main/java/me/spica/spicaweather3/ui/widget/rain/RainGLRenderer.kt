package me.spica.spicaweather3.ui.widget.rain

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.sqrt

/**
 * OpenGL ES 3.0 雨滴渲染器
 *
 * 双 Program 渲染策略：
 *  - dropProgram   : 下落雨滴 → GL_TRIANGLES 拖尾细长条
 *  - splashProgram : 溅落水花 → GL_POINTS + 径向渐变 fragment shader
 *
 * GL_POINTS 溅落方案消除"颗粒感"：每个水花粒子渲染成一个带 radial gradient 的
 * 柔和圆点，叠加模式 (additive blend) 下多个圆点自然融合成水花光晕，无硬边缘。
 */
class RainGLRenderer {

    // ───────── GLSL 着色器源码 ─────────

    // 下落雨滴顶点着色器（GL_TRIANGLES，忽略 gl_PointSize）
    private val DROP_VERT = """
        #version 300 es
        layout(location = 0) in vec2 a_position;
        layout(location = 1) in float a_alpha;
        uniform vec2 u_resolution;
        out float v_alpha;
        void main() {
            vec2 ndc = (a_position / u_resolution) * 2.0 - 1.0;
            ndc.y = -ndc.y;
            gl_Position = vec4(ndc, 0.0, 1.0);
            v_alpha = a_alpha;
        }
    """.trimIndent()

    // 下落雨滴片元着色器：白色半透明亮条
    private val DROP_FRAG = """
        #version 300 es
        precision mediump float;
        in float v_alpha;
        uniform vec4 u_color;
        out vec4 fragColor;
        void main() {
            fragColor = vec4(u_color.rgb, u_color.a * v_alpha);
        }
    """.trimIndent()

    // 水花 GL_POINTS 顶点着色器：用 a_alpha 控制点大小
    private val SPLASH_VERT = """
        #version 300 es
        layout(location = 0) in vec2 a_position;
        layout(location = 1) in float a_alpha;
        uniform vec2 u_resolution;
        uniform float u_pt_base;
        uniform float u_pt_range;
        out float v_alpha;
        void main() {
            vec2 ndc = (a_position / u_resolution) * 2.0 - 1.0;
            ndc.y = -ndc.y;
            gl_Position = vec4(ndc, 0.0, 1.0);
            gl_PointSize = u_pt_base + a_alpha * u_pt_range;
            v_alpha = a_alpha;
        }
    """.trimIndent()

    // 水花片元着色器：双层 smoothstep 径向渐变——无硬边缘，自然水花光晕
    private val SPLASH_FRAG = """
        #version 300 es
        precision mediump float;
        in float v_alpha;
        uniform vec4 u_color;
        out vec4 fragColor;
        void main() {
            float d = length(gl_PointCoord - vec2(0.5)) * 2.0;
            // smoothstep 替代 pow+discard，边缘自然过渡到全透明
            float fade = 1.0 - smoothstep(0.0, 1.0, d);
            // 二次衰减令中心更亮、边缘更柔和
            fade *= fade;
            fragColor = vec4(u_color.rgb, u_color.a * v_alpha * fade);
        }
    """.trimIndent()

    // ───────── 常量 ─────────

    companion object {
        // 水花 GL_POINTS 大小
        private const val SPLASH_PT_BASE  = 20f   // 最小点半径（px）
        private const val SPLASH_PT_RANGE = 18f   // 能量增量范围

        // 每个 vertex 3 个 float: (x, y, alpha)
        private const val FLOATS_PER_VERT = 3
    }

    // ───────── GL 对象 ─────────

    private var dropProgram   = 0
    private var splashProgram = 0
    private var vboId         = 0

    private var initialized = false

    // 缓存 uniform 位置，避免每帧 glGetUniformLocation 查询
    private var dropResolutionLoc = -1
    private var dropColorLoc      = -1
    private var splashResolutionLoc = -1
    private var splashColorLoc      = -1
    private var splashPtBaseLoc     = -1
    private var splashPtRangeLoc    = -1

    // VBO 数据缓冲（复用，避免每帧 GC）
    private var floatBuffer: FloatBuffer? = null
    private var bufferCapacity = 0   // 当前已分配的 float 数量

    // ───────── 初始化 ─────────

    fun init() {
        dropProgram   = buildProgram(DROP_VERT,   DROP_FRAG)
        splashProgram = buildProgram(SPLASH_VERT, SPLASH_FRAG)

        // 缓存所有 uniform 位置
        dropResolutionLoc   = GLES30.glGetUniformLocation(dropProgram,   "u_resolution")
        dropColorLoc        = GLES30.glGetUniformLocation(dropProgram,   "u_color")
        splashResolutionLoc = GLES30.glGetUniformLocation(splashProgram, "u_resolution")
        splashColorLoc      = GLES30.glGetUniformLocation(splashProgram, "u_color")
        splashPtBaseLoc     = GLES30.glGetUniformLocation(splashProgram, "u_pt_base")
        splashPtRangeLoc    = GLES30.glGetUniformLocation(splashProgram, "u_pt_range")

        val ids = IntArray(1)
        GLES30.glGenBuffers(1, ids, 0)
        vboId = ids[0]

        initialized = true
    }

    // ───────── 每帧绘制 ─────────

    fun draw(simulation: RainSimulation, screenWidth: Int, screenHeight: Int,
             bgStreaks: BackgroundRainStreaks? = null) {
        if (!initialized || !simulation.initOK) return

        val count    = simulation.particleCount
        if (count == 0 && bgStreaks == null) return
        val positions  = simulation.positionBuffer
        val velocities = simulation.velocityBuffer
        val prop       = simulation.proportion
        val infos      = simulation.groupInfos
        val infoCount  = simulation.groupInfoCount

        // ── 仅统计散开水花（聚合组不再渲染，由背景雨线替代） ──
        var splashCount = 0
        for (idx in 0 until infoCount) {
            val info = infos[idx]
            if (!info.cohesive) {
                splashCount += info.particleCount
            }
        }

        // VBO 空间：背景雨线 + 水花
        val bgFloats = if (bgStreaks != null) BackgroundRainStreaks.TOTAL_FLOATS else 0
        val totalFloats = bgFloats + splashCount * FLOATS_PER_VERT
        if (totalFloats == 0) return
        ensureBuffer(totalFloats)
        val buf = floatBuffer!!
        buf.clear()

        // ── 填充背景雨线顶点（最先写入 → 最先绘制 → 视觉最底层） ──
        val bgVertCount = if (bgStreaks != null) {
            val posBefore = buf.position()
            bgStreaks.fillVertices(buf)
            (buf.position() - posBefore) / FLOATS_PER_VERT
        } else 0

        // ── 填充散开组：每个粒子渲染为水花 GL_POINT ──
        for (idx in 0 until infoCount) {
            val info = infos[idx]
            if (info.cohesive) continue
            val start = info.bufferStart
            val end   = start + info.particleCount
            for (i in start until end) {
                val px     = positions[i].x * prop
                val py     = positions[i].y * prop
                val vx     = velocities[i].x
                val vyVal  = velocities[i].y
                val spd    = sqrt(vx * vx + vyVal * vyVal)
                val energy = (spd * prop * 0.00022f).coerceIn(0.25f, 1.0f)

                buf.put(px); buf.put(py); buf.put(energy)
            }
        }

        buf.flip()

        // 上传 VBO
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            buf.limit() * 4,
            buf,
            GLES30.GL_STREAM_DRAW
        )

        val stride = FLOATS_PER_VERT * 4

        // ── Pass 1：背景雨线（普通混合，GL_TRIANGLES） ──
        GLES30.glUseProgram(dropProgram)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        GLES30.glEnableVertexAttribArray(0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glVertexAttribPointer(1, 1, GLES30.GL_FLOAT, false, stride, 8)

        setResolution(dropResolutionLoc, screenWidth.toFloat(), screenHeight.toFloat())

        if (bgVertCount > 0) {
            setColor(dropColorLoc, 0.75f, 0.85f, 0.98f, 1.0f)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, bgVertCount)
        }

        // ── Pass 2：水花（加法混合，GL_POINTS）──
        GLES30.glUseProgram(splashProgram)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)   // additive

        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glVertexAttribPointer(1, 1, GLES30.GL_FLOAT, false, stride, 8)

        setResolution(splashResolutionLoc, screenWidth.toFloat(), screenHeight.toFloat())
        setColor(splashColorLoc, 0.82f, 0.93f, 1.0f, 0.90f)

        GLES30.glUniform1f(splashPtBaseLoc,  SPLASH_PT_BASE)
        GLES30.glUniform1f(splashPtRangeLoc, SPLASH_PT_RANGE)

        if (splashCount > 0) {
            val splashOffset = bgVertCount
            GLES30.glDrawArrays(GLES30.GL_POINTS, splashOffset, splashCount)
        }

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glDisable(GLES30.GL_BLEND)
    }

    // ───────── 资源释放 ─────────

    fun release() {
        if (vboId != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(vboId), 0)
            vboId = 0
        }
        if (dropProgram != 0)   { GLES30.glDeleteProgram(dropProgram);   dropProgram = 0 }
        if (splashProgram != 0) { GLES30.glDeleteProgram(splashProgram); splashProgram = 0 }
        initialized = false
    }

    // ───────── 私有工具 ─────────

    private fun ensureBuffer(floatCount: Int) {
        if (floatCount > bufferCapacity) {
            val capacity = (floatCount * 1.5f).toInt().coerceAtLeast(4096)
            floatBuffer = ByteBuffer
                .allocateDirect(capacity * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            bufferCapacity = capacity
        }
    }

    private fun setResolution(loc: Int, w: Float, h: Float) {
        GLES30.glUniform2f(loc, w, h)
    }

    private fun setColor(loc: Int, r: Float, g: Float, b: Float, a: Float) {
        GLES30.glUniform4f(loc, r, g, b, a)
    }

    private fun buildProgram(vertSrc: String, fragSrc: String): Int {
        val vs = compileShader(GLES30.GL_VERTEX_SHADER,   vertSrc)
        val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragSrc)
        return GLES30.glCreateProgram().also { prog ->
            GLES30.glAttachShader(prog, vs)
            GLES30.glAttachShader(prog, fs)
            GLES30.glLinkProgram(prog)
            GLES30.glDeleteShader(vs)
            GLES30.glDeleteShader(fs)
        }
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, src)
        GLES30.glCompileShader(shader)
        return shader
    }
}
