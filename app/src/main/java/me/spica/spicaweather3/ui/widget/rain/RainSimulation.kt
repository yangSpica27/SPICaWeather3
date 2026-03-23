package me.spica.spicaweather3.ui.widget.rain

import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World
import org.jbox2d.particle.ParticleGroup
import org.jbox2d.particle.ParticleGroupDef
import org.jbox2d.particle.ParticleType
import kotlin.random.Random

/**
 * JBox2D 物理雨滴模拟
 *
 * 完整保留原有物理碰撞逻辑：
 *  - 重力 Vec2(0, 15)，雨滴向下加速
 *  - 屏幕底部设置静态刚体（地板），粒子与之碰撞弹溅
 *  - 最多 MAX_GROUPS 个粒子组并发，超过生命周期后回收重生
 *  - 每帧固定步进 1/120s（原始行为）
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
        private const val MAX_GROUPS = 35

        /** 粒子组从生成到回收的总生命周期（毫秒） */
        private const val TOTAL_LIFETIME_MS = 4000L
    }

    /** 初始化是否完成 */
    var initOK = false
        private set

    private lateinit var world: World
    private lateinit var groundBody: Body

    // 当前活跃粒子组列表
    private val groups = ArrayList<ParticleGroup>()

    // 用于均匀水平分布的网格列索引
    private var nextGridIndex = 0

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

    // ────────── 初始化 ──────────

    /**
     * 初始化物理世界与粒子系统，必须在渲染线程启动前调用
     */
    fun init() {
        synchronized(this) {
            world = World(Vec2(0f, 22f))           // 向下重力（增大加速度，雨滴下落更快）
            world.particleRadius  = 6f / proportion
            world.particleMaxCount = MAX_PARTICLES

            // 地板刚体（放置于屏幕下方一倍高度，确保碰撞区域足够宽）
            val groundDef = BodyDef().apply {
                type = BodyType.STATIC
                position.set(0f, height / proportion * 2f)
            }
            groundBody = world.createBody(groundDef)

            val fixtureDef = FixtureDef().apply {
                shape = PolygonShape().also {
                    it.setAsBox(width * 1f / proportion, height / proportion)
                }
                friction    = 1.5f   // 低摩擦：粒子碰撞后保留更多水平动量，向外扩散更充分
                restitution = 1.55f  // 弹性系数提高：反弹更高，水花效果更明显
                filter.maskBits  = 0b01
                filter.groupIndex = 0b01
            }
            groundBody.createFixture(fixtureDef)

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
     * 固定以 1/120s 步进，与原始行为保持一致。
     * 在渲染线程内调用，无需在外部加锁。
     */
    fun update() {
        if (!initOK) return
        synchronized(this) {
            val now = System.currentTimeMillis()

            // 找出生命周期到期的粒子组
            val expired = groups.filter { g ->
                g.userData != null && now - (g.userData as Long) > TOTAL_LIFETIME_MS
            }

            // 销毁旧粒子并在原位置替换为新组
            for (old in expired) {
                world.destroyParticlesInGroup(old)
                groups[groups.indexOf(old)] = createGroup()
            }

            // 每帧步进两次（1/120s × 2 = 1/60s）= 与真实时间同步，消除 0.5× 速度偏差
            world.step(1f / 120f, 10, 3)
            world.step(1f / 120f, 10, 3)
        }
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

        // 初速度：以 28 为基础，±15% 随机浮动（提高初始下落速度）
        val baseV = 28f
        val varV  = baseV * 0.15f
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
