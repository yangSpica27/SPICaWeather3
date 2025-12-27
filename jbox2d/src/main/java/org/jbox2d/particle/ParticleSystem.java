package org.jbox2d.particle;

import java.lang.reflect.Array;
import java.util.Arrays;

import org.jbox2d.callbacks.ParticleDestructionListener;
import org.jbox2d.callbacks.ParticleQueryCallback;
import org.jbox2d.callbacks.ParticleRaycastCallback;
import org.jbox2d.callbacks.QueryCallback;
import org.jbox2d.collision.AABB;
import org.jbox2d.collision.RayCastInput;
import org.jbox2d.collision.RayCastOutput;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.BufferUtils;
import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Rot;
import org.jbox2d.common.Settings;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.TimeStep;
import org.jbox2d.dynamics.World;
import org.jbox2d.particle.VoronoiDiagram.VoronoiDiagramCallback;

/**
 * 粒子系统，管理粒子的创建、销毁、碰撞和力学模拟。
 * JBox2D 粒子系统是 Box2D 物理引擎的扩展，用于模拟液体、沙子等流体行为。
 */
public class ParticleSystem {
  /** 所有需要创建“对”（pairs）的粒子类型，例如弹簧粒子。 */
  private static final int k_pairFlags = ParticleType.b2_springParticle;
  /** 所有需要创建“三元组”（triads）的粒子类型，例如弹性粒子。 */
  private static final int k_triadFlags = ParticleType.b2_elasticParticle;
  /** 所有不需要计算压力的粒子类型，例如粉末粒子。 */
  private static final int k_noPressureFlags = ParticleType.b2_powderParticle;

  // 以下是用于空间哈希（spatial hashing）的位操作常量，用于高效查询粒子邻居。
  static final int xTruncBits = 12;
  static final int yTruncBits = 12;
  static final int tagBits = 8 * 4 - 1  /* sizeof(int) */; // 31位
  static final long yOffset = 1 << (yTruncBits - 1);
  static final int yShift = tagBits - yTruncBits;
  static final int xShift = tagBits - yTruncBits - xTruncBits;
  static final long xScale = 1 << xShift;
  static final long xOffset = xScale * (1 << (xTruncBits - 1));
  static final int xMask = (1 << xTruncBits) - 1;
  static final int yMask = (1 << yTruncBits) - 1;

  /**
   * 计算给定世界坐标 (x, y) 的空间哈希标签。
   * 用于将二维坐标映射到一维长整型，以便进行邻居查询。
   * @param x 世界坐标X
   * @param y 世界坐标Y
   * @return 粒子的空间哈希标签
   */
  static long computeTag(float x, float y) {
    return (((long) (y + yOffset)) << yShift) + (((long) (xScale * x)) + xOffset);
  }

  /**
   * 计算相对于给定标签的偏移标签。
   * 用于快速查找相邻格子（cell）的标签。
   * @param tag 基础标签
   * @param x X方向的格子偏移
   * @param y Y方向的格子偏移
   * @return 相对标签
   */
  static long computeRelativeTag(long tag, int x, int y) {
    return tag + (y << yShift) + (x << xShift);
  }

  /**
   * 限制给定容量不超过最大值。
   * @param capacity 当前容量
   * @param maxCount 最大允许数量
   * @return 限制后的容量
   */
  static int limitCapacity(int capacity, int maxCount) {
    return maxCount != 0 && capacity > maxCount ? maxCount : capacity;
  }

  int m_timestamp; // 时间戳，用于内部更新
  int m_allParticleFlags; // 所有粒子的标志位集合（按位或）
  int m_allGroupFlags; // 所有粒子组的标志位集合（按位或）
  float m_density; // 粒子密度
  float m_inverseDensity; // 粒子密度的倒数
  float m_gravityScale; // 重力缩放因子
  float m_particleDiameter; // 粒子直径
  float m_inverseDiameter; // 粒子直径的倒数
  float m_squaredDiameter; // 粒子直径的平方

  int m_count; // 当前粒子数量
  int m_internalAllocatedCapacity; // 内部已分配的粒子缓冲区容量
  int m_maxCount; // 粒子最大数量限制
  ParticleBufferInt m_flagsBuffer; // 粒子标志位缓冲区
  ParticleBuffer<Vec2> m_positionBuffer; // 粒子位置缓冲区
  ParticleBuffer<Vec2> m_velocityBuffer; // 粒子速度缓冲区
  float[] m_accumulationBuffer; // 临时累积值缓冲区 (浮点数)
  Vec2[] m_accumulation2Buffer; // 临时累积向量缓冲区
  float[] m_depthBuffer; // 粒子深度缓冲区 (表面距离)

  public ParticleBuffer<ParticleColor> m_colorBuffer; // 粒子颜色缓冲区
  ParticleGroup[] m_groupBuffer; // 粒子所属组的缓冲区
  ParticleBuffer<Object> m_userDataBuffer; // 粒子用户数据缓冲区

  int m_proxyCount; // 代理（Proxy）数量，用于空间哈希和碰撞检测
  int m_proxyCapacity; // 代理缓冲区容量
  Proxy[] m_proxyBuffer; // 代理缓冲区

  public int m_contactCount; // 粒子间接触数量
  int m_contactCapacity; // 粒子间接触缓冲区容量
  public ParticleContact[] m_contactBuffer; // 粒子间接触缓冲区

  public int m_bodyContactCount; // 粒子-刚体接触数量
  int m_bodyContactCapacity; // 粒子-刚体接触缓冲区容量
  public ParticleBodyContact[] m_bodyContactBuffer; // 粒子-刚体接触缓冲区

  int m_pairCount; // 粒子对数量
  int m_pairCapacity; // 粒子对缓冲区容量
  Pair[] m_pairBuffer; // 粒子对缓冲区

  int m_triadCount; // 粒子三元组数量
  int m_triadCapacity; // 粒子三元组缓冲区容量
  Triad[] m_triadBuffer; // 粒子三元组缓冲区

  int m_groupCount; // 粒子组数量
  ParticleGroup m_groupList; // 粒子组链表头

  // 各种物理效果的强度参数
  float m_pressureStrength; // 压力强度
  float m_dampingStrength; // 阻尼强度
  float m_elasticStrength; // 弹性强度
  float m_springStrength; // 弹簧强度
  float m_viscousStrength; // 粘性强度
  float m_surfaceTensionStrengthA; // 表面张力强度A
  float m_surfaceTensionStrengthB; // 表面张力强度B
  float m_powderStrength; // 粉末强度
  float m_ejectionStrength; // 喷射强度 (固体组排斥力)
  float m_colorMixingStrength; // 颜色混合强度

  World m_world; // 所属的物理世界

  /**
   * 构造函数。
   * @param world 粒子系统所属的物理世界。
   */
  public ParticleSystem(World world) {
    m_world = world;
    m_timestamp = 0;
    m_allParticleFlags = 0;
    m_allGroupFlags = 0;
    m_density = 1;
    m_inverseDensity = 1;
    m_gravityScale = 1;
    m_particleDiameter = 1;
    m_inverseDiameter = 1;
    m_squaredDiameter = 1;

    m_count = 0;
    m_internalAllocatedCapacity = 0;
    m_maxCount = 0;

    m_proxyCount = 0;
    m_proxyCapacity = 0;

    m_contactCount = 0;
    m_contactCapacity = 0;

    m_bodyContactCount = 0;
    m_bodyContactCapacity = 0;

    m_pairCount = 0;
    m_pairCapacity = 0;

    m_triadCount = 0;
    m_triadCapacity = 0;

    m_groupCount = 0;

    m_pressureStrength = 0.05f;
    m_dampingStrength = 1.0f;
    m_elasticStrength = 0.25f;
    m_springStrength = 0.25f;
    m_viscousStrength = 0.25f;
    m_surfaceTensionStrengthA = 0.1f;
    m_surfaceTensionStrengthB = 0.2f;
    m_powderStrength = 0.5f;
    m_ejectionStrength = 0.5f;
    m_colorMixingStrength = 0.5f;

    // 初始化粒子数据缓冲区
    m_flagsBuffer = new ParticleBufferInt();
    m_positionBuffer = new ParticleBuffer<Vec2>(Vec2.class);
    m_velocityBuffer = new ParticleBuffer<Vec2>(Vec2.class);
    m_colorBuffer = new ParticleBuffer<ParticleColor>(ParticleColor.class);
    m_userDataBuffer = new ParticleBuffer<Object>(Object.class);
  }

  //  public void assertNotSamePosition() {
  //    for (int i = 0; i < m_count; i++) {
  //      Vec2 vi = m_positionBuffer.data[i];
  //      for (int j = i + 1; j < m_count; j++) {
  //        Vec2 vj = m_positionBuffer.data[j];
  //        assert(vi.x != vj.x || vi.y != vj.y);
  //      }
  //    }
  //  }

  /**
   * 根据粒子定义创建一个新粒子。
   * 如果缓冲区容量不足，会自动重新分配。
   * @param def 粒子定义
   * @return 新创建粒子的索引，如果创建失败（容量不足）则返回 Settings.invalidParticleIndex
   */
  public int createParticle(ParticleDef def) {
    if (m_count >= m_internalAllocatedCapacity) {
      int capacity = m_count != 0 ? 2 * m_count : Settings.minParticleBufferCapacity;
      capacity = limitCapacity(capacity, m_maxCount);
      capacity = limitCapacity(capacity, m_flagsBuffer.userSuppliedCapacity);
      capacity = limitCapacity(capacity, m_positionBuffer.userSuppliedCapacity);
      capacity = limitCapacity(capacity, m_velocityBuffer.userSuppliedCapacity);
      capacity = limitCapacity(capacity, m_colorBuffer.userSuppliedCapacity);
      capacity = limitCapacity(capacity, m_userDataBuffer.userSuppliedCapacity);
      if (m_internalAllocatedCapacity < capacity) {
        // 重新分配所有粒子数据缓冲区
        m_flagsBuffer.data =
            reallocateBuffer(m_flagsBuffer, m_internalAllocatedCapacity, capacity, false);
        m_positionBuffer.data =
            reallocateBuffer(m_positionBuffer, m_internalAllocatedCapacity, capacity, false);
        m_velocityBuffer.data =
            reallocateBuffer(m_velocityBuffer, m_internalAllocatedCapacity, capacity, false);
        m_accumulationBuffer =
            BufferUtils.reallocateBuffer(m_accumulationBuffer, 0, m_internalAllocatedCapacity,
                capacity, false);
        m_accumulation2Buffer =
            BufferUtils.reallocateBuffer(Vec2.class, m_accumulation2Buffer, 0,
                m_internalAllocatedCapacity, capacity, true);
        m_depthBuffer =
            BufferUtils.reallocateBuffer(m_depthBuffer, 0, m_internalAllocatedCapacity, capacity,
                true);
        m_colorBuffer.data =
            reallocateBuffer(m_colorBuffer, m_internalAllocatedCapacity, capacity, true);
        m_groupBuffer =
            BufferUtils.reallocateBuffer(ParticleGroup.class, m_groupBuffer, 0,
                m_internalAllocatedCapacity, capacity, false);
        m_userDataBuffer.data =
            reallocateBuffer(m_userDataBuffer, m_internalAllocatedCapacity, capacity, true);
        m_internalAllocatedCapacity = capacity;
      }
    }
    if (m_count >= m_internalAllocatedCapacity) {
      return Settings.invalidParticleIndex; // 容量不足，创建失败
    }
    int index = m_count++; // 获取新粒子的索引并增加计数
    m_flagsBuffer.data[index] = def.flags; // 设置粒子标志
    m_positionBuffer.data[index].set(def.position); // 设置粒子位置
    //    assertNotSamePosition(); // 调试断言，确保位置不重复
    m_velocityBuffer.data[index].set(def.velocity); // 设置粒子速度
    m_groupBuffer[index] = null; // 初始化粒子所属组为null
    if (m_depthBuffer != null) {
      m_depthBuffer[index] = 0; // 初始化深度
    }
    if (m_colorBuffer.data != null || def.color != null) {
      m_colorBuffer.data = requestParticleBuffer(m_colorBuffer.dataClass, m_colorBuffer.data);
      m_colorBuffer.data[index].set(def.color); // 设置粒子颜色
    }
    if (m_userDataBuffer.data != null || def.userData != null) {
      m_userDataBuffer.data =
          requestParticleBuffer(m_userDataBuffer.dataClass, m_userDataBuffer.data);
      m_userDataBuffer.data[index] = def.userData; // 设置粒子用户数据
    }
    // 重新分配代理缓冲区（如果需要）
    if (m_proxyCount >= m_proxyCapacity) {
      int oldCapacity = m_proxyCapacity;
      int newCapacity = m_proxyCount != 0 ? 2 * m_proxyCount : Settings.minParticleBufferCapacity;
      m_proxyBuffer =
          BufferUtils.reallocateBuffer(Proxy.class, m_proxyBuffer, oldCapacity, newCapacity);
      m_proxyCapacity = newCapacity;
    }
    m_proxyBuffer[m_proxyCount++].index = index; // 创建一个新代理并关联到粒子
    return index; // 返回新粒子的索引
  }

  /**
   * 销毁指定索引的粒子。
   * 实际上是给粒子打上僵尸（b2_zombieParticle）标志，并在后续的 solveZombie() 中进行清理。
   * @param index 要销毁的粒子索引
   * @param callDestructionListener 是否调用销毁监听器
   */
  public void destroyParticle(int index, boolean callDestructionListener) {
    int flags = ParticleType.b2_zombieParticle;
    if (callDestructionListener) {
      flags |= ParticleType.b2_destructionListener;
    }
    m_flagsBuffer.data[index] |= flags; // 打上僵尸和/或销毁监听器标志
  }

  private final AABB temp = new AABB(); // 临时AABB对象
  private final DestroyParticlesInShapeCallback dpcallback = new DestroyParticlesInShapeCallback(); // 销毁粒子回调

