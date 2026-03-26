package me.spica.spicaweather3.ui.widget.rain

import java.nio.FloatBuffer
import kotlin.random.Random

/**
 * 纯装饰背景雨线——不参与物理碰撞，简单线性下落
 *
 * 多层视差：近景（快、粗、亮）→ 远景（慢、细、淡），营造雨幕纵深感。
 * 每帧调用 [update] 推进位置，调用 [fillVertices] 写入 GL 顶点数据，
 * 与物理雨滴共用 dropProgram（GL_TRIANGLES，顶点格式 x,y,alpha）。
 */
class BackgroundRainStreaks(
    private val screenWidth: Int,
    private val screenHeight: Int
) {

    companion object {
        /** 背景雨线总数 */
        private const val STREAK_COUNT = 100

        /** 每条雨线 6 个顶点（2 个三角形） */
        private const val VERTS_PER_STREAK = 6

        /** 每个顶点 3 个 float (x, y, alpha) */
        private const val FLOATS_PER_VERT = 3

        /** 总 float 数量（固定） */
        const val TOTAL_FLOATS = STREAK_COUNT * VERTS_PER_STREAK * FLOATS_PER_VERT

        // 层级参数：(speedMin, speedMax, halfWidth, length, alphaMax)
        // 远景层
        private val LAYER_FAR   = Layer(400f,  700f,  2.8f, 30f..60f,   0.5f)
        // 中景层
        private val LAYER_MID   = Layer(700f,  1100f, 4.0f, 45f..90f,   0.7f)
        // 近景层
        private val LAYER_NEAR  = Layer(1100f, 1600f, 5.5f, 60f..120f,  0.9f)

        private val LAYERS = arrayOf(LAYER_FAR, LAYER_MID, LAYER_NEAR)
    }

    private data class Layer(
        val speedMin: Float,
        val speedMax: Float,
        val halfWidth: Float,
        val lengthRange: ClosedFloatingPointRange<Float>,
        val alphaMax: Float
    )

    // 每条雨线的状态
    private val x       = FloatArray(STREAK_COUNT)
    private val y       = FloatArray(STREAK_COUNT)
    private val speed   = FloatArray(STREAK_COUNT)  // px/s
    private val length  = FloatArray(STREAK_COUNT)
    private val hw      = FloatArray(STREAK_COUNT)  // halfWidth
    private val alpha   = FloatArray(STREAK_COUNT)

    init {
        for (i in 0 until STREAK_COUNT) {
            resetStreak(i, randomizeY = true)
        }
    }

    /** 推进一帧（deltaTime 单位：秒） */
    fun update(dt: Float) {
        val extendedH = screenHeight * 1.3f
        for (i in 0 until STREAK_COUNT) {
            y[i] += speed[i] * dt
            if (y[i] - length[i] > extendedH) {
                resetStreak(i, randomizeY = false)
            }
        }
    }

    /**
     * 将所有雨线顶点写入 [buf]（不调用 flip）。
     * 写入 [TOTAL_FLOATS] 个 float，与 dropProgram 的 (x, y, alpha) 格式兼容。
     *
     * 头部 alpha 按屏幕 Y 位置线性渐变：底部 → 0.45，顶部 → 0.0
     */
    fun fillVertices(buf: FloatBuffer) {
        val hInv = 1f / screenHeight.coerceAtLeast(1)
        for (i in 0 until STREAK_COUNT) {
            val px = x[i]
            val py = y[i]
            val halfW = hw[i]
            val len = length[i]

            // 头部（下端）
            val hx = px
            val hy = py
            // 尾部（上端，垂直正上方）
            val tx = px
            val ty = py - len
            val tailHW = halfW * 0.2f

            // 按 Y 位置计算 alpha：底部 0.45，顶部 0.0
            val headA = ((hy * hInv).coerceIn(0f, 1f) * 0.45f) * alpha[i]
            val tailA = ((ty * hInv).coerceIn(0f, 1f) * 0.45f) * alpha[i]

            // 三角形 1：头左 → 头右 → 尾右
            buf.put(hx - halfW); buf.put(hy); buf.put(headA)
            buf.put(hx + halfW); buf.put(hy); buf.put(headA)
            buf.put(tx + tailHW); buf.put(ty); buf.put(tailA)

            // 三角形 2：头左 → 尾右 → 尾左
            buf.put(hx - halfW); buf.put(hy); buf.put(headA)
            buf.put(tx + tailHW); buf.put(ty); buf.put(tailA)
            buf.put(tx - tailHW); buf.put(ty); buf.put(tailA)
        }
    }

    private fun resetStreak(i: Int, randomizeY: Boolean) {
        val layer = LAYERS[i % LAYERS.size]
        x[i] = Random.nextFloat() * screenWidth
        y[i] = if (randomizeY) {
            Random.nextFloat() * screenHeight * 1.5f
        } else {
            -Random.nextFloat() * screenHeight * 0.3f
        }
        speed[i] = layer.speedMin + Random.nextFloat() * (layer.speedMax - layer.speedMin)
        val lr = layer.lengthRange
        length[i] = lr.start + Random.nextFloat() * (lr.endInclusive - lr.start)
        hw[i] = layer.halfWidth * (0.8f + Random.nextFloat() * 0.4f)
        alpha[i] = layer.alphaMax * (0.6f + Random.nextFloat() * 0.4f)
    }
}
