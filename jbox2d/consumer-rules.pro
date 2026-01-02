# JBox2D 物理引擎 - 消费者 ProGuard 规则
# 这些规则会自动应用到主应用的混淆配置中

# 保留所有公开 API（防止反射调用失败）
-keep public class org.jbox2d.** { public *; }

# 保留物理引擎核心类（防止序列化失败）
-keep class org.jbox2d.common.** { *; }
-keep class org.jbox2d.dynamics.** { *; }
-keep class org.jbox2d.collision.** { *; }

# 保留所有枚举类型
-keepclassmembers enum org.jbox2d.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