  /**
   * 销毁指定形状内的所有粒子。
   * @param shape 形状
   * @param xf 形状的变换
   * @param callDestructionListener 是否调用销毁监听器
   * @return 被销毁的粒子数量
   */
  public int destroyParticlesInShape(Shape shape, Transform xf, boolean callDestructionListener) {
    dpcallback.init(this, shape, xf, callDestructionListener); // 初始化回调
    shape.computeAABB(temp, xf, 0); // 计算形状的AABB
    m_world.queryAABB(dpcallback, temp); // 查询AABB内的粒子并销毁
    return dpcallback.destroyed; // 返回被销毁的粒子数量
  }

  /**
   * 销毁指定粒子组内的所有粒子。
   * @param group 要销毁的粒子组
   * @param callDestructionListener 是否调用销毁监听器
   */
  public void destroyParticlesInGroup(ParticleGroup group, boolean callDestructionListener) {
    for (int i = group.m_firstIndex; i < group.m_lastIndex; i++) {
      destroyParticle(i, callDestructionListener); // 逐个销毁组内的粒子
    }
  }

  private final AABB temp2 = new AABB(); // 临时AABB对象
  private final Vec2 tempVec = new Vec2(); // 临时向量对象
  private final Transform tempTransform = new Transform(); // 临时变换对象
  private final Transform tempTransform2 = new Transform(); // 临时变换对象
  private CreateParticleGroupCallback createParticleGroupCallback =
      new CreateParticleGroupCallback(); // 创建粒子组回调
  private final ParticleDef tempParticleDef = new ParticleDef(); // 临时粒子定义

  /**
   * 根据粒子组定义创建一个新粒子组。
   * 粒子会填充形状定义的区域。
   * @param groupDef 粒子组定义
   * @return 新创建的粒子组
   */
  public ParticleGroup createParticleGroup(ParticleGroupDef groupDef) {
    float stride = getParticleStride(); // 获取粒子间距
    final Transform identity = tempTransform;
    identity.setIdentity(); // 恒等变换
    Transform transform = tempTransform2;
    transform.setIdentity(); // 恒等变换
    int firstIndex = m_count; // 记录粒子组的起始索引
    if (groupDef.shape != null) {
      final ParticleDef particleDef = tempParticleDef;
      particleDef.flags = groupDef.flags; // 设置粒子标志
      particleDef.color = groupDef.color; // 设置粒子颜色
      particleDef.userData = groupDef.userData; // 设置粒子用户数据
      Shape shape = groupDef.shape;
      transform.set(groupDef.position, groupDef.angle); // 设置形状的变换
      AABB aabb = temp;
      int childCount = shape.getChildCount();
      // 计算形状的联合AABB
      for (int childIndex = 0; childIndex < childCount; childIndex++) {
        if (childIndex == 0) {
          shape.computeAABB(aabb, identity, childIndex);
        } else {
          AABB childAABB = temp2;
          shape.computeAABB(childAABB, identity, childIndex);
          aabb.combine(childAABB);
        }
      }
      final float upperBoundY = aabb.upperBound.y;
      final float upperBoundX = aabb.upperBound.x;
      // 在形状的AABB范围内以步长创建粒子
      for (float y = MathUtils.floor(aabb.lowerBound.y / stride) * stride; y < upperBoundY; y +=
          stride) {
        for (float x = MathUtils.floor(aabb.lowerBound.x / stride) * stride; x < upperBoundX; x +=
            stride) {
          Vec2 p = tempVec;
          p.x = x;
          p.y = y;
          // 如果点在形状内，则创建粒子
          if (shape.testPoint(identity, p)) {
            Transform.mulToOut(transform, p, p); // 将点转换到世界坐标
            particleDef.position.x = p.x;
            particleDef.position.y = p.y;
            p.subLocal(groupDef.position); // 相对于组中心点的偏移
            Vec2.crossToOutUnsafe(groupDef.angularVelocity, p, particleDef.velocity); // 计算角速度引起的线速度
            particleDef.velocity.addLocal(groupDef.linearVelocity); // 加上线速度
            createParticle(particleDef); // 创建粒子
          }
        }
      }
    }
    int lastIndex = m_count; // 记录粒子组的结束索引

    ParticleGroup group = new ParticleGroup(); // 创建新的粒子组对象
    group.m_system = this;
    group.m_firstIndex = firstIndex;
    group.m_lastIndex = lastIndex;
    group.m_groupFlags = groupDef.groupFlags;
    group.m_strength = groupDef.strength;
    group.m_userData = groupDef.userData;
    group.m_transform.set(transform);
    group.m_destroyAutomatically = groupDef.destroyAutomatically;
    // 将新组添加到组链表
    group.m_prev = null;
    group.m_next = m_groupList;
    if (m_groupList != null) {
      m_groupList.m_prev = group;
    }
    m_groupList = group;
    ++m_groupCount;
    // 更新组内每个粒子的所属组
    for (int i = firstIndex; i < lastIndex; i++) {
      m_groupBuffer[i] = group;
    }

    updateContacts(true); // 更新粒子接触
    // 如果需要创建粒子对
    if ((groupDef.flags & k_pairFlags) != 0) {
      for (int k = 0; k < m_contactCount; k++) {
        ParticleContact contact = m_contactBuffer[k];
        int a = contact.indexA;
        int b = contact.indexB;
        if (a > b) { // 确保a < b
          int temp = a;
          a = b;
          b = temp;
        }
        if (firstIndex <= a && b < lastIndex) { // 如果接触的两个粒子都在新组内
          // 重新分配粒子对缓冲区（如果需要）
          if (m_pairCount >= m_pairCapacity) {
            int oldCapacity = m_pairCapacity;
            int newCapacity =
                m_pairCount != 0 ? 2 * m_pairCount : Settings.minParticleBufferCapacity;
            m_pairBuffer =
                BufferUtils.reallocateBuffer(Pair.class, m_pairBuffer, oldCapacity, newCapacity);
            m_pairCapacity = newCapacity;
          }
          Pair pair = m_pairBuffer[m_pairCount]; // 创建新粒子对
          pair.indexA = a;
          pair.indexB = b;
          pair.flags = contact.flags;
          pair.strength = groupDef.strength;
          pair.distance = MathUtils.distance(m_positionBuffer.data[a], m_positionBuffer.data[b]); // 记录初始距离
          m_pairCount++;
        }
      }
    }
    // 如果需要创建粒子三元组（弹性粒子）
    if ((groupDef.flags & k_triadFlags) != 0) {
      VoronoiDiagram diagram = new VoronoiDiagram(lastIndex - firstIndex);
      // 添加组内粒子作为沃罗诺伊图生成器
      for (int i = firstIndex; i < lastIndex; i++) {
        diagram.addGenerator(m_positionBuffer.data[i], i);
      }
      diagram.generate(stride / 2); // 生成沃罗诺伊图
      createParticleGroupCallback.system = this;
      createParticleGroupCallback.def = groupDef;
      createParticleGroupCallback.firstIndex = firstIndex;
      diagram.getNodes(createParticleGroupCallback); // 获取节点并创建三元组
    }
    // 如果是固体粒子组，计算深度
    if ((groupDef.groupFlags & ParticleGroupType.b2_solidParticleGroup) != 0) {
      computeDepthForGroup(group);
    }

    return group;
  }

  /**
   * 将两个粒子组合并为一个。
   * 合并后，groupB 会被销毁，其粒子并入 groupA。
   * @param groupA 主粒子组
   * @param groupB 待合并的粒子组
   */
  public void joinParticleGroups(ParticleGroup groupA, ParticleGroup groupB) {
    assert (groupA != groupB);
    // 旋转缓冲区以将 groupB 的粒子移动到末尾，然后将 groupA 的粒子移动到 groupB 之前
    RotateBuffer(groupB.m_firstIndex, groupB.m_lastIndex, m_count);
    assert (groupB.m_lastIndex == m_count);
    RotateBuffer(groupA.m_firstIndex, groupA.m_lastIndex, groupB.m_firstIndex);
    assert (groupA.m_lastIndex == groupB.m_firstIndex);

    int particleFlags = 0;
    for (int i = groupA.m_firstIndex; i < groupB.m_lastIndex; i++) {
      particleFlags |= m_flagsBuffer.data[i]; // 收集合并后所有粒子的标志
    }

    updateContacts(true); // 更新粒子接触
    // 如果需要创建粒子对
    if ((particleFlags & k_pairFlags) != 0) {
      for (int k = 0; k < m_contactCount; k++) {
        final ParticleContact contact = m_contactBuffer[k];
        int a = contact.indexA;
        int b = contact.indexB;
        if (a > b) {
          int temp = a;
          a = b;
          b = temp;
        }
        // 如果接触的两个粒子分别来自 groupA 和 groupB
        if (groupA.m_firstIndex <= a && a < groupA.m_lastIndex && groupB.m_firstIndex <= b
            && b < groupB.m_lastIndex) {
          // 重新分配粒子对缓冲区（如果需要）
          if (m_pairCount >= m_pairCapacity) {
            int oldCapacity = m_pairCapacity;
            int newCapacity =
                m_pairCount != 0 ? 2 * m_pairCount : Settings.minParticleBufferCapacity;
            m_pairBuffer =
                BufferUtils.reallocateBuffer(Pair.class, m_pairBuffer, oldCapacity, newCapacity);
            m_pairCapacity = newCapacity;
          }
          Pair pair = m_pairBuffer[m_pairCount]; // 创建新粒子对
          pair.indexA = a;
          pair.indexB = b;
          pair.flags = contact.flags;
          pair.strength = MathUtils.min(groupA.m_strength, groupB.m_strength); // 强度取两者最小值
          pair.distance = MathUtils.distance(m_positionBuffer.data[a], m_positionBuffer.data[b]);
          m_pairCount++;
        }
      }
    }
    // 如果需要创建粒子三元组
    if ((particleFlags & k_triadFlags) != 0) {
      VoronoiDiagram diagram = new VoronoiDiagram(groupB.m_lastIndex - groupA.m_firstIndex);
      // 添加合并后所有未标记为僵尸的粒子作为沃罗诺伊图生成器
      for (int i = groupA.m_firstIndex; i < groupB.m_lastIndex; i++) {
        if ((m_flagsBuffer.data[i] & ParticleType.b2_zombieParticle) == 0) {
          diagram.addGenerator(m_positionBuffer.data[i], i);
        }
      }
      diagram.generate(getParticleStride() / 2);
      JoinParticleGroupsCallback callback = new JoinParticleGroupsCallback();
      callback.system = this;
      callback.groupA = groupA;
      callback.groupB = groupB;
      diagram.getNodes(callback); // 获取节点并创建三元组
    }

    // 更新 groupB 中粒子的所属组为 groupA
    for (int i = groupB.m_firstIndex; i < groupB.m_lastIndex; i++) {
      m_groupBuffer[i] = groupA;
    }
    // 更新 groupA 的标志位和范围
    int groupFlags = groupA.m_groupFlags | groupB.m_groupFlags;
    groupA.m_groupFlags = groupFlags;
    groupA.m_lastIndex = groupB.m_lastIndex;
    groupB.m_firstIndex = groupB.m_lastIndex; // 将 groupB 标记为空
    destroyParticleGroup(groupB); // 销毁 groupB

    // 如果合并后的组是固体粒子组，重新计算深度
    if ((groupFlags & ParticleGroupType.b2_solidParticleGroup) != 0) {
      computeDepthForGroup(groupA);
    }
  }

  /**
   * 销毁一个粒子组。
   * (仅从 solveZombie() 或 joinParticleGroups() 调用。)
   * @param group 要销毁的粒子组
   */
  void destroyParticleGroup(ParticleGroup group) {
    assert (m_groupCount > 0);
    assert (group != null);

    // 如果设置了粒子销毁监听器，则调用之
    if (m_world.getParticleDestructionListener() != null) {
      m_world.getParticleDestructionListener().sayGoodbye(group);
    }

    // 将组内粒子的所属组设为null
    for (int i = group.m_firstIndex; i < group.m_lastIndex; i++) {
      m_groupBuffer[i] = null;
    }

    // 从链表中移除该组
    if (group.m_prev != null) {
      group.m_prev.m_next = group.m_next;
    }
    if (group.m_next != null) {
      group.m_next.m_prev = group.m_prev;
    }
    if (group == m_groupList) {
      m_groupList = group.m_next;
    }

    --m_groupCount; // 减少粒子组计数
  }

  /**
   * 为指定粒子组计算粒子深度。
   * 深度表示粒子与表面之间的距离，用于模拟固体粒子组的堆积和排斥。
   * @param group 粒子组
   */
  public void computeDepthForGroup(ParticleGroup group) {
    // 1. 初始化累积缓冲区
    for (int i = group.m_firstIndex; i < group.m_lastIndex; i++) {
      m_accumulationBuffer[i] = 0;
    }
    // 2. 累积组内粒子间的接触权重
    for (int k = 0; k < m_contactCount; k++) {
      final ParticleContact contact = m_contactBuffer[k];
      int a = contact.indexA;
      int b = contact.indexB;
      if (a >= group.m_firstIndex && a < group.m_lastIndex && b >= group.m_firstIndex
          && b < group.m_lastIndex) {
        float w = contact.weight;
        m_accumulationBuffer[a] += w;
        m_accumulationBuffer[b] += w;
      }
    }
    // 3. 初始化深度缓冲区
    m_depthBuffer = requestParticleBuffer(m_depthBuffer);
    for (int i = group.m_firstIndex; i < group.m_lastIndex; i++) {
      float w = m_accumulationBuffer[i];
      m_depthBuffer[i] = w < 0.8f ? 0 : Float.MAX_VALUE; // 接触权重不足0.8则深度为0，否则为无限大
    }
    // 4. 迭代计算深度（类似于广度优先搜索）
    int interationCount = group.getParticleCount();
    for (int t = 0; t < interationCount; t++) {
      boolean updated = false;
      for (int k = 0; k < m_contactCount; k++) {
        final ParticleContact contact = m_contactBuffer[k];
        int a = contact.indexA;
        int b = contact.indexB;
        if (a >= group.m_firstIndex && a < group.m_lastIndex && b >= group.m_firstIndex
            && b < group.m_lastIndex) {
          float r = 1 - contact.weight; // 1 - 权重 (距离因子)
          float ap0 = m_depthBuffer[a];
          float bp0 = m_depthBuffer[b];
          float ap1 = bp0 + r; // 从邻居b到a的深度
          float bp1 = ap0 + r; // 从邻居a到b的深度
          if (ap0 > ap1) { // 如果新深度更小，则更新
            m_depthBuffer[a] = ap1;
            updated = true;
          }
          if (bp0 > bp1) { // 如果新深度更小，则更新
            m_depthBuffer[b] = bp1;
            updated = true;
          }
        }
      }
      if (!updated) { // 如果一轮迭代没有更新，说明达到稳定状态
        break;
      }
    }
    // 5. 缩放深度
    for (int i = group.m_firstIndex; i < group.m_lastIndex; i++) {
      float p = m_depthBuffer[i];
      if (p < Float.MAX_VALUE) {
        m_depthBuffer[i] *= m_particleDiameter; // 深度乘以粒子直径
      } else {
        m_depthBuffer[i] = 0; // 无限深度设为0
      }
    }
  }

