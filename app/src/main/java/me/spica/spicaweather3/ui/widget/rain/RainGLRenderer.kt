package me.spica.spicaweather3.ui.widget.rain

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.sqrt

/**
 * OpenGL ES 3.0 雨滴渲染器
 *
 * 多 pass 渲染策略：
 *  - screenRainProgram : 全屏程序化雨幕，负责多层软边雨丝
 *  - dropProgram       : 背景雨线 → GL_TRIANGLES
 *  - frontDropProgram  : 前景雨滴 → GL_POINTS + 软边拉伸雨滴
 *  - splashProgram     : 溅落水花 → GL_POINTS + 径向渐变 fragment shader
 */
class RainGLRenderer {

    // ───────── GLSL 着色器源码 ─────────

    private val SCREEN_RAIN_VERT = """
        #version 300 es
        out vec2 v_uv;
        const vec2 POSITIONS[6] = vec2[](
            vec2(-1.0, -1.0),
            vec2( 1.0, -1.0),
            vec2(-1.0,  1.0),
            vec2(-1.0,  1.0),
            vec2( 1.0, -1.0),
            vec2( 1.0,  1.0)
        );
        void main() {
            vec2 position = POSITIONS[gl_VertexID];
            gl_Position = vec4(position, 0.0, 1.0);
            v_uv = position * 0.5 + 0.5;
        }
    """.trimIndent()

    private val SCREEN_RAIN_FRAG = """
        #version 300 es
        precision mediump float;
        in vec2 v_uv;
        uniform vec2 u_resolution;
        uniform float u_time;
        out vec4 fragColor;

        float hash11(float p) {
            p = fract(p * 0.1031);
            p *= p + 33.33;
            p *= p + p;
            return fract(p);
        }

        vec3 hash31(float p) {
            vec3 p3 = fract(vec3(p) * vec3(0.1031, 0.11369, 0.13787));
            p3 += dot(p3, p3.yzx + 19.19);
            return fract(vec3(
                (p3.x + p3.y) * p3.z,
                (p3.x + p3.z) * p3.y,
                (p3.y + p3.z) * p3.x
            ));
        }

        float dropLayer(
            vec2 uv,
            float scale,
            float alpha,
            float seed,
            float laneScale,
            float speedBase,
            float trailLength
        ) {
            uv *= scale;
            uv = (uv - 0.5) * vec2(u_resolution.x / max(u_resolution.y, 1.0), 1.0);
            uv.x *= laneScale;
            float lane = floor(uv.x);
            float dx = fract(uv.x);
            vec3 rnd = hash31(lane + seed * 31.0 + hash11(lane + seed));
            float offset = (rnd.x + rnd.y - 1.0) * 2.1;
            float speed = mix(speedBase * 0.8, speedBase * 1.15, rnd.z);
            float yv = fract(uv.y + u_time * speed + offset) * trailLength;
            yv = 1.0 / max(yv, 0.08);
            yv = smoothstep(0.0, 1.0, yv * yv);
            float alphaGradient = clamp(yv + 0.45, 0.0, 1.0);
            yv = sin(yv * 3.1415926) * (speed * 3.4);
            float head = sin(dx * 3.1415926);
            yv *= head * head;
            return smoothstep(0.0, 1.0, yv) * alphaGradient * alpha;
        }

        void main() {
            vec2 uv = v_uv;
            float rain = 0.0;
            rain += dropLayer(uv, 16.0, 0.16, 1.0,  7.5, 0.52, 82.0);
            rain += dropLayer(uv, 24.0, 0.13, 2.0, 11.0, 0.70, 90.0);
            rain += dropLayer(uv, 34.0, 0.10, 3.0, 15.0, 0.92, 98.0);
            float bottomFade = mix(0.55, 1.0, smoothstep(0.0, 0.8, uv.y));
            rain *= bottomFade;
            vec3 baseColor = mix(
                vec3(0.70, 0.81, 0.95),
                vec3(0.90, 0.97, 1.0),
                smoothstep(0.0, 1.0, uv.y)
            );
            fragColor = vec4(baseColor, clamp(rain, 0.0, 0.72));
        }
    """.trimIndent()

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

