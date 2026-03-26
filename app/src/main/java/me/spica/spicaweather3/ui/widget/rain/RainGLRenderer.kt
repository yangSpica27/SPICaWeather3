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
        // 下落雨滴拖尾参数
        private const val FALL_TRAIL_FACTOR  = 0.045f   // 速度→拖尾长度系数（加长拖尾）
        private const val FALL_HALF_WIDTH    = 3.5f     // 头部半宽（像素）
        private const val FALL_MAX_TRAIL     = 220f     // 拖尾最大长度（像素）
        private const val FALL_ALPHA         = 0.35f    // 下落雨滴头部不透明度
        private const val TAIL_WIDTH_FRAC    = 0.30f    // 尾端宽度占头部宽度的比例（收窄）

        // 水花检测阈值
        private const val SPLASH_VY              = -0.5f   // 向上速度阈值（world 单位/s），检测弹起粒子
        private const val SPLASH_NEAR_GROUND_RATIO = 0.88f // 靠近地面比例阈值

        // 水花 GL_POINTS 大小
        private const val SPLASH_PT_BASE  = 20f   // 最小点半径（px）
        private const val SPLASH_PT_RANGE = 18f   // 能量增量范围

        // 每个 vertex 3 个 float: (x, y, alpha)
        private const val FLOATS_PER_VERT = 3
        // 每个下落雨滴 6 个顶点（锥形矩形 = 2 三角形）
        private const val VERTS_PER_DROP  = 6
    }

    // ───────── GL 对象 ─────────

    private var dropProgram   = 0
    private var splashProgram = 0
    private var vboId         = 0

    private var initialized = false

    // VBO 数据缓冲（复用，避免每帧 GC）
    private var floatBuffer: FloatBuffer? = null
    private var bufferCapacity = 0   // 当前已分配的 float 数量

    // 粒子分类缓冲（预分配，避免每帧分配）
    private var isSplashCache = BooleanArray(RainSimulation.MAX_PARTICLES)
    private var speedCache    = FloatArray(RainSimulation.MAX_PARTICLES)

    // ───────── 初始化 ─────────

    fun init() {
        dropProgram   = buildProgram(DROP_VERT,   DROP_FRAG)
        splashProgram = buildProgram(SPLASH_VERT, SPLASH_FRAG)

        val ids = IntArray(1)
        GLES30.glGenBuffers(1, ids, 0)
        vboId = ids[0]

        initialized = true
    }

    // ───────── 每帧绘制 ─────────

    fun draw(simulation: RainSimulation, screenWidth: Int, screenHeight: Int,
             collisionRect: FloatArray? = null) {
        if (!initialized || !simulation.initOK) return

        val count    = simulation.particleCount
        if (count == 0) return
        val positions  = simulation.positionBuffer
        val velocities = simulation.velocityBuffer
        val prop       = simulation.proportion

        // ── 单次遍历：分类 + 缓存速度，避免后续重复计算 ──
        var fallingCount = 0
        var splashCount  = 0
        for (i in 0 until count) {
            val spd = velocityLen(velocities[i])
            speedCache[i] = spd
            val xPx = positions[i].x * prop
            val yPx = positions[i].y * prop
            val splash = isSplashParticle(velocities[i].y, xPx, yPx, spd, screenHeight, collisionRect)
            isSplashCache[i] = splash
            if (splash) splashCount++ else fallingCount++
        }

        // VBO 空间：下落 = VERTS_PER_DROP 个顶点，水花 = 1 个顶点
        val totalFloats = fallingCount * VERTS_PER_DROP * FLOATS_PER_VERT + splashCount * FLOATS_PER_VERT
        if (totalFloats == 0) return
        ensureBuffer(totalFloats)
        val buf = floatBuffer!!
        buf.clear()

        // ── 填充下落雨滴顶点（锥形矩形：头宽满α → 尾窄零α，GPU 线性插值渐变） ──
        for (i in 0 until count) {
            if (isSplashCache[i]) continue

            val px    = positions[i].x * prop
            val py    = positions[i].y * prop
            val vx    = velocities[i].x
            val vyVal = velocities[i].y
            val spd   = speedCache[i]

            val trail = (spd * prop * FALL_TRAIL_FACTOR).coerceAtMost(FALL_MAX_TRAIL)
            val normX = if (spd > 0.001f) vx / spd else 0f
            val normY = if (spd > 0.001f) vyVal / spd else 1f
            val perpX = -normY * FALL_HALF_WIDTH
            val perpY =  normX * FALL_HALF_WIDTH

            // 头部（前端，满宽）
            val hx = px; val hy = py
            // 尾部（后端，收窄）
            val tx = px - normX * trail
            val ty = py - normY * trail
            val tailPerpX = perpX * TAIL_WIDTH_FRAC
            val tailPerpY = perpY * TAIL_WIDTH_FRAC

            val headA = FALL_ALPHA
            val tailA = 0f

            // 三角形 1：头左 → 头右 → 尾右
            buf.put(hx - perpX);     buf.put(hy - perpY);     buf.put(headA)
            buf.put(hx + perpX);     buf.put(hy + perpY);     buf.put(headA)
            buf.put(tx + tailPerpX); buf.put(ty + tailPerpY); buf.put(tailA)

            // 三角形 2：头左 → 尾右 → 尾左
            buf.put(hx - perpX);     buf.put(hy - perpY);     buf.put(headA)
            buf.put(tx + tailPerpX); buf.put(ty + tailPerpY); buf.put(tailA)
            buf.put(tx - tailPerpX); buf.put(ty - tailPerpY); buf.put(tailA)
        }

        // ── 填充水花顶点（GL_POINTS） ──
        for (i in 0 until count) {
            if (!isSplashCache[i]) continue

            val px     = positions[i].x * prop
            val py     = positions[i].y * prop
            val energy = (speedCache[i] * prop * 0.00022f).coerceIn(0.25f, 1.0f)

            buf.put(px); buf.put(py); buf.put(energy)
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

        // ── Pass 1：下落雨滴（普通混合，GL_TRIANGLES）──
        GLES30.glUseProgram(dropProgram)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        GLES30.glEnableVertexAttribArray(0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glVertexAttribPointer(1, 1, GLES30.GL_FLOAT, false, stride, 8)

        setResolution(dropProgram, screenWidth.toFloat(), screenHeight.toFloat())
        // u_color.a 设为 1.0，让顶点 alpha (FALL_ALPHA) 成为唯一不透明度控制
        setColor(dropProgram, 0.78f, 0.88f, 1.0f, 1.0f)

        if (fallingCount > 0) {
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, fallingCount * VERTS_PER_DROP)
        }

        // ── Pass 2：水花（加法混合，GL_POINTS）──
        GLES30.glUseProgram(splashProgram)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)   // additive

        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glVertexAttribPointer(1, 1, GLES30.GL_FLOAT, false, stride, 8)

        setResolution(splashProgram, screenWidth.toFloat(), screenHeight.toFloat())
        setColor(splashProgram, 0.82f, 0.93f, 1.0f, 0.90f)

        val ptBaseLoc  = GLES30.glGetUniformLocation(splashProgram, "u_pt_base")
        val ptRangeLoc = GLES30.glGetUniformLocation(splashProgram, "u_pt_range")
        GLES30.glUniform1f(ptBaseLoc,  SPLASH_PT_BASE)
        GLES30.glUniform1f(ptRangeLoc, SPLASH_PT_RANGE)

        if (splashCount > 0) {
            val splashOffset = fallingCount * VERTS_PER_DROP
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

    private fun velocityLen(v: org.jbox2d.common.Vec2) =
        sqrt(v.x * v.x + v.y * v.y)

    /**
     * 判断粒子是否为溅落水花：
     * - 速度向上（反弹）→ 水花
     * - 在碰撞矩形附近且速度较慢 → 水花
     */
    private fun isSplashParticle(
        vy: Float, xScreen: Float, yScreen: Float, speed: Float,
        screenHeight: Int, collisionRect: FloatArray?
    ): Boolean {
        if (vy < SPLASH_VY) return true
        return if (collisionRect != null) {
            val margin = 30f
            xScreen in (collisionRect[0] - margin)..(collisionRect[2] + margin) &&
                yScreen in (collisionRect[1] - margin)..(collisionRect[3] + margin) &&
                speed in 0.5f..12f
        } else {
            yScreen > screenHeight * SPLASH_NEAR_GROUND_RATIO && speed in 0.5f..12f
        }
    }

    private fun setResolution(program: Int, w: Float, h: Float) {
        val loc = GLES30.glGetUniformLocation(program, "u_resolution")
        GLES30.glUniform2f(loc, w, h)
    }

    private fun setColor(program: Int, r: Float, g: Float, b: Float, a: Float) {
        val loc = GLES30.glGetUniformLocation(program, "u_color")
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