  /**
   * 添加两个粒子之间的接触。
   * 如果距离在直径范围内，则创建接触。
   * @param a 粒子A的索引
   * @param b 粒子B的索引
   */
  public void addContact(int a, int b) {
    assert(a != b); // 粒子不能是同一个
    Vec2 pa = m_positionBuffer.data[a];
    Vec2 pb = m_positionBuffer.data[b];
    float dx = pb.x - pa.x;
    float dy = pb.y - pa.y;
    float d2 = dx * dx + dy * dy; // 距离的平方
    //    assert(d2 != 0); // 调试断言，确保距离不为0
    if (d2 < m_squaredDiameter) { // 如果距离在直径范围内（碰撞）
      // 重新分配粒子接触缓冲区（如果需要）
      if (m_contactCount >= m_contactCapacity) {
        int oldCapacity = m_contactCapacity;
        int newCapacity =
            m_contactCount != 0 ? 2 * m_contactCount : Settings.minParticleBufferCapacity;
        m_contactBuffer =
            BufferUtils.reallocateBuffer(ParticleContact.class, m_contactBuffer, oldCapacity,
                newCapacity);
        m_contactCapacity = newCapacity;
      }
      float invD = d2 != 0 ? MathUtils.sqrt(1 / d2) : Float.MAX_VALUE; // 距离的倒数
      ParticleContact contact = m_contactBuffer[m_contactCount]; // 创建新接触
      contact.indexA = a;
      contact.indexB = b;
      contact.flags = m_flagsBuffer.data[a] | m_flagsBuffer.data[b]; // 接触标志是两个粒子标志的或运算
      contact.weight = 1 - d2 * invD * m_inverseDiameter; // 接触权重
      contact.normal.x = invD * dx;
      contact.normal.y = invD * dy; // 接触法线
      m_contactCount++; // 增加接触计数
    }
  }

  /**
   * 更新所有粒子间的接触。
   * 使用空间哈希和排序代理（proxies）来高效地查找潜在接触。
   * @param exceptZombie 是否排除僵尸粒子
   */
  public void updateContacts(boolean exceptZombie) {
    // 1. 为每个粒子计算空间哈希标签并更新代理
    for (int p = 0; p < m_proxyCount; p++) {
      Proxy proxy = m_proxyBuffer[p];
      int i = proxy.index;
      Vec2 pos = m_positionBuffer.data[i];
      proxy.tag = computeTag(m_inverseDiameter * pos.x, m_inverseDiameter * pos.y);
    }
    // 2. 根据标签对代理进行排序
    Arrays.sort(m_proxyBuffer, 0, m_proxyCount);
    m_contactCount = 0; // 重置接触计数
    int c_index = 0;
    // 3. 遍历排序后的代理，查找相邻粒子并添加接触
    for (int i = 0; i < m_proxyCount; i++) {
      Proxy a = m_proxyBuffer[i];
      long rightTag = computeRelativeTag(a.tag, 1, 0); // 右侧邻居的标签
      for (int j = i + 1; j < m_proxyCount; j++) {
        Proxy b = m_proxyBuffer[j];
        if (rightTag < b.tag) { // 如果b的标签超过右侧范围，则后续粒子更远
          break;
        }
        addContact(a.index, b.index); // 添加接触
      }
      // 检查左下方的邻居
      long bottomLeftTag = computeRelativeTag(a.tag, -1, 1);
      for (; c_index < m_proxyCount; c_index++) {
        Proxy c = m_proxyBuffer[c_index];
        if (bottomLeftTag <= c.tag) {
          break;
        }
      }
      // 检查右下方的邻居
      long bottomRightTag = computeRelativeTag(a.tag, 1, 1);

      for (int b_index = c_index; b_index < m_proxyCount; b_index++) {
        Proxy b = m_proxyBuffer[b_index];
        if (bottomRightTag < b.tag) {
          break;
        }
        addContact(a.index, b.index); // 添加接触
      }
    }
    // 4. 如果排除僵尸粒子，则移除带有僵尸标志的接触
    if (exceptZombie) {
      int j = m_contactCount;
      for (int i = 0; i < j; i++) {
        if ((m_contactBuffer[i].flags & ParticleType.b2_zombieParticle) != 0) {
          --j; // 减少有效接触计数
          // 将僵尸接触与最后一个有效接触交换，然后重新检查当前位置
          ParticleContact temp = m_contactBuffer[j];
          m_contactBuffer[j] = m_contactBuffer[i];
          m_contactBuffer[i] = temp;
          --i;
        }
      }
      m_contactCount = j; // 更新接触总数
    }
  }

  private final UpdateBodyContactsCallback ubccallback = new UpdateBodyContactsCallback(); // 更新刚体接触回调

  /**
   * 更新所有粒子与刚体之间的接触。
   * 首先计算所有粒子的包围盒，然后查询世界中与该包围盒重叠的刚体。
   */
  public void updateBodyContacts() {
    final AABB aabb = temp;
    // 1. 计算所有粒子的联合AABB
    aabb.lowerBound.x = Float.MAX_VALUE;
    aabb.lowerBound.y = Float.MAX_VALUE;
    aabb.upperBound.x = -Float.MAX_VALUE;
    aabb.upperBound.y = -Float.MAX_VALUE;
    for (int i = 0; i < m_count; i++) {
      Vec2 p = m_positionBuffer.data[i];
      Vec2.minToOut(aabb.lowerBound, p, aabb.lowerBound);
      Vec2.maxToOut(aabb.upperBound, p, aabb.upperBound);
    }
    // 2. 扩展AABB，考虑到粒子直径
    aabb.lowerBound.x -= m_particleDiameter;
    aabb.lowerBound.y -= m_particleDiameter;
    aabb.upperBound.x += m_particleDiameter;
    aabb.upperBound.y += m_particleDiameter;
    m_bodyContactCount = 0; // 重置粒子-刚体接触计数

    ubccallback.system = this;
    m_world.queryAABB(ubccallback, aabb); // 查询世界中与扩展AABB重叠的刚体
  }

  private SolveCollisionCallback sccallback = new SolveCollisionCallback(); // 解决碰撞回调

  /**
   * 解决粒子与刚体之间的碰撞。
   * 计算粒子在时间步长内的运动轨迹，然后对轨迹进行射线投射检测。
   * @param step 时间步长
   */
  public void solveCollision(TimeStep step) {
    final AABB aabb = temp;
    final Vec2 lowerBound = aabb.lowerBound;
    final Vec2 upperBound = aabb.upperBound;
    // 1. 计算粒子在当前和下一个时间步之间的运动范围的AABB
    lowerBound.x = Float.MAX_VALUE;
    lowerBound.y = Float.MAX_VALUE;
    upperBound.x = -Float.MAX_VALUE;
    upperBound.y = -Float.MAX_VALUE;
    for (int i = 0; i < m_count; i++) {
      final Vec2 v = m_velocityBuffer.data[i];
      final Vec2 p1 = m_positionBuffer.data[i];
      final float p1x = p1.x;
      final float p1y = p1.y;
      final float p2x = p1x + step.dt * v.x; // 下一时刻位置
      final float p2y = p1y + step.dt * v.y;
      final float bx = p1x < p2x ? p1x : p2x; // 运动轨迹的最小X
      final float by = p1y < p2y ? p1y : p2y; // 运动轨迹的最小Y
      lowerBound.x = lowerBound.x < bx ? lowerBound.x : bx;
      lowerBound.y = lowerBound.y < by ? lowerBound.y : by;
      final float b1x = p1x > p2x ? p1x : p2x; // 运动轨迹的最大X
      final float b1y = p1y > p2y ? p1y : p2y; // 运动轨迹的最大Y
      upperBound.x = upperBound.x > b1x ? upperBound.x : b1x;
      upperBound.y = upperBound.y > b1y ? upperBound.y : b1y;
    }
    sccallback.step = step;
    sccallback.system = this;
    m_world.queryAABB(sccallback, aabb); // 查询世界中与运动轨迹AABB重叠的刚体
  }

  /**
   * 执行粒子系统的一个时间步模拟。
   * 包含多个解决阶段，例如僵尸粒子清理、重力应用、碰撞解决、各种力学效果计算等。
   * @param step 时间步长信息
   */
  public void solve(TimeStep step) {
    ++m_timestamp; // 更新时间戳
    if (m_count == 0) {
      return; // 没有粒子，直接返回
    }
    // 收集所有粒子的标志
    m_allParticleFlags = 0;
    for (int i = 0; i < m_count; i++) {
      m_allParticleFlags |= m_flagsBuffer.data[i];
    }
    // 如果存在僵尸粒子，则进行清理
    if ((m_allParticleFlags & ParticleType.b2_zombieParticle) != 0) {
      solveZombie();
    }
    if (m_count == 0) {
      return; // 清理后可能没有粒子了
    }
    // 收集所有粒子组的标志
    m_allGroupFlags = 0;
    for (ParticleGroup group = m_groupList; group != null; group = group.getNext()) {
      m_allGroupFlags |= group.m_groupFlags;
    }
    // 应用重力
    final float gravityx = step.dt * m_gravityScale * m_world.getGravity().x;
    final float gravityy = step.dt * m_gravityScale * m_world.getGravity().y;
    float criticalVelocytySquared = getCriticalVelocitySquared(step); // 临界速度平方
    for (int i = 0; i < m_count; i++) {
      Vec2 v = m_velocityBuffer.data[i];
      v.x += gravityx;
      v.y += gravityy;
      float v2 = v.x * v.x + v.y * v.y;
      // 限制粒子速度不超过临界速度
      if (v2 > criticalVelocytySquared) {
        float a = v2 == 0 ? Float.MAX_VALUE : MathUtils.sqrt(criticalVelocytySquared / v2);
        v.x *= a;
        v.y *= a;
      }
    }
    // 解决碰撞
    solveCollision(step);
    // 解决刚体粒子组（如果存在）
    if ((m_allGroupFlags & ParticleGroupType.b2_rigidParticleGroup) != 0) {
      solveRigid(step);
    }
    // 解决壁粒子（如果存在）
    if ((m_allParticleFlags & ParticleType.b2_wallParticle) != 0) {
      solveWall(step);
    }
    // 更新粒子位置
    for (int i = 0; i < m_count; i++) {
      Vec2 pos = m_positionBuffer.data[i];
      Vec2 vel = m_velocityBuffer.data[i];
      pos.x += step.dt * vel.x;
      pos.y += step.dt * vel.y;
    }
    updateBodyContacts(); // 更新粒子-刚体接触
    updateContacts(false); // 更新粒子间接触（不排除僵尸粒子，因为清理已完成）
    // 解决各种粒子效应
    if ((m_allParticleFlags & ParticleType.b2_viscousParticle) != 0) {
      solveViscous(step);
    }
    if ((m_allParticleFlags & ParticleType.b2_powderParticle) != 0) {
      solvePowder(step);
    }
    if ((m_allParticleFlags & ParticleType.b2_tensileParticle) != 0) {
      solveTensile(step);
    }
    if ((m_allParticleFlags & ParticleType.b2_elasticParticle) != 0) {
      solveElastic(step);
    }
    if ((m_allParticleFlags & ParticleType.b2_springParticle) != 0) {
      solveSpring(step);
    }
    if ((m_allGroupFlags & ParticleGroupType.b2_solidParticleGroup) != 0) {
      solveSolid(step);
    }
    if ((m_allParticleFlags & ParticleType.b2_colorMixingParticle) != 0) {
      solveColorMixing(step);
    }
    solvePressure(step); // 解决压力
    solveDamping(step); // 解决阻尼
  }