    // 前景雨滴顶点着色器：根据能量缩放点大小
    private val FRONT_DROP_VERT = """
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

    // 前景雨滴片元着色器：纵向拉伸 + 圆头，避免硬边方块感
    private val FRONT_DROP_FRAG = """
        #version 300 es
        precision mediump float;
        in float v_alpha;
        uniform vec4 u_color;
        out vec4 fragColor;
        void main() {
            vec2 p = gl_PointCoord * 2.0 - 1.0;
            float body = 1.0 - smoothstep(0.22, 1.0, length(vec2(p.x * 3.8, p.y * 0.85)));
            float tail = 1.0 - smoothstep(-0.15, 1.0, p.y);
            float head = 1.0 - smoothstep(0.0, 1.0, length(vec2(p.x * 2.6, (p.y + 0.72) * 2.2)));
            float fade = body * (0.35 + 0.65 * tail);
            fade = max(fade, head * 0.9);
            fade *= fade;
            fragColor = vec4(u_color.rgb, u_color.a * v_alpha * fade);
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
            float fade = 1.0 - smoothstep(0.0, 1.0, d);
            fade *= fade;
            fragColor = vec4(u_color.rgb, u_color.a * v_alpha * fade);
        }
    """.trimIndent()

    // ───────── 常量 ─────────

    companion object {
        private const val FRONT_DROP_PT_BASE  = 9f
        private const val FRONT_DROP_PT_RANGE = 18f

        // 水花 GL_POINTS 大小
        private const val SPLASH_PT_BASE  = 20f   // 最小点半径（px）
        private const val SPLASH_PT_RANGE = 18f   // 能量增量范围

        // 每个 vertex 3 个 float: (x, y, alpha)
        private const val FLOATS_PER_VERT = 3
    }

    // ───────── GL 对象 ─────────

    private var screenRainProgram = 0
    private var dropProgram   = 0
    private var frontDropProgram = 0
    private var splashProgram = 0
    private var vboId         = 0

    private var initialized = false

    // 缓存 uniform 位置，避免每帧 glGetUniformLocation 查询
    private var screenRainResolutionLoc = -1
    private var screenRainTimeLoc       = -1
    private var dropResolutionLoc = -1
    private var dropColorLoc      = -1
    private var frontDropResolutionLoc = -1
    private var frontDropColorLoc      = -1
    private var frontDropPtBaseLoc     = -1
    private var frontDropPtRangeLoc    = -1
    private var splashResolutionLoc = -1
    private var splashColorLoc      = -1
    private var splashPtBaseLoc     = -1
    private var splashPtRangeLoc    = -1

    // VBO 数据缓冲（复用，避免每帧 GC）
    private var floatBuffer: FloatBuffer? = null
    private var bufferCapacity = 0
    private var startTimeNs = 0L

    // ───────── 初始化 ─────────

    fun init() {
        screenRainProgram = buildProgram(SCREEN_RAIN_VERT, SCREEN_RAIN_FRAG)
        dropProgram   = buildProgram(DROP_VERT,   DROP_FRAG)
        frontDropProgram = buildProgram(FRONT_DROP_VERT, FRONT_DROP_FRAG)
        splashProgram = buildProgram(SPLASH_VERT, SPLASH_FRAG)

        // 缓存所有 uniform 位置
        screenRainResolutionLoc = GLES30.glGetUniformLocation(screenRainProgram, "u_resolution")
        screenRainTimeLoc       = GLES30.glGetUniformLocation(screenRainProgram, "u_time")
        dropResolutionLoc   = GLES30.glGetUniformLocation(dropProgram,   "u_resolution")
        dropColorLoc        = GLES30.glGetUniformLocation(dropProgram,   "u_color")
        frontDropResolutionLoc = GLES30.glGetUniformLocation(frontDropProgram, "u_resolution")
        frontDropColorLoc      = GLES30.glGetUniformLocation(frontDropProgram, "u_color")
        frontDropPtBaseLoc     = GLES30.glGetUniformLocation(frontDropProgram, "u_pt_base")
        frontDropPtRangeLoc    = GLES30.glGetUniformLocation(frontDropProgram, "u_pt_range")
        splashResolutionLoc = GLES30.glGetUniformLocation(splashProgram, "u_resolution")
        splashColorLoc      = GLES30.glGetUniformLocation(splashProgram, "u_color")
        splashPtBaseLoc     = GLES30.glGetUniformLocation(splashProgram, "u_pt_base")
        splashPtRangeLoc    = GLES30.glGetUniformLocation(splashProgram, "u_pt_range")

        val ids = IntArray(1)
        GLES30.glGenBuffers(1, ids, 0)
        vboId = ids[0]

        startTimeNs = System.nanoTime()
        initialized = true
    }

    // ───────── 每帧绘制 ─────────

    fun draw(simulation: RainSimulation, screenWidth: Int, screenHeight: Int,
             bgStreaks: BackgroundRainStreaks? = null) {
        if (!initialized || !simulation.initOK) return

        val positions  = simulation.positionBuffer
        val velocities = simulation.velocityBuffer
        val prop       = simulation.proportion
        val infos      = simulation.groupInfos
        val infoCount  = simulation.groupInfoCount

        var frontDropCount = 0
        var splashCount = 0
        for (idx in 0 until infoCount) {
            val info = infos[idx]
            if (info.cohesive) {
                frontDropCount += info.particleCount
            } else {
                splashCount += info.particleCount
            }
        }

        // VBO 空间：背景雨线 + 前景雨滴 + 水花
        val bgFloats = if (bgStreaks != null) BackgroundRainStreaks.TOTAL_FLOATS else 0
        val totalFloats = bgFloats + (frontDropCount + splashCount) * FLOATS_PER_VERT
        var bgVertCount = 0
        var frontDropOffset = 0
        var splashOffset = 0
        if (totalFloats > 0) {
            ensureBuffer(totalFloats)
            val buf = floatBuffer!!
            buf.clear()

            // ── 填充背景雨线顶点（最先写入 → 最先绘制 → 视觉最底层） ──
            bgVertCount = if (bgStreaks != null) {
                val posBefore = buf.position()
                bgStreaks.fillVertices(buf)
                (buf.position() - posBefore) / FLOATS_PER_VERT
            } else 0
            frontDropOffset = bgVertCount

            // ── 填充聚合组：每个粒子渲染为前景软雨滴 ──
            for (idx in 0 until infoCount) {
                val info = infos[idx]
                if (!info.cohesive) continue
                val start = info.bufferStart
                val end   = start + info.particleCount
                for (i in start until end) {
                    val px     = positions[i].x * prop
                    val py     = positions[i].y * prop
                    val vx     = velocities[i].x
                    val vyVal  = velocities[i].y
                    val spd    = sqrt(vx * vx + vyVal * vyVal)
                    val energy = (spd * prop * 0.00014f).coerceIn(0.18f, 1.0f)

                    buf.put(px); buf.put(py); buf.put(energy)
                }
            }

            splashOffset = bgVertCount + frontDropCount

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
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
            GLES30.glBufferData(
                GLES30.GL_ARRAY_BUFFER,
                buf.limit() * 4,
                buf,
                GLES30.GL_STREAM_DRAW
            )
        }

        val stride = FLOATS_PER_VERT * 4

        GLES30.glEnable(GLES30.GL_BLEND)

        // ── Pass 1：全屏程序化雨幕（加法混合，软边多层） ──
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glUseProgram(screenRainProgram)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)
        setResolution(screenRainResolutionLoc, screenWidth.toFloat(), screenHeight.toFloat())
        GLES30.glUniform1f(screenRainTimeLoc, (System.nanoTime() - startTimeNs) / 1_000_000_000f)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6)

        if (totalFloats <= 0) {
            GLES30.glDisable(GLES30.GL_BLEND)
            return
        }

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glEnableVertexAttribArray(1)

        // ── Pass 2：背景雨线（普通混合，GL_TRIANGLES） ──
        if (bgVertCount > 0) {
            GLES30.glUseProgram(dropProgram)
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
            GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, stride, 0)
            GLES30.glVertexAttribPointer(1, 1, GLES30.GL_FLOAT, false, stride, 8)

            setResolution(dropResolutionLoc, screenWidth.toFloat(), screenHeight.toFloat())
            setColor(dropColorLoc, 0.75f, 0.85f, 0.98f, 1.0f)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, bgVertCount)
        }

        // ── Pass 3：前景软雨滴（加法混合，GL_POINTS）──
        if (frontDropCount > 0) {
            GLES30.glUseProgram(frontDropProgram)
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)
            GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, stride, 0)
            GLES30.glVertexAttribPointer(1, 1, GLES30.GL_FLOAT, false, stride, 8)

            setResolution(frontDropResolutionLoc, screenWidth.toFloat(), screenHeight.toFloat())
            setColor(frontDropColorLoc, 0.88f, 0.96f, 1.0f, 0.72f)
            GLES30.glUniform1f(frontDropPtBaseLoc,  FRONT_DROP_PT_BASE)
            GLES30.glUniform1f(frontDropPtRangeLoc, FRONT_DROP_PT_RANGE)
            GLES30.glDrawArrays(GLES30.GL_POINTS, frontDropOffset, frontDropCount)
        }

        // ── Pass 4：水花（加法混合，GL_POINTS）──
        if (splashCount > 0) {
            GLES30.glUseProgram(splashProgram)
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)
            GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, stride, 0)
            GLES30.glVertexAttribPointer(1, 1, GLES30.GL_FLOAT, false, stride, 8)

            setResolution(splashResolutionLoc, screenWidth.toFloat(), screenHeight.toFloat())
            setColor(splashColorLoc, 0.82f, 0.93f, 1.0f, 0.90f)
            GLES30.glUniform1f(splashPtBaseLoc,  SPLASH_PT_BASE)
            GLES30.glUniform1f(splashPtRangeLoc, SPLASH_PT_RANGE)
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
        if (screenRainProgram != 0) {
            GLES30.glDeleteProgram(screenRainProgram)
            screenRainProgram = 0
        }
        if (dropProgram != 0)   { GLES30.glDeleteProgram(dropProgram);   dropProgram = 0 }
        if (frontDropProgram != 0) {
            GLES30.glDeleteProgram(frontDropProgram)
            frontDropProgram = 0
        }
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
            val linkStatus = IntArray(1)
            GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val infoLog = GLES30.glGetProgramInfoLog(prog)
                GLES30.glDeleteProgram(prog)
                GLES30.glDeleteShader(vs)
                GLES30.glDeleteShader(fs)
                error("Failed to link rain program: $infoLog")
            }
            GLES30.glDeleteShader(vs)
            GLES30.glDeleteShader(fs)
        }
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, src)
        GLES30.glCompileShader(shader)
        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val infoLog = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            error("Failed to compile rain shader: $infoLog")
        }
        return shader
    }
}
