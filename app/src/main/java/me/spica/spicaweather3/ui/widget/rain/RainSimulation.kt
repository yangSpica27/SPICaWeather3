package me.spica.spicaweather3.ui.widget.rain

import android.graphics.Bitmap
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Settings
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World
import org.jbox2d.particle.ParticleGroup
import org.jbox2d.particle.ParticleGroupDef
import org.jbox2d.particle.ParticleType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 粒子组渲染信息：供渲染器判断该组应整体渲染还是逐粒子渲染。
 * 使用可变字段 + 对象池，避免每帧分配新对象。
 */
class GroupRenderInfo {
    var cohesive: Boolean = false   // 粒子是否仍聚合为整体
    var bufferStart: Int = 0        // 粒子在全局缓冲中的起始索引
    var particleCount: Int = 0      // 该组的粒子数量
}

/**
 * JBox2D 物理雨滴模拟
 *
 * 完整保留原有物理碰撞逻辑：
 *  - 重力 Vec2(0, 15)，雨滴向下加速
 *  - 屏幕底部设置静态刚体（地板），粒子与之碰撞弹溅
 *  - 最多 MAX_GROUPS 个粒子组并发，超过生命周期后回收重生
 *  - 每帧固定步进 2 × 1/120s = 1/60s（与渲染帧率同步）
 *
 * OpenGL 渲染器通过 [positionBuffer]、[velocityBuffer]、[particleCount]
 * 直接读取 JBox2D 内部缓冲，在同一渲染线程内无需额外同步。
 *
 * @param width      绘制区域宽度（像素）
 * @param height     绘制区域高度（像素）
 * @param proportion 世界坐标 → 屏幕像素的缩放比例，默认 100
 */