  /**
   * 解决粒子压力。
   * 计算每个粒子的“密度”（接触权重之和），然后根据密度应用压力。
   * @param step 时间步长
   */
  void solvePressure(TimeStep step) {
    // 1. 计算每个粒子的累积接触权重（无量纲密度）
    for (int i = 0; i < m_count; i++) {
      m_accumulationBuffer[i] = 0;
    }
    for (int k = 0; k < m_bodyContactCount; k++) {
      ParticleBodyContact contact = m_bodyContactBuffer[k];
      int a = contact.index;
      float w = contact.weight;
      m_accumulationBuffer[a] += w;
    }
    for (int k = 0; k < m_contactCount; k++) {
      ParticleContact contact = m_contactBuffer[k];
      int a = contact.indexA;
      int b = contact.indexB;
      float w = contact.weight;
      m_accumulationBuffer[a] += w;
      m_accumulationBuffer[b] += w;
    }
    // 2. 忽略粉末粒子（不产生压力）
    if ((m_allParticleFlags & k_noPressureFlags) != 0) {
      for (int i = 0; i < m_count; i++) {
        if ((m_flagsBuffer.data[i] & k_noPressureFlags) != 0) {
          m_accumulationBuffer[i] = 0;
        }
      }
    }
    // 3. 将累积权重转换为压力（密度的线性函数）
    float pressurePerWeight = m_pressureStrength * getCriticalPressure(step);
    for (int i = 0; i < m_count; i++) {
      float w = m_accumulationBuffer[i];
      // 压力与 (权重 - 最小权重) 成正比，并限制最大权重
      float h =
          pressurePerWeight
              * MathUtils.max(0.0f, MathUtils.min(w, Settings.maxParticleWeight)
              - Settings.minParticleWeight);
      m_accumulationBuffer[i] = h; // 存储计算出的压力
    }
    // 4. 应用粒子-刚体接触之间的压力
    float velocityPerPressure = step.dt / (m_density * m_particleDiameter);
    for (int k = 0; k < m_bodyContactCount; k++) {
      ParticleBodyContact contact = m_bodyContactBuffer[k];
      int a = contact.index;
      Body b = contact.body;
      float w = contact.weight;
      float m = contact.mass;
      Vec2 n = contact.normal;
      Vec2 p = m_positionBuffer.data[a];
      float h = m_accumulationBuffer[a] + pressurePerWeight * w; // 考虑粒子自身和接触点的压力
      final Vec2 f = tempVec;
      final float coef = velocityPerPressure * w * m * h;
      f.x = coef * n.x;
      f.y = coef * n.y; // 压力产生的力
      final Vec2 velData = m_velocityBuffer.data[a];
      final float particleInvMass = getParticleInvMass();
      velData.x -= particleInvMass * f.x; // 粒子受力反向
      velData.y -= particleInvMass * f.y;
      b.applyLinearImpulse(f, p, true); // 刚体受力
    }
    // 5. 应用粒子-粒子接触之间的压力
    for (int k = 0; k < m_contactCount; k++) {
      ParticleContact contact = m_contactBuffer[k];
      int a = contact.indexA;
      int b = contact.indexB;
      float w = contact.weight;
      Vec2 n = contact.normal;
      float h = m_accumulationBuffer[a] + m_accumulationBuffer[b]; // 两个粒子压力的和
      final float fx = velocityPerPressure * w * h * n.x;
      final float fy = velocityPerPressure * w * h * n.y; // 压力产生的力
      final Vec2 velDataA = m_velocityBuffer.data[a];
      final Vec2 velDataB = m_velocityBuffer.data[b];
      velDataA.x -= fx; // 粒子A受力反向
      velDataA.y -= fy;
      velDataB.x += fx; // 粒子B受力正向
      velDataB.y += fy;
    }
  }

  /**
   * 解决粒子阻尼。
   * 减小接触点的法向相对速度。
   * @param step 时间步长
   */
  void solveDamping(TimeStep step) {
    float damping = m_dampingStrength; // 阻尼强度
    // 1. 粒子-刚体接触的阻尼
    for (int k = 0; k < m_bodyContactCount; k++) {
      final ParticleBodyContact contact = m_bodyContactBuffer[k];
      int a = contact.index;
      Body b = contact.body;
      float w = contact.weight;
      float m = contact.mass;
      Vec2 n = contact.normal;
      Vec2 p = m_positionBuffer.data[a];
      final float tempX = p.x - b.m_sweep.c.x;
      final float tempY = p.y - b.m_sweep.c.y;
      final Vec2 velA = m_velocityBuffer.data[a];
      // 计算粒子相对于刚体接触点的速度
      float vx = -b.m_angularVelocity * tempY + b.m_linearVelocity.x - velA.x;
      float vy = b.m_angularVelocity * tempX + b.m_linearVelocity.y - velA.y;
      // 计算法向速度
      float vn = vx * n.x + vy * n.y;
      if (vn < 0) { // 如果法向速度指向内（正在接近）
        final Vec2 f = tempVec;
        f.x = damping * w * m * vn * n.x;
        f.y = damping * w * m * vn * n.y; // 阻尼力
        final float invMass = getParticleInvMass();
        velA.x += invMass * f.x; // 粒子受力
        velA.y += invMass * f.y;
        f.x = -f.x;
        f.y = -f.y;
        b.applyLinearImpulse(f, p, true); // 刚体受反向力
      }
    }
    // 2. 粒子-粒子接触的阻尼
    for (int k = 0; k < m_contactCount; k++) {
      final ParticleContact contact = m_contactBuffer[k];
      int a = contact.indexA;
      int b = contact.indexB;
      float w = contact.weight;
      Vec2 n = contact.normal;
      final Vec2 velA = m_velocityBuffer.data[a];
      final Vec2 velB = m_velocityBuffer.data[b];
      final float vx = velB.x - velA.x; // 相对速度
      final float vy = velB.y - velA.y;
      float vn = vx * n.x + vy * n.y; // 法向相对速度
      if (vn < 0) { // 如果法向速度指向内
        float fx = damping * w * vn * n.x;
        float fy = damping * w * vn * n.y; // 阻尼力
        velA.x += fx; // 粒子A受力
        velA.y += fy;
        velB.x -= fx; // 粒子B受反向力
        velB.y -= fy;
      }
    }
  }

  /**
   * 解决壁粒子（b2_wallParticle）的效应。
   * 壁粒子是静止的，其速度在每个时间步都被重置为零。
   * @param step 时间步长
   */
  public void solveWall(TimeStep step) {
    for (int i = 0; i < m_count; i++) {
      if ((m_flagsBuffer.data[i] & ParticleType.b2_wallParticle) != 0) {
        final Vec2 r = m_velocityBuffer.data[i];
        r.x = 0.0f; // 速度设为0
        r.y = 0.0f;
      }
    }
  }

  private final Vec2 tempVec2 = new Vec2(); // 临时向量对象
  private final Rot tempRot = new Rot(); // 临时旋转对象
  private final Transform tempXf = new Transform(); // 临时变换对象
  private final Transform tempXf2 = new Transform(); // 临时变换对象

  /**
   * 解决刚体粒子组（b2_rigidParticleGroup）的效应。
   * 刚体粒子组内的粒子作为一个整体进行运动，保持相对位置。
   * @param step 时间步长
   */
  void solveRigid(final TimeStep step) {
    for (ParticleGroup group = m_groupList; group != null; group = group.getNext()) {
      if ((group.m_groupFlags & ParticleGroupType.b2_rigidParticleGroup) != 0) {
        group.updateStatistics(); // 更新组的质心、线速度、角速度等统计信息
        Vec2 temp = tempVec;
        Vec2 cross = tempVec2;
        Rot rotation = tempRot;
        rotation.set(step.dt * group.m_angularVelocity); // 根据角速度计算旋转
        Rot.mulToOutUnsafe(rotation, group.m_center, cross); // 旋转质心
        temp.set(group.m_linearVelocity).mulLocal(step.dt).addLocal(group.m_center).subLocal(cross); // 计算新的质心位置
        tempXf.p.set(temp);
        tempXf.q.set(rotation); // 构造新的变换
        Transform.mulToOut(tempXf, group.m_transform, group.m_transform); // 应用新的变换到组的变换
        final Transform velocityTransform = tempXf2;
        // 计算速度变换 (差分近似)
        velocityTransform.p.x = step.inv_dt * tempXf.p.x;
        velocityTransform.p.y = step.inv_dt * tempXf.p.y;
        velocityTransform.q.s = step.inv_dt * tempXf.q.s;
        velocityTransform.q.c = step.inv_dt * (tempXf.q.c - 1);
        // 将速度变换应用到组内每个粒子的位置，从而得到其速度
        for (int i = group.m_firstIndex; i < group.m_lastIndex; i++) {
          Transform.mulToOutUnsafe(velocityTransform, m_positionBuffer.data[i],
              m_velocityBuffer.data[i]);
        }
      }
    }
  }

  /**
   * 解决弹性粒子（b2_elasticParticle）的效应。
   * 弹性粒子通过三元组（Triads）连接，试图保持其原始的相对构型。
   * @param step 时间步长
   */
  void solveElastic(final TimeStep step) {
    float elasticStrength = step.inv_dt * m_elasticStrength; // 弹性强度
    for (int k = 0; k < m_triadCount; k++) {
      final Triad triad = m_triadBuffer[k];
      if ((triad.flags & ParticleType.b2_elasticParticle) != 0) {
        int a = triad.indexA;
        int b = triad.indexB;
        int c = triad.indexC;
        final Vec2 oa = triad.pa; // 原始相对位置A
        final Vec2 ob = triad.pb; // 原始相对位置B
        final Vec2 oc = triad.pc; // 原始相对位置C
        final Vec2 pa = m_positionBuffer.data[a];
        final Vec2 pb = m_positionBuffer.data[b];
        final Vec2 pc = m_positionBuffer.data[c];
        final float px = 1f / 3 * (pa.x + pb.x + pc.x); // 当前三元组的质心
        final float py = 1f / 3 * (pa.y + pb.y + pc.y);
        // 计算当前构型相对于原始构型的旋转
        float rs = Vec2.cross(oa, pa) + Vec2.cross(ob, pb) + Vec2.cross(oc, pc);
        float rc = Vec2.dot(oa, pa) + Vec2.dot(ob, pb) + Vec2.dot(oc, pc);
        float r2 = rs * rs + rc * rc;
        float invR = r2 == 0 ? Float.MAX_VALUE : MathUtils.sqrt(1f / r2);
        rs *= invR;
        rc *= invR; // 归一化旋转分量
        final float strength = elasticStrength * triad.strength; // 三元组强度
        // 旋转原始相对位置到当前帧，并计算校正力
        final float roax = rc * oa.x - rs * oa.y;
        final float roay = rs * oa.x + rc * oa.y;
        final float robx = rc * ob.x - rs * ob.y;
        final float roby = rs * ob.x + rc * ob.y;
        final float rocx = rc * oc.x - rs * oc.y;
        final float rocy = rs * oc.x + rc * oc.y;
        final Vec2 va = m_velocityBuffer.data[a];
        final Vec2 vb = m_velocityBuffer.data[b];
        final Vec2 vc = m_velocityBuffer.data[c];
        // 应用弹性力，将粒子速度向理想位置调整
        va.x += strength * (roax - (pa.x - px));
        va.y += strength * (roay - (pa.y - py));
        vb.x += strength * (robx - (pb.x - px));
        vb.y += strength * (roby - (pb.y - py));
        vc.x += strength * (rocx - (pc.x - px));
        vc.y += strength * (rocy - (pc.y - py));
      }
    }
  }

  /**
   * 解决弹簧粒子（b2_springParticle）的效应。
   * 弹簧粒子通过粒子对（Pairs）连接，试图保持其原始的连接距离。
   * @param step 时间步长
   */
  void solveSpring(final TimeStep step) {
    float springStrength = step.inv_dt * m_springStrength; // 弹簧强度
    for (int k = 0; k < m_pairCount; k++) {
      final Pair pair = m_pairBuffer[k];
      if ((pair.flags & ParticleType.b2_springParticle) != 0) {
        int a = pair.indexA;
        int b = pair.indexB;
        final Vec2 pa = m_positionBuffer.data[a];
        final Vec2 pb = m_positionBuffer.data[b];
        final float dx = pb.x - pa.x;
        final float dy = pb.y - pa.y;
        float r0 = pair.distance; // 原始距离
        float r1 = MathUtils.sqrt(dx * dx + dy * dy); // 当前距离
        if (r1 == 0) r1 = Float.MAX_VALUE; // 避免除以零
        float strength = springStrength * pair.strength; // 弹簧强度
        final float fx = strength * (r0 - r1) / r1 * dx; // 弹簧力 (胡克定律)
        final float fy = strength * (r0 - r1) / r1 * dy;
        final Vec2 va = m_velocityBuffer.data[a];
        final Vec2 vb = m_velocityBuffer.data[b];
        va.x -= fx; // 粒子A受力
        va.y -= fy;
        vb.x += fx; // 粒子B受反向力
        vb.y += fy;
      }
    }
  }

  /**
   * 解决拉伸粒子（b2_tensileParticle）的效应（表面张力）。
   * @param step 时间步长
   */
  void solveTensile(final TimeStep step) {
    m_accumulation2Buffer = requestParticleBuffer(Vec2.class, m_accumulation2Buffer); // 请求临时缓冲区
    // 1. 初始化累积缓冲区
    for (int i = 0; i < m_count; i++) {
      m_accumulationBuffer[i] = 0;
      m_accumulation2Buffer[i].setZero();
    }
    // 2. 累积接触权重和法线方向的力
    for (int k = 0; k < m_contactCount; k++) {
      final ParticleContact contact = m_contactBuffer[k];
      if ((contact.flags & ParticleType.b2_tensileParticle) != 0) {
        int a = contact.indexA;
        int b = contact.indexB;
        float w = contact.weight;
        Vec2 n = contact.normal;
        m_accumulationBuffer[a] += w; // 累积权重
        m_accumulationBuffer[b] += w;
        final Vec2 a2A = m_accumulation2Buffer[a];
        final Vec2 a2B = m_accumulation2Buffer[b];
        final float inter = (1 - w) * w; // 权重因子
        a2A.x -= inter * n.x; // 累积法线方向的力
        a2A.y -= inter * n.y;
        a2B.x += inter * n.x;
        a2B.y += inter * n.y;
      }
    }
    // 3. 应用表面张力
    float strengthA = m_surfaceTensionStrengthA * getCriticalVelocity(step);
    float strengthB = m_surfaceTensionStrengthB * getCriticalVelocity(step);
    for (int k = 0; k < m_contactCount; k++) {
      final ParticleContact contact = m_contactBuffer[k];
      if ((contact.flags & ParticleType.b2_tensileParticle) != 0) {
        int a = contact.indexA;
        int b = contact.indexB;
        float w = contact.weight;
        Vec2 n = contact.normal;
        final Vec2 a2A = m_accumulation2Buffer[a];
        final Vec2 a2B = m_accumulation2Buffer[b];
        float h = m_accumulationBuffer[a] + m_accumulationBuffer[b]; // 两个粒子的累积权重和
        final float sx = a2B.x - a2A.x;
        final float sy = a2B.y - a2A.y;
        // 根据权重、法线方向的相对力以及表面张力强度计算最终的法向力
        float fn = (strengthA * (h - 2) + strengthB * (sx * n.x + sy * n.y)) * w;
        final float fx = fn * n.x;
        final float fy = fn * n.y;
        final Vec2 va = m_velocityBuffer.data[a];
        final Vec2 vb = m_velocityBuffer.data[b];
        va.x -= fx; // 粒子A受力
        va.y -= fy;
        vb.x += fx; // 粒子B受反向力
        vb.y += fy;
      }
    }
  }

