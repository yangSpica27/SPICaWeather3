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

    // 水花片元着色器：径向渐变柔和圆——无硬边缘，消除颗粒感
    private val SPLASH_FRAG = """
        #version 300 es
        precision mediump float;
        in float v_alpha;
        uniform vec4 u_color;
        out vec4 fragColor;
        void main() {
            float d = length(gl_PointCoord - vec2(0.5)) * 2.0;
            if (d > 1.0) discard;
            // 幂次越大，中心越亮、边缘衰减越快 → 更像水滴光晕
            float fade = pow(1.0 - d, 1.8);
            fragColor = vec4(u_color.rgb, u_color.a * v_alpha * fade);
        }
    """.trimIndent()

    // ───────── 常量 ─────────

    companion object {
        // 下落雨滴拖尾参数
        private const val FALL_TRAIL_FACTOR  = 0.030f   // 速度→拖尾长度系数（越大拖尾越长）
        private const val FALL_HALF_WIDTH    = 2.8f     // 拖尾半宽（像素）
        private const val FALL_MAX_TRAIL     = 200f     // 拖尾最大长度（像素）
        private const val FALL_ALPHA         = 0.70f    // 下落雨滴基础不透明度

        // 水花检测阈值
        private const val SPLASH_VY              = -0.5f   // 向上速度阈值（world 单位/s），检测弹起粒子
        private const val SPLASH_NEAR_GROUND_RATIO = 0.88f // 靠近地面比例阈值

        // 水花 GL_POINTS 大小
        private const val SPLASH_PT_BASE  = 10f   // 最小点半径（px）
        private const val SPLASH_PT_RANGE = 18f   // 能量增量范围

        // 每个 vertex 3 个 float: (x, y, alpha)
        private const val FLOATS_PER_VERT = 3
    }

    // ───────── GL 对象 ─────────

    private var dropProgram   = 0
    private var splashProgram = 0
    private var vboId         = 0

    private var initialized = false

    // VBO 数据缓冲（复用，避免每帧 GC）
    private var floatBuffer: FloatBuffer? = null
    private var bufferCapacity = 0   // 当前已分配的 float 数量

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
        val positions = simulation.positionBuffer
        val velocities = simulation.velocityBuffer
        val prop     = simulation.proportion

        // 分类统计：下落 vs 水花
        var fallingCount = 0
        var splashCount  = 0

        for (i in 0 until count) {
            val vy = velocities[i].y
            val xScreen = positions[i].x * prop
            val yScreen = positions[i].y * prop
            val speed = velocityLen(velocities[i])
            val isSplash = isSplashParticle(vy, xScreen, yScreen, speed, screenHeight, collisionRect)
            if (isSplash) splashCount++ else fallingCount++
        }

        // 申请 VBO 数据空间：下落 = 3 verts × 3 floats，水花 = 1 vert × 3 floats
        val totalFloats = fallingCount * 3 * FLOATS_PER_VERT + splashCount * FLOATS_PER_VERT
        ensureBuffer(totalFloats)
        val buf = floatBuffer!!
        buf.clear()

        // 填充下落雨滴顶点（GL_TRIANGLES 拖尾）
        for (i in 0 until count) {
            val vy = velocities[i].y
            val xScreen = positions[i].x * prop
            val yScreen = positions[i].y * prop
            val speed = velocityLen(velocities[i])
            val isSplash = isSplashParticle(vy, xScreen, yScreen, speed, screenHeight, collisionRect)
            if (isSplash) continue

            val px = xScreen
            val py = positions[i].y * prop
            val vx = velocities[i].x
            val vyVal = velocities[i].y

            val len = velocityLen(velocities[i])
            val trail = (len * prop * FALL_TRAIL_FACTOR).coerceAtMost(FALL_MAX_TRAIL)
            val normX = if (len > 0.001f) vx / len else 0f
            val normY = if (len > 0.001f) vyVal / len else 1f
            // 拖尾方向的垂直向量（法线），用于构造矩形两顶点
            val perpX = -normY * FALL_HALF_WIDTH
            val perpY =  normX * FALL_HALF_WIDTH
            // 拖尾起点（尾部）
            val tx = px - normX * trail
            val ty = py - normY * trail

            // 三角形 strip → 2 个三角形组成矩形，这里只用 1 个三角形（线条足够细）
            // Triangle: tip(px,py), left(tx-perp), right(tx+perp)
            buf.put(px);          buf.put(py);          buf.put(FALL_ALPHA)
            buf.put(tx - perpX);  buf.put(ty - perpY);  buf.put(FALL_ALPHA * 0.1f)
            buf.put(tx + perpX);  buf.put(ty + perpY);  buf.put(FALL_ALPHA * 0.1f)
        }

        // 填充水花顶点（GL_POINTS）
        for (i in 0 until count) {
            val vy = velocities[i].y
            val xScreen = positions[i].x * prop
            val yScreen = positions[i].y * prop
            val speed = velocityLen(velocities[i])
            val isSplash = isSplashParticle(vy, xScreen, yScreen, speed, screenHeight, collisionRect)
            if (!isSplash) continue

            val px = xScreen
            val py = positions[i].y * prop
            val energy = (speed * prop * 0.00022f).coerceIn(0.25f, 1.0f)

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
        setColor(dropProgram, 0.78f, 0.88f, 1.0f, 0.85f)

        if (fallingCount > 0) {
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, fallingCount * 3)
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
            val splashOffset = fallingCount * 3
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