class RainSimulation(
    val width: Int,
    val height: Int,
    val proportion: Float = 100f
) {
    companion object {
        /** JBox2D 允许的最大粒子数 */
        const val MAX_PARTICLES = 2000

        /** 同时活跃的最大粒子组数 */
        private const val MAX_GROUPS = 42

        /** 粒子组从生成到回收的总生命周期（毫秒） */
        private const val TOTAL_LIFETIME_MS = 4000L

        /** 每个圆角的弧线细分段数，4 个角共 SEGMENTS_PER_CORNER*4 个顶点 */
        private const val SEGMENTS_PER_CORNER = 4

        /** 聚合判定：粒子离质心最大距离（世界坐标），超出则视为散开 */
        private const val COHESIVE_SPREAD_SQ = 0.35f * 0.35f

        /** 聚合判定：平均竖直速度下限，低于此值说明已减速/碰撞 */
        private const val COHESIVE_MIN_VY = 5f

        /** 源项目最大雨档（bg type 8）迁移过来的主要参数 */
        private const val MAX_RAIN_GRAVITY_Y = 100.25f
        private const val MAX_RAIN_PRESSURE = 0.024f
        private const val MAX_RAIN_DAMPING = 1.0f
        private const val MAX_RAIN_BASE_VELOCITY = 40f
        private const val MAX_RAIN_VELOCITY_VARIANCE_RATIO = 0.15f
        private const val TEXT_COLLISION_ALPHA_THRESHOLD = 32
        private const val TEXT_COLLISION_MIN_RUN_PX = 3

        init {
            // 提高 JBox2D 多边形顶点上限以支持更平滑的圆角碰撞体
            Settings.maxPolygonVertices = SEGMENTS_PER_CORNER * 4
        }
    }

    /** 初始化是否完成 */
    var initOK = false
        private set

    private lateinit var world: World

    // 当前活跃粒子组列表
    private val groups = ArrayList<ParticleGroup>()

    // 用于均匀水平分布的网格列索引
    private var nextGridIndex = 0

    // ────────── 碰撞矩形（替代原底部地板碰撞） ──────────

    /**
     * 碰撞矩形（像素坐标：left, top, right, bottom）
     * 由外部设置，在渲染线程 [update] 中同步到物理世界
     */
    @Volatile
    var pendingCollisionRect: FloatArray? = null

    @Volatile
    var pendingTextCollision: RainTextCollision? = null

    private var appliedCollisionRect: FloatArray? = null
    private var appliedTextCollisionSignature: Int? = null
    private var collisionBody: Body? = null

    // ────────── 供渲染器使用的访问器 ──────────

    /** 当前帧粒子数量（有效索引范围 [0, particleCount)） */
    val particleCount: Int get() = world.particleCount

    /**
     * JBox2D 内部粒子位置缓冲（世界坐标，Vec2 单位）
     * 渲染时需乘以 [proportion] 转换为屏幕像素坐标
     */
    val positionBuffer: Array<Vec2> get() = world.particlePositionBuffer

    /**
     * JBox2D 内部粒子速度缓冲（世界单位/秒）
     * 速度方向决定雨滴拖尾朝向，速度大小决定拖尾长度
     */
    val velocityBuffer: Array<Vec2> get() = world.particleVelocityBuffer

    // ────────── 粒子组渲染信息（预分配对象池，避免每帧 GC） ──────────

    private val _groupInfoPool = Array(MAX_GROUPS) { GroupRenderInfo() }
    private var _groupInfoCount = 0

    /** 每帧更新后的有效粒子组数量 */
    val groupInfoCount: Int get() = _groupInfoCount

    /** 预分配的粒子组渲染信息数组，有效范围 [0, groupInfoCount) */
    val groupInfos: Array<GroupRenderInfo> get() = _groupInfoPool

    // ────────── 初始化 ──────────

    /**
     * 初始化物理世界与粒子系统，必须在渲染线程启动前调用
     */
    fun init() {
        synchronized(this) {
            world = World(Vec2(0f, MAX_RAIN_GRAVITY_Y))
            world.setParticlePressureStrength(MAX_RAIN_PRESSURE)
            world.setParticleDamping(MAX_RAIN_DAMPING)
            world.particleRadius  = 6f / proportion
            world.particleMaxCount = MAX_PARTICLES

            // 同步碰撞体（如果外部已设置碰撞矩形或文本轮廓）
            syncCollisionBody()

            // 错时生成各粒子组，避免所有粒子同时出现
            for (i in 0 until MAX_GROUPS) {
                groups.add(createGroup(seedIndex = i))
            }
            initOK = true
        }
    }

    // ────────── 每帧更新 ──────────

    /**
     * 推进一帧物理模拟，并回收生命周期到期的粒子组
     *
     * 每帧步进 2 次 × 1/120s = 1/60s，与渲染帧率同步。
     * 在渲染线程内调用，无需在外部加锁。
     */
    fun update() {
        if (!initOK) return
        synchronized(this) {
            syncCollisionBody()
            val now = System.currentTimeMillis()

            // 索引遍历，避免 filter/indexOf 产生临时列表和 O(n) 查找
            for (i in groups.indices) {
                val g = groups[i]
                val birth = g.userData as? Long ?: continue
                if (now - birth > TOTAL_LIFETIME_MS) {
                    world.destroyParticlesInGroup(g)
                    groups[i] = createGroup()
                }
            }

            // 每帧步进两次（1/120s × 2 = 1/60s），与真实时间同步
            world.step(1f / 120f, 8, 3)
            world.step(1f / 120f, 8, 3)

            // 更新粒子组渲染信息，供渲染器判断整体/逐粒子渲染
            computeGroupInfos()
        }
    }

    // ────────── 碰撞矩形同步 ──────────

    /**
     * 将 [pendingCollisionRect] 同步到物理世界：
     * 销毁旧碰撞体，在新矩形位置创建静态刚体，替代原底部地板碰撞
     */
    private fun applyCollisionRect() {
        val rect = pendingCollisionRect ?: return
        if (rect.contentEquals(appliedCollisionRect)) return

        val left = rect[0] / proportion
        val top = rect[1] / proportion
        val right = rect[2] / proportion
        val bottom = rect[3] / proportion
        val cx = (left + right) / 2f
        val cy = (top + bottom) / 2f
        val hw = (right - left) / 2f
        val hh = (bottom - top) / 2f

        // 跳过退化矩形，避免 JBox2D PolygonShape 断言失败
        if (hw < Settings.linearSlop || hh < Settings.linearSlop) return

        collisionBody?.let {
            world.destroyBody(it)
            collisionBody = null
        }

        val bodyDef = BodyDef().apply {
            type = BodyType.STATIC
            position.set(cx, cy)
        }
        collisionBody = world.createBody(bodyDef)
        collisionBody!!.createFixture(FixtureDef().apply {
            val cr = (if (rect.size > 4) rect[4] / proportion else 0f)
                .coerceAtMost(minOf(hw, hh))
            shape = PolygonShape().also {
                // 圆角半径必须远小于半宽/半高，否则角心重叠导致顶点退化
                if (cr > 0.01f && hw - cr > Settings.linearSlop && hh - cr > Settings.linearSlop) {
                    // 每个圆角用 SEGMENTS_PER_CORNER 段弧线近似，共 N*4 个顶点（CCW）
                    val n = SEGMENTS_PER_CORNER
                    val verts = ArrayList<Vec2>(n * 4)
                    val halfPi = (Math.PI / 2.0).toFloat()
                    for (corner in 0 until 4) {
                        // 四个角的圆心偏移和起始角度
                        val (ocx, ocy, startAngle) = when (corner) {
                            0 -> Triple(hw - cr, -(hh - cr), -halfPi)    // 右下
                            1 -> Triple(hw - cr, hh - cr, 0f)            // 右上
                            2 -> Triple(-(hw - cr), hh - cr, halfPi)     // 左上
                            else -> Triple(-(hw - cr), -(hh - cr), Math.PI.toFloat()) // 左下
                        }
                        for (s in 0 until n) {
                            val angle = startAngle + halfPi * s / n
                            verts.add(Vec2(ocx + cr * cos(angle), ocy + cr * sin(angle)))
                        }
                    }
                    it.set(verts.toTypedArray(), verts.size)
                } else {
                    it.setAsBox(hw, hh)
                }
            }
            friction = 0.8f
            restitution = 0.5f
            filter.maskBits = 0b01
            filter.groupIndex = 0b01
        })

        appliedCollisionRect = rect.clone()
        appliedTextCollisionSignature = null
    }

    private fun syncCollisionBody() {
        when {
            pendingTextCollision != null -> applyTextCollision()
            pendingCollisionRect != null -> applyCollisionRect()
            else -> {
                destroyCollisionBody()
                appliedCollisionRect = null
                appliedTextCollisionSignature = null
            }
        }
    }

    private fun applyTextCollision() {
        val textCollision = pendingTextCollision ?: return
        val signature = buildTextCollisionSignature(textCollision)
        if (appliedTextCollisionSignature == signature) return

        destroyCollisionBody()

        val bitmap = textCollision.bitmap
        if (bitmap.width <= 0 || bitmap.height <= 0) return

        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val body = world.createBody(BodyDef().apply { type = BodyType.STATIC })
        var fixtureCount = 0
        val sampleStepPx = (bitmap.width / 42).coerceIn(4, 8)

        for (xStart in 0 until bitmap.width step sampleStepPx) {
            val xEnd = minOf(bitmap.width, xStart + sampleStepPx)
            var runStart = -1
            for (y in 0 until bitmap.height) {
                val opaque = isColumnOpaque(pixels, bitmap.width, xStart, xEnd, y)
                if (opaque) {
                    if (runStart < 0) runStart = y
                } else if (runStart >= 0) {
                    fixtureCount += addTextFixture(body, textCollision, xStart, xEnd, runStart, y)
                    runStart = -1
                }
            }
            if (runStart >= 0) {
                fixtureCount += addTextFixture(body, textCollision, xStart, xEnd, runStart, bitmap.height)
            }
        }

        if (fixtureCount == 0) {
            world.destroyBody(body)
            appliedTextCollisionSignature = null
            return
        }

        collisionBody = body
        appliedTextCollisionSignature = signature
        appliedCollisionRect = null
    }

    private fun addTextFixture(
        body: Body,
        textCollision: RainTextCollision,
        xStart: Int,
        xEnd: Int,
        yStart: Int,
        yEnd: Int,
    ): Int {
        val runHeight = yEnd - yStart
        if (runHeight < TEXT_COLLISION_MIN_RUN_PX) return 0

        val halfWidth = ((xEnd - xStart) * 0.5f) / proportion
        val halfHeight = (runHeight * 0.5f) / proportion
        if (halfWidth < Settings.linearSlop || halfHeight < Settings.linearSlop) return 0

        val centerX = (textCollision.left + xStart + ((xEnd - xStart) * 0.5f)) / proportion
        val centerY = (textCollision.top + yStart + (runHeight * 0.5f)) / proportion
        val shape = PolygonShape().apply {
            setAsBox(halfWidth, halfHeight, Vec2(centerX, centerY), 0f)
        }
        body.createFixture(FixtureDef().apply {
            this.shape = shape
            friction = 0.8f
            restitution = 0.15f
            filter.maskBits = 0b01
            filter.groupIndex = 0b01
        })
        return 1
    }

    private fun isColumnOpaque(
        pixels: IntArray,
        bitmapWidth: Int,
        xStart: Int,
        xEnd: Int,
        y: Int,
    ): Boolean {
        val rowOffset = y * bitmapWidth
        for (x in xStart until xEnd) {
            val alpha = pixels[rowOffset + x] ushr 24
            if (alpha >= TEXT_COLLISION_ALPHA_THRESHOLD) return true
        }
        return false
    }

    private fun buildTextCollisionSignature(textCollision: RainTextCollision): Int {
        val bitmap = textCollision.bitmap
        var result = bitmap.generationId
        result = 31 * result + bitmap.width
        result = 31 * result + bitmap.height
        result = 31 * result + textCollision.left.toBits()
        result = 31 * result + textCollision.top.toBits()
        return result
    }

    private fun destroyCollisionBody() {
        collisionBody?.let {
            world.destroyBody(it)
            collisionBody = null
        }
    }

    // ────────── 私有：粒子组渲染信息计算 ──────────

    /**
     * 遍历所有活跃粒子组，计算聚合状态并更新预分配的渲染信息池。
     * 聚合判定：所有粒子与质心的距离平方 < [COHESIVE_SPREAD_SQ] 且平均下落速度 > [COHESIVE_MIN_VY]
     */
    private fun computeGroupInfos() {
        val positions = world.particlePositionBuffer
        val velocities = world.particleVelocityBuffer
        var infoIdx = 0

        for (g in groups) {
            val start = g.bufferIndex
            val cnt = g.particleCount
            if (cnt == 0) continue

            // 先计算质心和平均竖直速度（仅用于聚合判定）
            var sumX = 0f; var sumY = 0f; var sumVy = 0f
            for (i in start until start + cnt) {
                sumX += positions[i].x
                sumY += positions[i].y
                sumVy += velocities[i].y
            }
            val cx = sumX / cnt
            val cy = sumY / cnt
            val avy = sumVy / cnt

            // 计算粒子与质心的最大距离平方
            var maxDistSq = 0f
            for (i in start until start + cnt) {
                val dx = positions[i].x - cx
                val dy = positions[i].y - cy
                val distSq = dx * dx + dy * dy
                if (distSq > maxDistSq) maxDistSq = distSq
            }

            val info = _groupInfoPool[infoIdx++]
            info.cohesive = maxDistSq < COHESIVE_SPREAD_SQ && avy > COHESIVE_MIN_VY
            info.bufferStart = start
            info.particleCount = cnt
        }
        _groupInfoCount = infoIdx
    }

    // ────────── 私有：创建粒子组 ──────────

    /**
     * 创建一个新的水粒子组
     *
     * @param seedIndex 初始化时传入的序号，用于错开生成时间和高度
     */
    private fun createGroup(seedIndex: Int = -1): ParticleGroup {
        val def = ParticleGroupDef()

        // 粒子组形状：细长矩形，宽约 1/300 屏幕，高约 1/25 屏幕
        val shape = PolygonShape()
        shape.setAsBox(width / (300f * proportion), width / (25f * proportion))
        def.shape = shape
        def.flags = ParticleType.b2_waterParticle

        // 最大雨档：提高初始下落速度，并保留源实现的 15% 波动
        val baseV = MAX_RAIN_BASE_VELOCITY
        val varV  = baseV * MAX_RAIN_VELOCITY_VARIANCE_RATIO
        def.linearVelocity.set(
            0f,
            baseV + (Random.nextFloat() - 0.5f) * 2f * varV
        )

        // 水平位置：5 列网格均匀分布，每列内随机偏移
        val cols    = 5
        val colIdx  = nextGridIndex % cols
        nextGridIndex = (nextGridIndex + 1) % cols
        val colW    = width.toFloat() / cols
        val xPos    = (colIdx * colW + Random.nextFloat() * colW) / proportion

        // 垂直位置：初始化时按序号错开高度；重生时从上方随机位置落下
        val yPos = if (seedIndex >= 0) {
            -height / proportion * (0.5f + seedIndex * 0.15f)
        } else {
            -height / proportion * (0.3f + Random.nextFloat() * 0.4f)
        }
        def.position.set(xPos, yPos)

        val group = world.createParticleGroup(def)
        // userData 存储出生时间戳（加上种子延迟），用于生命周期判断
        val delay = if (seedIndex >= 0) seedIndex * 120L else 0L
        group.userData = System.currentTimeMillis() + delay
        return group
    }
}