  /**
   * 解决粘性粒子（b2_viscousParticle）的效应。
   * 粘性粒子试图减小其与周围环境（刚体或其它粒子）的相对速度。
   * @param step 时间步长
   */
  void solveViscous(final TimeStep step) {
    float viscousStrength = m_viscousStrength; // 粘性强度
    // 1. 粒子-刚体接触的粘性
    for (int k = 0; k < m_bodyContactCount; k++) {
      final ParticleBodyContact contact = m_bodyContactBuffer[k];
      int a = contact.index;
      if ((m_flagsBuffer.data[a] & ParticleType.b2_viscousParticle) != 0) {
        Body b = contact.body;
        float w = contact.weight;
        float m = contact.mass;
        Vec2 p = m_positionBuffer.data[a];
        final Vec2 va = m_velocityBuffer.data[a];
        final float tempX = p.x - b.m_sweep.c.x;
        final float tempY = p.y - b.m_sweep.c.y;
        // 计算粒子相对于刚体接触点的相对速度
        final float vx = -b.m_angularVelocity * tempY + b.m_linearVelocity.x - va.x;
        final float vy = b.m_angularVelocity * tempX + b.m_linearVelocity.y - va.y;
        final Vec2 f = tempVec;
        final float pInvMass = getParticleInvMass();
        f.x = viscousStrength * m * w * vx;
        f.y = viscousStrength * m * w * vy; // 粘性力
        va.x += pInvMass * f.x; // 粒子受力
        va.y += pInvMass * f.y;
        f.x = -f.x;
        f.y = -f.y;
        b.applyLinearImpulse(f, p, true); // 刚体受反向力
      }
    }
    // 2. 粒子-粒子接触的粘性
    for (int k = 0; k < m_contactCount; k++) {
      final ParticleContact contact = m_contactBuffer[k];
      if ((contact.flags & ParticleType.b2_viscousParticle) != 0) {
        int a = contact.indexA;
        int b = contact.indexB;
        float w = contact.weight;
        final Vec2 va = m_velocityBuffer.data[a];
        final Vec2 vb = m_velocityBuffer.data[b];
        final float vx = vb.x - va.x; // 相对速度
        final float vy = vb.y - va.y;
        final float fx = viscousStrength * w * vx;
        final float fy = viscousStrength * w * vy; // 粘性力
        va.x += fx; // 粒子A受力
        va.y += fy;
        vb.x -= fx; // 粒子B受反向力
        vb.y -= fy;
      }
    }
  }

  /**
   * 解决粉末粒子（b2_powderParticle）的效应。
   * 粉末粒子在接触点上表现出轻微的排斥力，防止过度堆积。
   * @param step 时间步长
   */
  void solvePowder(final TimeStep step) {
    float powderStrength = m_powderStrength * getCriticalVelocity(step); // 粉末强度
    float minWeight = 1.0f - Settings.particleStride; // 最小接触权重阈值
    // 1. 粒子-刚体接触的粉末效应
    for (int k = 0; k < m_bodyContactCount; k++) {
      final ParticleBodyContact contact = m_bodyContactBuffer[k];
      int a = contact.index;
      if ((m_flagsBuffer.data[a] & ParticleType.b2_powderParticle) != 0) {
        float w = contact.weight;
        if (w > minWeight) { // 如果接触权重超过阈值
          Body b = contact.body;
          float m = contact.mass;
          Vec2 p = m_positionBuffer.data[a];
          Vec2 n = contact.normal;
          final Vec2 f = tempVec;
          final Vec2 va = m_velocityBuffer.data[a];
          final float inter = powderStrength * m * (w - minWeight); // 排斥力强度
          final float pInvMass = getParticleInvMass();
          f.x = inter * n.x;
          f.y = inter * n.y; // 排斥力
          va.x -= pInvMass * f.x; // 粒子受力反向
          va.y -= pInvMass * f.y;
          b.applyLinearImpulse(f, p, true); // 刚体受力
        }
      }
    }
    // 2. 粒子-粒子接触的粉末效应
    for (int k = 0; k < m_contactCount; k++) {
      final ParticleContact contact = m_contactBuffer[k];
      if ((contact.flags & ParticleType.b2_powderParticle) != 0) {
        float w = contact.weight;
        if (w > minWeight) { // 如果接触权重超过阈值
          int a = contact.indexA;
          int b = contact.indexB;
          Vec2 n = contact.normal;
          final Vec2 va = m_velocityBuffer.data[a];
          final Vec2 vb = m_velocityBuffer.data[b];
          final float inter = powderStrength * (w - minWeight); // 排斥力强度
          final float fx = inter * n.x;
          final float fy = inter * n.y;
          va.x -= fx; // 粒子A受力反向
          va.y -= fy;
          vb.x += fx; // 粒子B受力正向
          vb.y += fy;
        }
      }
    }
  }

  /**
   * 解决固体粒子组（b2_solidParticleGroup）的效应。
   * 固体粒子组中的粒子会根据其深度产生额外的排斥力，模拟固体间的挤压。
   * @param step 时间步长
   */
  void solveSolid(final TimeStep step) {
    m_depthBuffer = requestParticleBuffer(m_depthBuffer); // 确保深度缓冲区存在
    float ejectionStrength = step.inv_dt * m_ejectionStrength; // 喷射强度
    for (int k = 0; k < m_contactCount; k++) {
      final ParticleContact contact = m_contactBuffer[k];
      int a = contact.indexA;
      int b = contact.indexB;
      // 只有当两个粒子属于不同的组时才应用此力
      if (m_groupBuffer[a] != m_groupBuffer[b]) {
        float w = contact.weight;
        Vec2 n = contact.normal;
        float h = m_depthBuffer[a] + m_depthBuffer[b]; // 两个粒子的深度和
        final Vec2 va = m_velocityBuffer.data[a];
        final Vec2 vb = m_velocityBuffer.data[b];
        final float inter = ejectionStrength * h * w; // 排斥力强度
        final float fx = inter * n.x;
        final float fy = inter * n.y;
        va.x -= fx; // 粒子A受力反向
        va.y -= fy;
        vb.x += fx; // 粒子B受力正向
        vb.y += fy;
      }
    }
  }

  /**
   * 解决颜色混合粒子（b2_colorMixingParticle）的效应。
   * 接触的粒子会根据颜色混合强度互相混合颜色。
   * @param step 时间步长
   */
  void solveColorMixing(final TimeStep step) {
    m_colorBuffer.data = requestParticleBuffer(ParticleColor.class, m_colorBuffer.data); // 确保颜色缓冲区存在
    int colorMixing256 = (int) (256 * m_colorMixingStrength); // 颜色混合强度 (0-256)
    for (int k = 0; k < m_contactCount; k++) {
      final ParticleContact contact = m_contactBuffer[k];
      int a = contact.indexA;
      int b = contact.indexB;
      // 只有当两个粒子都是颜色混合粒子时才进行混合
      if ((m_flagsBuffer.data[a] & m_flagsBuffer.data[b] & ParticleType.b2_colorMixingParticle) != 0) {
        ParticleColor colorA = m_colorBuffer.data[a];
        ParticleColor colorB = m_colorBuffer.data[b];
        // 计算颜色差异并按强度混合
        int dr = (colorMixing256 * (colorB.r - colorA.r)) >> 8; // (>> 8 相当于 / 256)
        int dg = (colorMixing256 * (colorB.g - colorA.g)) >> 8;
        int db = (colorMixing256 * (colorB.b - colorA.b)) >> 8;
        int da = (colorMixing256 * (colorB.a - colorA.a)) >> 8;
        colorA.r += dr; // 粒子A颜色更新
        colorA.g += dg;
        colorA.b += db;
        colorA.a += da;
        colorB.r -= dr; // 粒子B颜色更新 (反向)
        colorB.g -= dg;
        colorB.b -= db;
        colorB.a -= da;
      }
    }
  }

  /**
   * 清理所有带有僵尸标志（b2_zombieParticle）的粒子。
   * 僵尸粒子会被从缓冲区中移除，其他粒子的索引会进行调整。
   */
  void solveZombie() {
    int newCount = 0;
    int[] newIndices = new int[m_count]; // 用于存储旧索引到新索引的映射
    // 1. 标记要移除的粒子，并构建新索引映射
    for (int i = 0; i < m_count; i++) {
      int flags = m_flagsBuffer.data[i];
      if ((flags & ParticleType.b2_zombieParticle) != 0) {
        ParticleDestructionListener destructionListener = m_world.getParticleDestructionListener();
        if ((flags & ParticleType.b2_destructionListener) != 0 && destructionListener != null) {
          destructionListener.sayGoodbye(i); // 调用销毁监听器
        }
        newIndices[i] = Settings.invalidParticleIndex; // 标记为无效索引
      } else {
        newIndices[i] = newCount; // 分配新索引
        if (i != newCount) { // 如果粒子不是在原地，则移动数据
          m_flagsBuffer.data[newCount] = m_flagsBuffer.data[i];
          m_positionBuffer.data[newCount].set(m_positionBuffer.data[i]);
          m_velocityBuffer.data[newCount].set(m_velocityBuffer.data[i]);
          m_groupBuffer[newCount] = m_groupBuffer[i];
          if (m_depthBuffer != null) {
            m_depthBuffer[newCount] = m_depthBuffer[i];
          }
          if (m_colorBuffer.data != null) {
            m_colorBuffer.data[newCount].set(m_colorBuffer.data[i]);
          }
          if (m_userDataBuffer.data != null) {
            m_userDataBuffer.data[newCount] = m_userDataBuffer.data[i];
          }
        }
        newCount++; // 增加新粒子计数
      }
    }

    // 2. 更新代理索引并移除无效代理
    for (int k = 0; k < m_proxyCount; k++) {
      Proxy proxy = m_proxyBuffer[k];
      proxy.index = newIndices[proxy.index];
    }
    // 使用快速移除/交换法删除无效代理
    int j = m_proxyCount;
    for (int i = 0; i < j; i++) {
      if (Test.IsProxyInvalid(m_proxyBuffer[i])) {
        --j;
        Proxy temp = m_proxyBuffer[j];
        m_proxyBuffer[j] = m_proxyBuffer[i];
        m_proxyBuffer[i] = temp;
        --i;
      }
    }
    m_proxyCount = j;

    // 3. 更新接触索引并移除无效接触
    for (int k = 0; k < m_contactCount; k++) {
      ParticleContact contact = m_contactBuffer[k];
      contact.indexA = newIndices[contact.indexA];
      contact.indexB = newIndices[contact.indexB];
    }
    j = m_contactCount;
    for (int i = 0; i < j; i++) {
      if (Test.IsContactInvalid(m_contactBuffer[i])) {
        --j;
        ParticleContact temp = m_contactBuffer[j];
        m_contactBuffer[j] = m_contactBuffer[i];
        m_contactBuffer[i] = temp;
        --i;
      }
    }
    m_contactCount = j;

    // 4. 更新粒子-刚体接触索引并移除无效接触
    for (int k = 0; k < m_bodyContactCount; k++) {
      ParticleBodyContact contact = m_bodyContactBuffer[k];
      contact.index = newIndices[contact.index];
    }
    j = m_bodyContactCount;
    for (int i = 0; i < j; i++) {
      if (Test.IsBodyContactInvalid(m_bodyContactBuffer[i])) {
        --j;
        ParticleBodyContact temp = m_bodyContactBuffer[j];
        m_bodyContactBuffer[j] = m_bodyContactBuffer[i];
        m_bodyContactBuffer[i] = temp;
        --i;
      }
    }
    m_bodyContactCount = j;

    // 5. 更新粒子对索引并移除无效粒子对
    for (int k = 0; k < m_pairCount; k++) {
      Pair pair = m_pairBuffer[k];
      pair.indexA = newIndices[pair.indexA];
      pair.indexB = newIndices[pair.indexB];
    }
    j = m_pairCount;
    for (int i = 0; i < j; i++) {
      if (Test.IsPairInvalid(m_pairBuffer[i])) {
        --j;
        Pair temp = m_pairBuffer[j];
        m_pairBuffer[j] = m_pairBuffer[i];
        m_pairBuffer[i] = temp;
        --i;
      }
    }
    m_pairCount = j;

    // 6. 更新三元组索引并移除无效三元组
    for (int k = 0; k < m_triadCount; k++) {
      Triad triad = m_triadBuffer[k];
      triad.indexA = newIndices[triad.indexA];
      triad.indexB = newIndices[triad.indexB];
      triad.indexC = newIndices[triad.indexC];
    }
    j = m_triadCount;
    for (int i = 0; i < j; i++) {
      if (Test.IsTriadInvalid(m_triadBuffer[i])) {
        --j;
        Triad temp = m_triadBuffer[j];
        m_triadBuffer[j] = m_triadBuffer[i];
        m_triadBuffer[i] = temp;
        --i;
      }
    }
    m_triadCount = j;

    // 7. 更新粒子组的索引和状态
    for (ParticleGroup group = m_groupList; group != null; group = group.getNext()) {
      int firstIndex = newCount; // 理论上的新起始索引
      int lastIndex = 0; // 理论上的新结束索引
      boolean modified = false; // 组是否被修改（有粒子被移除）
      for (int i = group.m_firstIndex; i < group.m_lastIndex; i++) {
        j = newIndices[i];
        if (j >= 0) { // 如果粒子仍然有效
          firstIndex = MathUtils.min(firstIndex, j);
          lastIndex = MathUtils.max(lastIndex, j + 1);
        } else { // 粒子被移除
          modified = true;
        }
      }
      if (firstIndex < lastIndex) { // 组内仍有有效粒子
        group.m_firstIndex = firstIndex;
        group.m_lastIndex = lastIndex;
        if (modified) {
          if ((group.m_groupFlags & ParticleGroupType.b2_rigidParticleGroup) != 0) {
            group.m_toBeSplit = true; // 刚体组有粒子被移除可能需要分裂
          }
        }
      } else { // 组内所有粒子都被移除
        group.m_firstIndex = 0;
        group.m_lastIndex = 0;
        if (group.m_destroyAutomatically) {
          group.m_toBeDestroyed = true; // 自动销毁的组标记为待销毁
        }
      }
    }

    m_count = newCount; // 更新粒子总数
    // m_world.m_stackAllocator.Free(newIndices); // 释放临时数组 (Java GC会自动处理)

    // 8. 销毁或分裂标记的组
    for (ParticleGroup group = m_groupList; group != null;) {
      ParticleGroup next = group.getNext();
      if (group.m_toBeDestroyed) {
        destroyParticleGroup(group); // 销毁组
      } else if (group.m_toBeSplit) {
        // TODO: 分裂组 (目前JBox2D没有实现)
      }
      group = next;
    }
  }

  /**
   * 辅助类，用于 RotateBuffer 方法计算新的粒子索引。
   */
  private static class NewIndices {
    int start, mid, end;

    /**
     * 根据缓冲区旋转的参数，获取旧索引对应的新索引。
     * @param i 旧索引
     * @return 新索引
     */
    final int getIndex(final int i) {
      if (i < start) {
        return i; // 未受影响的部分
      } else if (i < mid) {
        return i + end - mid; // 从 [start, mid) 移动到 [end - (mid - start), end)
      } else if (i < end) {
        return i + start - mid; // 从 [mid, end) 移动到 [start, start + (end - mid))
      } else {
        return i; // 未受影响的部分
      }
    }
  }

  private final NewIndices newIndices = new NewIndices(); // NewIndices实例

  /**
   * 旋转粒子缓冲区，将 [start, mid) 区间的粒子移到 [end - (mid - start), end)
   * 将 [mid, end) 区间的粒子移到 [start, start + (end - mid))
   * 用于合并粒子组时，将特定组的粒子数据集中。
   * @param start 旋转区间的起始索引
   * @param mid 旋转区间的中间索引
   * @param end 旋转区间的结束索引
   */
  void RotateBuffer(int start, int mid, int end) {
    if (start == mid || mid == end) {
      return; // 无需旋转
    }
    newIndices.start = start;
    newIndices.mid = mid;
    newIndices.end = end;

    // 旋转所有相关的粒子数据缓冲区
    BufferUtils.rotate(m_flagsBuffer.data, start, mid, end);
    BufferUtils.rotate(m_positionBuffer.data, start, mid, end);
    BufferUtils.rotate(m_velocityBuffer.data, start, mid, end);
    BufferUtils.rotate(m_groupBuffer, start, mid, end);
    if (m_depthBuffer != null) {
      BufferUtils.rotate(m_depthBuffer, start, mid, end);
    }
    if (m_colorBuffer.data != null) {
      BufferUtils.rotate(m_colorBuffer.data, start, mid, end);
    }
    if (m_userDataBuffer.data != null) {
      BufferUtils.rotate(m_userDataBuffer.data, start, mid, end);
    }

    // 更新所有内部数据结构中存储的粒子索引
    for (int k = 0; k < m_proxyCount; k++) {
      Proxy proxy = m_proxyBuffer[k];
      proxy.index = newIndices.getIndex(proxy.index);
    }

    for (int k = 0; k < m_contactCount; k++) {
      ParticleContact contact = m_contactBuffer[k];
      contact.indexA = newIndices.getIndex(contact.indexA);
      contact.indexB = newIndices.getIndex(contact.indexB);
    }

    for (int k = 0; k < m_bodyContactCount; k++) {
      ParticleBodyContact contact = m_bodyContactBuffer[k];
      contact.index = newIndices.getIndex(contact.index);
    }

    for (int k = 0; k < m_pairCount; k++) {
      Pair pair = m_pairBuffer[k];
      pair.indexA = newIndices.getIndex(pair.indexA);
      pair.indexB = newIndices.getIndex(pair.indexB);
    }

    for (int k = 0; k < m_triadCount; k++) {
      Triad triad = m_triadBuffer[k];
      triad.indexA = newIndices.getIndex(triad.indexA);
      triad.indexB = newIndices.getIndex(triad.indexB);
      triad.indexC = newIndices.getIndex(triad.indexC);
    }

    // 更新粒子组的起始和结束索引
    for (ParticleGroup group = m_groupList; group != null; group = group.getNext()) {
      group.m_firstIndex = newIndices.getIndex(group.m_firstIndex);
      group.m_lastIndex = newIndices.getIndex(group.m_lastIndex - 1) + 1; // 结束索引需要特殊处理
    }
  }

  /**
   * 设置粒子半径。
   * 会同时更新粒子直径、直径平方和直径倒数。
   * @param radius 粒子半径
   */
  public void setParticleRadius(float radius) {
    m_particleDiameter = 2 * radius;
    m_squaredDiameter = m_particleDiameter * m_particleDiameter;
    m_inverseDiameter = 1 / m_particleDiameter;
  }

  /**
   * 设置粒子密度。
   * @param density 粒子密度
   */
  public void setParticleDensity(float density) {
    m_density = density;
    m_inverseDensity = 1 / m_density;
  }

  /**
   * 获取粒子密度。
   * @return 粒子密度
   */
  public float getParticleDensity() {
    return m_density;
  }

  /**
   * 设置粒子重力缩放因子。
   * @param gravityScale 重力缩放因子
   */
  public void setParticleGravityScale(float gravityScale) {
    m_gravityScale = gravityScale;
  }

  /**
   * 获取粒子重力缩放因子。
   * @return 重力缩放因子
   */
  public float getParticleGravityScale() {
    return m_gravityScale;
  }

  /**
   * 设置粒子阻尼强度。
   * @param damping 阻尼强度
   */
  public void setParticleDamping(float damping) {
    m_dampingStrength = damping;
  }

  /**
   * 获取粒子阻尼强度。
   * @return 阻尼强度
   */
  public float getParticleDamping() {
    return m_dampingStrength;
  }

  /**
   * 获取粒子半径。
   * @return 粒子半径
   */
  public float getParticleRadius() {
    return m_particleDiameter / 2;
  }

  /**
   * 获取临界速度。
   * 粒子速度不应超过此值，以保持模拟稳定性。
   * @param step 时间步长
   * @return 临界速度
   */
  float getCriticalVelocity(final TimeStep step) {
    return m_particleDiameter * step.inv_dt;
  }

  /**
   * 获取临界速度的平方。
   * @param step 时间步长
   * @return 临界速度的平方
   */
  float getCriticalVelocitySquared(final TimeStep step) {
    float velocity = getCriticalVelocity(step);
    return velocity * velocity;
  }

  /**
   * 获取临界压力。
   * @param step 时间步长
   * @return 临界压力
   */
  float getCriticalPressure(final TimeStep step) {
    return m_density * getCriticalVelocitySquared(step);
  }

  /**
   * 获取粒子创建的步长（间距）。
   * @return 粒子步长
   */
  float getParticleStride() {
    return Settings.particleStride * m_particleDiameter;
  }

  /**
   * 获取单个粒子的质量。
   * @return 粒子质量
   */
  float getParticleMass() {
    float stride = getParticleStride();
    return m_density * stride * stride;
  }

  /**
   * 获取单个粒子的质量倒数。
   * @return 粒子质量倒数
   */
  float getParticleInvMass() {
    // 经验系数，用于调整质量，确保稳定性
    return 1.777777f * m_inverseDensity * m_inverseDiameter * m_inverseDiameter;
  }

  /**
   * 获取粒子标志位缓冲区。
   * @return 粒子标志位数组
   */
  public int[] getParticleFlagsBuffer() {
    return m_flagsBuffer.data;
  }

  /**
   * 获取粒子位置缓冲区。
   * @return 粒子位置Vec2数组
   */
  public Vec2[] getParticlePositionBuffer() {
    return m_positionBuffer.data;
  }

  /**
   * 获取粒子速度缓冲区。
   * @return 粒子速度Vec2数组
   */
  public Vec2[] getParticleVelocityBuffer() {
    return m_velocityBuffer.data;
  }

  /**
   * 获取粒子颜色缓冲区。
   * 如果缓冲区尚未创建，会自动创建。
   * @return 粒子颜色ParticleColor数组
   */
  public ParticleColor[] getParticleColorBuffer() {
    m_colorBuffer.data = requestParticleBuffer(ParticleColor.class, m_colorBuffer.data);
    return m_colorBuffer.data;
  }

  /**
   * 获取粒子用户数据缓冲区。
   * 如果缓冲区尚未创建，会自动创建。
   * @return 粒子用户数据Object数组
   */
  public Object[] getParticleUserDataBuffer() {
    m_userDataBuffer.data = requestParticleBuffer(Object.class, m_userDataBuffer.data);
    return m_userDataBuffer.data;
  }

  /**
   * 获取粒子最大数量限制。
   * @return 粒子最大数量
   */
  public int getParticleMaxCount() {
    return m_maxCount;
  }

  /**
   * 设置粒子最大数量限制。
   * @param count 粒子最大数量
   */
  public void setParticleMaxCount(int count) {
    assert (m_count <= count); // 确保当前粒子数量不超过新设置的最大数量
    m_maxCount = count;
  }

  /**
   * 内部方法：设置粒子缓冲区。
   * 用于将用户提供的缓冲区与内部系统连接。
   * @param buffer ParticleBufferInt对象
   * @param newData 新的数据数组
   * @param newCapacity 新的容量
   */
  void setParticleBuffer(ParticleBufferInt buffer, int[] newData, int newCapacity) {
    assert ((newData != null && newCapacity != 0) || (newData == null && newCapacity == 0));
    if (buffer.userSuppliedCapacity != 0) {
      // 如果之前是用户提供的缓冲区，需要释放 (Java中由GC处理，这里是注释)
      // m_world.m_blockAllocator.Free(buffer.data, sizeof(T) * m_internalAllocatedCapacity);
    }
    buffer.data = newData;
    buffer.userSuppliedCapacity = newCapacity;
  }

  /**
   * 内部方法：设置粒子缓冲区 (泛型版本)。
   * @param buffer ParticleBuffer对象
   * @param newData 新的数据数组
   * @param newCapacity 新的容量
   * @param <T> 缓冲区中存储的数据类型
   */
  <T> void setParticleBuffer(ParticleBuffer<T> buffer, T[] newData, int newCapacity) {
    assert ((newData != null && newCapacity != 0) || (newData == null && newCapacity == 0));
    if (buffer.userSuppliedCapacity != 0) {
      // 如果之前是用户提供的缓冲区，需要释放
      // m_world.m_blockAllocator.Free(buffer.data, sizeof(T) * m_internalAllocatedCapacity);
    }
    buffer.data = newData;
    buffer.userSuppliedCapacity = newCapacity;
  }

  /**
   * 设置粒子标志位缓冲区为用户提供的数据。
   * @param buffer 用户提供的标志位数组
   * @param capacity 缓冲区的容量
   */
  public void setParticleFlagsBuffer(int[] buffer, int capacity) {
    setParticleBuffer(m_flagsBuffer, buffer, capacity);
  }

  /**
   * 设置粒子位置缓冲区为用户提供的数据。
   * @param buffer 用户提供的位置Vec2数组
   * @param capacity 缓冲区的容量
   */
  public void setParticlePositionBuffer(Vec2[] buffer, int capacity) {
    setParticleBuffer(m_positionBuffer, buffer, capacity);
  }

  /**
   * 设置粒子速度缓冲区为用户提供的数据。
   * @param buffer 用户提供的速度Vec2数组
   * @param capacity 缓冲区的容量
   */
  public void setParticleVelocityBuffer(Vec2[] buffer, int capacity) {
    setParticleBuffer(m_velocityBuffer, buffer, capacity);
  }

  /**
   * 设置粒子颜色缓冲区为用户提供的数据。
   * @param buffer 用户提供的颜色ParticleColor数组
   * @param capacity 缓冲区的容量
   */
  public void setParticleColorBuffer(ParticleColor[] buffer, int capacity) {
    setParticleBuffer(m_colorBuffer, buffer, capacity);
  }

  /**
   * 获取粒子所属组的缓冲区（指向ParticleGroup对象的数组）。
   * @return 粒子组数组
   */
  public ParticleGroup[] getParticleGroupBuffer() {
    return m_groupBuffer;
  }

  /**
   * 获取当前粒子组的数量。
   * @return 粒子组数量
   */
  public int getParticleGroupCount() {
    return m_groupCount;
  }

  /**
   * 获取粒子组链表头。
   * @return ParticleGroup数组（实际上是链表头，命名可能引起误解）
   */
  public ParticleGroup[] getParticleGroupList() {
    return m_groupBuffer; // 注意：这个方法名可能有点误导，它返回的是一个数组，但实际粒子组管理是通过 m_groupList 链表。
  }

  /**
   * 获取当前粒子数量。
   * @return 粒子数量
   */
  public int getParticleCount() {
    return m_count;
  }

  /**
   * 设置粒子用户数据缓冲区为用户提供的数据。
   * @param buffer 用户提供的用户数据Object数组
   * @param capacity 缓冲区的容量
   */
  public void setParticleUserDataBuffer(Object[] buffer, int capacity) {
    setParticleBuffer(m_userDataBuffer, buffer, capacity);
  }

  /**
   * 在代理缓冲区中查找第一个标签大于或等于给定标签的代理的索引 (二分查找)。
   * @param ray 代理数组
   * @param length 数组长度
   * @param tag 目标标签
   * @return 第一个大于或等于tag的代理索引
   */
  private static final int lowerBound(Proxy[] ray, int length, long tag) {
    int left = 0;
    int step, curr;
    while (length > 0) {
      step = length / 2;
      curr = left + step;
      if (ray[curr].tag < tag) {
        left = curr + 1;
        length -= step + 1;
      } else {
        length = step;
      }
    }
    return left;
  }

  /**
   * 在代理缓冲区中查找第一个标签大于给定标签的代理的索引 (二分查找)。
   * @param ray 代理数组
   * @param length 数组长度
   * @param tag 目标标签
   * @return 第一个大于tag的代理索引
   */
  private static final int upperBound(Proxy[] ray, int length, long tag) {
    int left = 0;
    int step, curr;
    while (length > 0) {
      step = length / 2;
      curr = left + step;
      if (ray[curr].tag <= tag) {
        left = curr + 1;
        length -= step + 1;
      } else {
        length = step;
      }
    }
    return left;
  }

  /**
   * 查询指定AABB（轴对齐包围盒）内的所有粒子。
   * @param callback 回调接口，用于报告查询到的粒子
   * @param aabb 查询的AABB
   */
  public void queryAABB(ParticleQueryCallback callback, final AABB aabb) {
    if (m_proxyCount == 0) {
      return;
    }

    final float lowerBoundX = aabb.lowerBound.x;
    final float lowerBoundY = aabb.lowerBound.y;
    final float upperBoundX = aabb.upperBound.x;
    final float upperBoundY = aabb.upperBound.y;
    // 根据AABB的边界计算空间哈希标签范围，进行初步筛选
    int firstProxy =
        lowerBound(m_proxyBuffer, m_proxyCount,
            computeTag(m_inverseDiameter * lowerBoundX, m_inverseDiameter * lowerBoundY));
    int lastProxy =
        upperBound(m_proxyBuffer, m_proxyCount,
            computeTag(m_inverseDiameter * upperBoundX, m_inverseDiameter * upperBoundY));
    // 遍历筛选出的代理，进行精确的AABB检测
    for (int proxy = firstProxy; proxy < lastProxy; ++proxy) {
      int i = m_proxyBuffer[proxy].index;
      final Vec2 p = m_positionBuffer.data[i];
      if (lowerBoundX < p.x && p.x < upperBoundX && lowerBoundY < p.y && p.y < upperBoundY) {
        if (!callback.reportParticle(i)) { // 如果回调返回false，则停止查询
          break;
        }
      }
    }
  }

  /**
   * 对粒子系统进行射线投射查询。
   * @param callback 回调接口，用于报告射线击中的粒子
   * @param point1 射线的起点
   * @param point2 射线的终点
   */
  public void raycast(ParticleRaycastCallback callback, final Vec2 point1, final Vec2 point2) {
    if (m_proxyCount == 0) {
      return;
    }
    // 根据射线起点和终点的包围盒计算空间哈希标签范围，进行初步筛选
    int firstProxy =
        lowerBound(
            m_proxyBuffer,
            m_proxyCount,
            computeTag(m_inverseDiameter * MathUtils.min(point1.x, point2.x) - 1, m_inverseDiameter
                * MathUtils.min(point1.y, point2.y) - 1));
    int lastProxy =
        upperBound(
            m_proxyBuffer,
            m_proxyCount,
            computeTag(m_inverseDiameter * MathUtils.max(point1.x, point2.x) + 1, m_inverseDiameter
                * MathUtils.max(point1.y, point2.y) + 1));
    float fraction = 1; // 击中最近粒子的分数 (0到1)
    // 求解二次方程：((1-t)*point1+t*point2-position)^2 = diameter^2
    // t是潜在的分数
    final float vx = point2.x - point1.x;
    final float vy = point2.y - point1.y;
    float v2 = vx * vx + vy * vy; // 射线方向向量长度平方
    if (v2 == 0) v2 = Float.MAX_VALUE; // 避免除以零
    for (int proxy = firstProxy; proxy < lastProxy; ++proxy) {
      int i = m_proxyBuffer[proxy].index;
      final Vec2 posI = m_positionBuffer.data[i];
      final float px = point1.x - posI.x; // 粒子相对于射线起点的向量
      final float py = point1.y - posI.y;
      float pv = px * vx + py * vy; // 向量点积
      float p2 = px * px + py * py; // 向量长度平方
      float determinant = pv * pv - v2 * (p2 - m_squaredDiameter); // 判别式
      if (determinant >= 0) { // 如果有实数解 (射线与粒子相交)
        float sqrtDeterminant = MathUtils.sqrt(determinant);
        // 查找在 [0, fraction] 范围内的解
        float t = (-pv - sqrtDeterminant) / v2;
        if (t > fraction) { // 超出当前最近击中点
          continue;
        }
        if (t < 0) { // 第一个解在射线起点之前
          t = (-pv + sqrtDeterminant) / v2; // 尝试第二个解
          if (t < 0 || t > fraction) { // 第二个解仍在射线起点之前或超出当前最近击中点
            continue;
          }
        }
        final Vec2 n = tempVec;
        tempVec.x = px + t * vx; // 碰撞点到粒子中心的向量
        tempVec.y = py + t * vy;
        n.normalize(); // 碰撞法线
        final Vec2 point = tempVec2;
        point.x = point1.x + t * vx; // 实际碰撞点
        point.y = point1.y + t * vy;
        float f = callback.reportParticle(i, point, n, t); // 调用回调函数
        fraction = MathUtils.min(fraction, f); // 更新最近击中分数
        if (fraction <= 0) { // 如果回调返回0或更小，表示停止进一步查询
          break;
        }
      }
    }
  }

  /**
   * 计算粒子碰撞能量。
   * 衡量粒子间由于碰撞而损失的能量。
   * @return 碰撞能量
   */
  public float computeParticleCollisionEnergy() {
    float sum_v2 = 0;
    for (int k = 0; k < m_contactCount; k++) {
      final ParticleContact contact = m_contactBuffer[k];
      int a = contact.indexA;
      int b = contact.indexB;
      Vec2 n = contact.normal;
      final Vec2 va = m_velocityBuffer.data[a];
      final Vec2 vb = m_velocityBuffer.data[b];
      final float vx = vb.x - va.x;
      final float vy = vb.y - va.y;
      float vn = vx * n.x + vy * n.y; // 粒子间法向相对速度
      if (vn < 0) { // 如果正在接近
        sum_v2 += vn * vn; // 累积速度平方
      }
    }
    return 0.5f * getParticleMass() * sum_v2; // 0.5 * m * v^2
  }

  /**
   * 内部方法：重新分配泛型缓冲区。
   * @param buffer 粒子缓冲区对象
   * @param oldCapacity 旧容量
   * @param newCapacity 新容量
   * @param deferred 是否延迟分配（目前JBox2D中此参数通常为false）
   * @param <T> 缓冲区中存储的数据类型
   * @return 重新分配后的数据数组
   */
  static <T> T[] reallocateBuffer(ParticleBuffer<T> buffer, int oldCapacity, int newCapacity,
      boolean deferred) {
    assert (newCapacity > oldCapacity);
    return BufferUtils.reallocateBuffer(buffer.dataClass, buffer.data, buffer.userSuppliedCapacity,
        oldCapacity, newCapacity, deferred);
  }

  /**
   * 内部方法：重新分配整数缓冲区。
   * @param buffer 粒子整数缓冲区对象
   * @param oldCapacity 旧容量
   * @param newCapacity 新容量
   * @param deferred 是否延迟分配
   * @return 重新分配后的数据数组
   */
  static int[] reallocateBuffer(ParticleBufferInt buffer, int oldCapacity, int newCapacity,
      boolean deferred) {
    assert (newCapacity > oldCapacity);
    return BufferUtils.reallocateBuffer(buffer.data, buffer.userSuppliedCapacity, oldCapacity,
        newCapacity, deferred);
  }

  /**
   * 内部方法：请求并初始化泛型粒子缓冲区。
   * 如果缓冲区为 null，则根据当前内部容量创建并初始化数组元素。
   * @param klass 元素的Class对象
   * @param buffer 现有缓冲区数组
   * @param <T> 缓冲区中存储的数据类型
   * @return 已准备好的缓冲区数组
   */
  @SuppressWarnings("unchecked")
  <T> T[] requestParticleBuffer(Class<T> klass, T[] buffer) {
    if (buffer == null) {
      buffer = (T[]) Array.newInstance(klass, m_internalAllocatedCapacity);
      for (int i = 0; i < m_internalAllocatedCapacity; i++) {
        try {
          buffer[i] = klass.newInstance(); // 通过反射创建新实例
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
    return buffer;
  }

  /**
   * 内部方法：请求并初始化浮点数粒子缓冲区。
   * 如果缓冲区为 null，则根据当前内部容量创建新数组。
   * @param buffer 现有缓冲区数组
   * @return 已准备好的缓冲区数组
   */
  float[] requestParticleBuffer(float[] buffer) {
    if (buffer == null) {
      buffer = new float[m_internalAllocatedCapacity];
    }
    return buffer;
  }

  /**
   * 泛型粒子缓冲区封装类。
   * @param <T> 缓冲区中存储的数据类型
   */
  public static class ParticleBuffer<T> {
    public T[] data; // 实际存储数据的数组
    final Class<T> dataClass; // 数据类型Class对象，用于反射创建实例
    int userSuppliedCapacity; // 用户提供的容量 (如果用户自己提供了缓冲区)

    public ParticleBuffer(Class<T> dataClass) {
      this.dataClass = dataClass;
    }
  }

  /**
   * 整数粒子缓冲区封装类。
   */
  static class ParticleBufferInt {
    int[] data; // 实际存储数据的数组
    int userSuppliedCapacity; // 用户提供的容量
  }

  /**
   * 用于检测粒子接触的代理对象。
   * 包含粒子索引和空间哈希标签，支持排序。
   */
  public static class Proxy implements Comparable<Proxy> {
    int index; // 粒子索引
    long tag; // 空间哈希标签

    @Override
    public int compareTo(Proxy o) {
      return (tag - o.tag) < 0 ? -1 : (o.tag == tag ? 0 : 1); // 按照tag排序
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Proxy other = (Proxy) obj;
      if (tag != other.tag) return false;
      return true;
    }
  }

  /**
   * 两个粒子之间的连接，用于弹簧等效果。
   */
  public static class Pair {
    int indexA, indexB; // 两个粒子的索引
    int flags; // 粒子对的标志
    float strength; // 连接强度
    float distance; // 原始连接距离
  }

  /**
   * 三个粒子之间的连接，用于弹性等效果。
   */
  public static class Triad {
    int indexA, indexB, indexC; // 三个粒子的索引
    int flags; // 三元组的标志
    float strength; // 连接强度
    final Vec2 pa = new Vec2(), pb = new Vec2(), pc = new Vec2(); // 原始相对位置
    float ka, kb, kc, s; // 内部计算参数
  }

  /**
   * 创建粒子组时使用的 VoronoiDiagram 回调。
   * 用于根据 Voronoi 图创建弹性粒子的三元组。
   */
  static class CreateParticleGroupCallback implements VoronoiDiagramCallback {
    public void callback(int a, int b, int c) {
      final Vec2 pa = system.m_positionBuffer.data[a];
      final Vec2 pb = system.m_positionBuffer.data[b];
      final Vec2 pc = system.m_positionBuffer.data[c];
      // 计算粒子间的距离平方
      final float dabx = pa.x - pb.x;
      final float daby = pa.y - pb.y;
      final float dbcx = pb.x - pc.x;
      final float dbcy = pb.y - pc.y;
      final float dcax = pc.x - pa.x;
      final float dcay = pc.y - pa.y;
      // 如果粒子间的距离在最大三元组距离之内
      float maxDistanceSquared = Settings.maxTriadDistanceSquared * system.m_squaredDiameter;
      if (dabx * dabx + daby * daby < maxDistanceSquared
          && dbcx * dbcx + dbcy * dbcy < maxDistanceSquared
          && dcax * dcax + dcay * dcay < maxDistanceSquared) {
        // 重新分配三元组缓冲区（如果需要）
        if (system.m_triadCount >= system.m_triadCapacity) {
          int oldCapacity = system.m_triadCapacity;
          int newCapacity =
              system.m_triadCount != 0
              ? 2 * system.m_triadCount
              : Settings.minParticleBufferCapacity;
          system.m_triadBuffer =
              BufferUtils.reallocateBuffer(Triad.class, system.m_triadBuffer, oldCapacity,
                  newCapacity);
          system.m_triadCapacity = newCapacity;
        }
        Triad triad = system.m_triadBuffer[system.m_triadCount]; // 创建新三元组
        triad.indexA = a;
        triad.indexB = b;
        triad.indexC = c;
        // 三元组标志是三个粒子标志的或运算
        triad.flags =
            system.m_flagsBuffer.data[a] | system.m_flagsBuffer.data[b]
                | system.m_flagsBuffer.data[c];
        triad.strength = def.strength; // 强度来自粒子组定义
        final float midPointx = (float) 1 / 3 * (pa.x + pb.x + pc.x); // 三元组质心
        final float midPointy = (float) 1 / 3 * (pa.y + pb.y + pc.y);
        // 存储粒子相对于质心的原始相对位置
        triad.pa.x = pa.x - midPointx;
        triad.pa.y = pa.y - midPointy;
        triad.pb.x = pb.x - midPointx;
        triad.pb.y = pb.y - midPointy;
        triad.pc.x = pc.x - midPointx;
        triad.pc.y = pc.y - midPointy;
        // 内部计算参数
        triad.ka = -(dcax * dabx + dcay * daby);
        triad.kb = -(dabx * dbcx + daby * dbcy);
        triad.kc = -(dbcx * dcax + dbcy * dcay);
        triad.s = Vec2.cross(pa, pb) + Vec2.cross(pb, pc) + Vec2.cross(pc, pa);
        system.m_triadCount++; // 增加三元组计数
      }
    }

    ParticleSystem system; // 粒子系统引用
    ParticleGroupDef def; // 粒子组定义引用
    int firstIndex; // 粒子组的起始索引
  }

  /**
   * 合并粒子组时使用的 VoronoiDiagram 回调。
   * 用于在合并组之间创建弹性粒子的三元组。
   */
  static class JoinParticleGroupsCallback implements VoronoiDiagramCallback {
    public void callback(int a, int b, int c) {
      // 只有当三元组包含来自两个组的粒子时才创建
      int countA =
          ((a < groupB.m_firstIndex) ? 1 : 0) + ((b < groupB.m_firstIndex) ? 1 : 0)
              + ((c < groupB.m_firstIndex) ? 1 : 0);
      if (countA > 0 && countA < 3) { // 至少有一个来自groupA，至少有一个来自groupB
        int af = system.m_flagsBuffer.data[a];
        int bf = system.m_flagsBuffer.data[b];
        int cf = system.m_flagsBuffer.data[c];
        // 并且所有粒子都支持三元组特性
        if ((af & bf & cf & k_triadFlags) != 0) {
          final Vec2 pa = system.m_positionBuffer.data[a];
          final Vec2 pb = system.m_positionBuffer.data[b];
          final Vec2 pc = system.m_positionBuffer.data[c];
          final float dabx = pa.x - pb.x;
          final float daby = pa.y - pb.y;
          final float dbcx = pb.x - pc.x;
          final float dbcy = pb.y - pc.y;
          final float dcax = pc.x - pa.x;
          final float dcay = pc.y - pa.y;
          float maxDistanceSquared = Settings.maxTriadDistanceSquared * system.m_squaredDiameter;
          if (dabx * dabx + daby * daby < maxDistanceSquared
              && dbcx * dbcx + dbcy * dbcy < maxDistanceSquared
              && dcax * dcax + dcay * dcay < maxDistanceSquared) {
            if (system.m_triadCount >= system.m_triadCapacity) {
              int oldCapacity = system.m_triadCapacity;
              int newCapacity =
                  system.m_triadCount != 0
                  ? 2 * system.m_triadCount
                  : Settings.minParticleBufferCapacity;
              system.m_triadBuffer =
                  BufferUtils.reallocateBuffer(Triad.class, system.m_triadBuffer, oldCapacity,
                      newCapacity);
              system.m_triadCapacity = newCapacity;
            }
            Triad triad = system.m_triadBuffer[system.m_triadCount];
            triad.indexA = a;
            triad.indexB = b;
            triad.indexC = c;
            triad.flags = af | bf | cf;
            triad.strength = MathUtils.min(groupA.m_strength, groupB.m_strength); // 强度取最小值
            final float midPointx = (float) 1 / 3 * (pa.x + pb.x + pc.x);
            final float midPointy = (float) 1 / 3 * (pa.y + pb.y + pc.y);
            triad.pa.x = pa.x - midPointx;
            triad.pa.y = pa.y - midPointy;
            triad.pb.x = pb.x - midPointx;
            triad.pb.y = pb.y - midPointy;
            triad.pc.x = pc.x - midPointx;
            triad.pc.y = pc.y - midPointy;
            triad.ka = -(dcax * dabx + dcay * daby);
            triad.kb = -(dabx * dbcx + daby * dbcy);
            triad.kc = -(dbcx * dcax + dbcy * dcay);
            triad.s = Vec2.cross(pa, pb) + Vec2.cross(pb, pc) + Vec2.cross(pc, pa);
            system.m_triadCount++;
          }
        }
      }
    }

    ParticleSystem system; // 粒子系统引用
    ParticleGroup groupA; // 粒子组A引用
    ParticleGroup groupB; // 粒子组B引用
  };

  /**
   * 实现 ParticleQueryCallback 接口，用于在指定形状内销毁粒子。
   */
  static class DestroyParticlesInShapeCallback implements ParticleQueryCallback {
    ParticleSystem system;
    Shape shape;
    Transform xf;
    boolean callDestructionListener;
    int destroyed; // 计数被销毁的粒子数量

    public DestroyParticlesInShapeCallback() {
      // TODO Auto-generated constructor stub
    }

    /**
     * 初始化回调。
     * @param system 粒子系统
     * @param shape 形状
     * @param xf 形状的变换
     * @param callDestructionListener 是否调用销毁监听器
     */
    public void init(ParticleSystem system, Shape shape, Transform xf,
        boolean callDestructionListener) {
      this.system = system;
      this.shape = shape;
      this.xf = xf;
      this.destroyed = 0;
      this.callDestructionListener = callDestructionListener;
    }

    @Override
    public boolean reportParticle(int index) {
      assert (index >= 0 && index < system.m_count);
      if (shape.testPoint(xf, system.m_positionBuffer.data[index])) { // 如果粒子在形状内
        system.destroyParticle(index, callDestructionListener); // 销毁粒子
        destroyed++;
      }
      return true; // 继续查询
    }
  }

  /**
   * 实现 QueryCallback 接口，用于更新粒子与刚体之间的接触。
   */
  static class UpdateBodyContactsCallback implements QueryCallback {
    ParticleSystem system;

    private final Vec2 tempVec = new Vec2(); // 临时向量

    @Override
    public boolean reportFixture(Fixture fixture) {
      if (fixture.isSensor()) {
        return true; // 忽略传感器
      }
      final Shape shape = fixture.getShape();
      Body b = fixture.getBody();
      Vec2 bp = b.getWorldCenter(); // 刚体世界中心
      float bm = b.getMass();
      float bI = b.getInertia() - bm * b.getLocalCenter().lengthSquared(); // 刚体转动惯量
      float invBm = bm > 0 ? 1 / bm : 0; // 刚体质量倒数
      float invBI = bI > 0 ? 1 / bI : 0; // 刚体转动惯量倒数
      int childCount = shape.getChildCount();
      for (int childIndex = 0; childIndex < childCount; childIndex++) {
        AABB aabb = fixture.getAABB(childIndex); // 夹具子形状的AABB
        // 扩展AABB，考虑到粒子直径
        final float aabblowerBoundx = aabb.lowerBound.x - system.m_particleDiameter;
        final float aabblowerBoundy = aabb.lowerBound.y - system.m_particleDiameter;
        final float aabbupperBoundx = aabb.upperBound.x + system.m_particleDiameter;
        final float aabbupperBoundy = aabb.upperBound.y + system.m_particleDiameter;
        // 根据扩展AABB的标签范围筛选粒子代理
        int firstProxy =
            lowerBound(
                system.m_proxyBuffer,
                system.m_proxyCount,
                computeTag(system.m_inverseDiameter * aabblowerBoundx, system.m_inverseDiameter
                    * aabblowerBoundy));
        int lastProxy =
            upperBound(
                system.m_proxyBuffer,
                system.m_proxyCount,
                computeTag(system.m_inverseDiameter * aabbupperBoundx, system.m_inverseDiameter
                    * aabbupperBoundy));

        for (int proxy = firstProxy; proxy != lastProxy; ++proxy) {
          int a = system.m_proxyBuffer[proxy].index;
          Vec2 ap = system.m_positionBuffer.data[a];
          if (aabblowerBoundx <= ap.x && ap.x <= aabbupperBoundx && aabblowerBoundy <= ap.y
              && ap.y <= aabbupperBoundy) { // 如果粒子在扩展AABB内
            float d;
            final Vec2 n = tempVec;
            d = fixture.computeDistance(ap, childIndex, n); // 计算粒子到夹具的距离和法线
            if (d < system.m_particleDiameter) { // 如果粒子与夹具碰撞
              float invAm =
                  (system.m_flagsBuffer.data[a] & ParticleType.b2_wallParticle) != 0 ? 0 : system
                      .getParticleInvMass(); // 粒子质量倒数 (壁粒子为0)
              final float rpx = ap.x - bp.x;
              final float rpy = ap.y - bp.y;
              float rpn = rpx * n.y - rpy * n.x; // 力臂在法线方向的分量
              // 重新分配粒子-刚体接触缓冲区（如果需要）
              if (system.m_bodyContactCount >= system.m_bodyContactCapacity) {
                int oldCapacity = system.m_bodyContactCapacity;
                int newCapacity =
                    system.m_bodyContactCount != 0
                    ? 2 * system.m_bodyContactCount
                    : Settings.minParticleBufferCapacity;
                system.m_bodyContactBuffer =
                    BufferUtils.reallocateBuffer(ParticleBodyContact.class,
                        system.m_bodyContactBuffer, oldCapacity, newCapacity);
                system.m_bodyContactCapacity = newCapacity;
              }
              ParticleBodyContact contact = system.m_bodyContactBuffer[system.m_bodyContactCount]; // 创建新接触
              contact.index = a;
              contact.body = b;
              contact.weight = 1 - d * system.m_inverseDiameter; // 接触权重
              contact.normal.x = -n.x;
              contact.normal.y = -n.y; // 接触法线 (指向粒子)
              contact.mass = 1 / (invAm + invBm + invBI * rpn * rpn); // 有效质量
              system.m_bodyContactCount++;
            }
          }
        }
      }
      return true; // 继续查询
    }
  }

  /**
   * 实现 QueryCallback 接口，用于解决粒子与刚体之间的碰撞。
   */
  static class SolveCollisionCallback implements QueryCallback {
    ParticleSystem system;
    TimeStep step;

    private final RayCastInput input = new RayCastInput(); // 射线投射输入
    private final RayCastOutput output = new RayCastOutput(); // 射线投射输出
    private final Vec2 tempVec = new Vec2(); // 临时向量
    private final Vec2 tempVec2 = new Vec2(); // 临时向量

    @Override
    public boolean reportFixture(Fixture fixture) {
      if (fixture.isSensor()) {
        return true; // 忽略传感器
      }
      final Shape shape = fixture.getShape();
      Body body = fixture.getBody();
      int childCount = shape.getChildCount();
      for (int childIndex = 0; childIndex < childCount; childIndex++) {
        AABB aabb = fixture.getAABB(childIndex); // 夹具子形状的AABB
        // 扩展AABB，考虑到粒子直径
        final float aabblowerBoundx = aabb.lowerBound.x - system.m_particleDiameter;
        final float aabblowerBoundy = aabb.lowerBound.y - system.m_particleDiameter;
        final float aabbupperBoundx = aabb.upperBound.x + system.m_particleDiameter;
        final float aabbupperBoundy = aabb.upperBound.y + system.m_particleDiameter;
        // 根据扩展AABB的标签范围筛选粒子代理
        int firstProxy =
            lowerBound(
                system.m_proxyBuffer,
                system.m_proxyCount,
                computeTag(system.m_inverseDiameter * aabblowerBoundx, system.m_inverseDiameter
                    * aabblowerBoundy));
        int lastProxy =
            upperBound(
                system.m_proxyBuffer,
                system.m_proxyCount,
                computeTag(system.m_inverseDiameter * aabbupperBoundx, system.m_inverseDiameter
                    * aabbupperBoundy));

        for (int proxy = firstProxy; proxy != lastProxy; ++proxy) {
          int a = system.m_proxyBuffer[proxy].index;
          Vec2 ap = system.m_positionBuffer.data[a];
          if (aabblowerBoundx <= ap.x && ap.x <= aabbupperBoundx && aabblowerBoundy <= ap.y
              && ap.y <= aabbupperBoundy) { // 如果粒子在扩展AABB内
            Vec2 av = system.m_velocityBuffer.data[a]; // 粒子当前速度
            final Vec2 temp = tempVec;
            // 将粒子的当前位置和下一个时间步的位置转换到刚体局部坐标，然后反转换回来，以考虑刚体的运动
            Transform.mulTransToOutUnsafe(body.m_xf0, ap, temp); // 粒子世界位置到刚体上一帧局部位置
            Transform.mulToOutUnsafe(body.m_xf, temp, input.p1); // 刚体当前帧局部位置到世界位置
            input.p2.x = ap.x + step.dt * av.x; // 粒子在下一时间步的预测位置
            input.p2.y = ap.y + step.dt * av.y;
            input.maxFraction = 1; // 最大射线分数
            if (fixture.raycast(output, input, childIndex)) { // 对粒子运动轨迹进行射线投射
              final Vec2 p = tempVec;
              // 计算碰撞点 (略微偏移以避免卡在表面)
              p.x =
                  (1 - output.fraction) * input.p1.x + output.fraction * input.p2.x
                      + Settings.linearSlop * output.normal.x;
              p.y =
                  (1 - output.fraction) * input.p1.y + output.fraction * input.p2.y
                      + Settings.linearSlop * output.normal.y;

              final float vx = step.inv_dt * (p.x - ap.x); // 计算碰撞后的新速度
              final float vy = step.inv_dt * (p.y - ap.y);
              av.x = vx; // 更新粒子速度
              av.y = vy;
              final float particleMass = system.getParticleMass();
              final float ax = particleMass * (av.x - vx); // 计算粒子受到的冲量（未处理前后的速度差）
              final float ay = particleMass * (av.y - vy);
              Vec2 bNormal = output.normal; // 碰撞法线
              final float fdn = ax * bNormal.x + ay * bNormal.y;
              final Vec2 f = tempVec2;
              f.x = fdn * bNormal.x;
              f.y = fdn * bNormal.y; // 碰撞冲量
              body.applyLinearImpulse(f, p, true); // 将冲量应用到刚体
            }
          }
        }
      }
      return true; // 继续查询
    }
  }

  /**
   * 内部辅助类，用于检测各种粒子相关数据结构是否“无效”（例如索引为-1）。
   */
  static class Test {
    static boolean IsProxyInvalid(final Proxy proxy) {
      return proxy.index < 0;
    }

    static boolean IsContactInvalid(final ParticleContact contact) {
      return contact.indexA < 0 || contact.indexB < 0;
    }

    static boolean IsBodyContactInvalid(final ParticleBodyContact contact) {
      return contact.index < 0;
    }

    static boolean IsPairInvalid(final Pair pair) {
      return pair.indexA < 0 || pair.indexB < 0;
    }

    static boolean IsTriadInvalid(final Triad triad) {
      return triad.indexA < 0 || triad.indexB < 0 || triad.indexC < 0;
    }
  };
}