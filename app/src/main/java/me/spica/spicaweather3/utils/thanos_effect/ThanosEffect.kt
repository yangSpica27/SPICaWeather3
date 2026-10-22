package me.spica.spicaweather3.utils.thanos_effect

import android.util.Log
import androidx.annotation.MainThread
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * 灭霸消散效果配置
 *
 * 粒子运动参考：https://github.com/Aghajari/ThanosEffect
 *
 * @property particleSize 粒子采样间距
 * @property maxParticles 单次动画的粒子数量上限
 * @property sweepDurationMillis 从左向右扫描内容的时长，单位为毫秒
 * @property minParticleLifetimeMillis 粒子最短存活时间，单位为毫秒
 * @property maxParticleLifetimeMillis 粒子最长存活时间，单位为毫秒
 * @property drawRectParticles 是否绘制矩形粒子，默认绘制圆形粒子
 */
@Immutable
data class ThanosEffectConfig(
    val particleSize: Dp = 2.dp,
    val maxParticles: Int = 12_000,
    val sweepDurationMillis: Int = 550,
    val minParticleLifetimeMillis: Int = 750,
    val maxParticleLifetimeMillis: Int = 1_250,
    val drawRectParticles: Boolean = false,
) {
    init {
        require(particleSize.value.isFinite() && particleSize.value > 0f)
        require(maxParticles > 0)
        require(sweepDurationMillis > 0)
        require(minParticleLifetimeMillis > 200)
        require(maxParticleLifetimeMillis >= minParticleLifetimeMillis)
    }
}

/**
 * 消散效果叠加层
 *
 * 放在懒加载列表外，使粒子可以越过列表项边界。仅在动画期间创建 TextureView 和 GL 上下文。
 */
@Composable
fun ThanosEffectHost(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val host = remember(scope) { ThanosEffectHostState(scope) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(host, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) host.finishAll()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            host.finishAll()
        }
    }

    Box(modifier.onGloballyPositioned { host.coordinates = it }) {
        CompositionLocalProvider(LocalThanosEffectHost provides host) {
            content()
        }
        host.requests.forEach { request ->
            val snapshot = request.snapshot
            if (snapshot != null) {
                key(request) {
                    AndroidView(
                        factory = { context ->
                            ThanosEffectTextureView(
                                context, snapshot, request.config, request.density,
                                onFirstFrame = { request.state.hideContent() },
                                onComplete = { host.finish(request) },
                            )
                        },
                        modifier = Modifier.matchParentSize(),
                        onRelease = { it.release() },
                    )
                }
            }
        }
    }
}

/**
 * 一次性灭霸消散效果
 *
 * 叠加层呈现首帧后隐藏原内容，动画结束后保持隐藏，等待调用方将内容移除。
 * 页面关闭或渲染失败时也会触发完成回调，保证已确认的删除操作可以继续执行。
 * 未提供外层 [ThanosEffectHost] 时使用局部叠加层，粒子可能被父布局裁剪。
 *
 * @param isDisintegrating 是否触发消散
 * @param onDisintegrationComplete 消散结束后的回调，每次动画只调用一次
 * @param config 粒子和动画配置
 * @param content 要消散的内容
 */
@Composable
fun ThanosEffect(
    isDisintegrating: Boolean,
    onDisintegrationComplete: () -> Unit,
    modifier: Modifier = Modifier,
    config: ThanosEffectConfig = ThanosEffectConfig(),
    content: @Composable () -> Unit,
) {
    if (LocalThanosEffectHost.current == null) {
        ThanosEffectHost(modifier) {
            ThanosEffect(isDisintegrating, onDisintegrationComplete, config = config, content = content)
        }
        return
    }

    val state = rememberThanosEffect()
    val onComplete by rememberUpdatedState(onDisintegrationComplete)
    LaunchedEffect(isDisintegrating, state) {
        if (isDisintegrating) state.start(config) { onComplete() }
    }
    Box(modifier.thanosEffect(state)) { content() }
}

@Composable
fun rememberThanosEffect(): ThanosEffectState {
    val host = checkNotNull(LocalThanosEffectHost.current) {
        "rememberThanosEffect must be used inside ThanosEffectHost"
    }
    val density = LocalDensity.current.density
    return remember(host, density) { ThanosEffectState(host, density) }
}

/** [Modifier.thanosEffect] 的消散状态，每个内容节点独占一个实例。 */
@Stable
class ThanosEffectState internal constructor(
    private val host: ThanosEffectHostState,
    private val density: Float,
) {
    internal var source: ThanosEffectNode? = null
    internal var contentHidden by mutableStateOf(false)
        private set
    var isRunning by mutableStateOf(false)
        private set
    var isComplete by mutableStateOf(false)
        private set

    /** 触发一次消散，重复调用会被忽略。动画由叠加层持有，列表项离屏后仍会完成。 */
    @MainThread
    fun start(
        config: ThanosEffectConfig = ThanosEffectConfig(),
        onComplete: () -> Unit = {},
    ) {
        if (isRunning || isComplete) return
        require(density.isFinite() && density > 0f)
        isRunning = true
        host.start(this, config, density, onComplete)
    }

    internal fun hideContent() {
        contentHidden = true
    }

    internal fun finish() {
        contentHidden = true
        isRunning = false
        isComplete = true
    }
}

internal data class ThanosEffectSnapshot(val image: ImageBitmap, val bounds: Rect)

internal class ThanosEffectRequest(
    val state: ThanosEffectState,
    val config: ThanosEffectConfig,
    val density: Float,
    val onComplete: () -> Unit,
) {
    var snapshot by mutableStateOf<ThanosEffectSnapshot?>(null)
    var captureJob: Job? = null
    var finished = false
}

internal class ThanosEffectHostState(private val scope: CoroutineScope) {
    val requests = mutableStateListOf<ThanosEffectRequest>()
    var coordinates: LayoutCoordinates? = null

    fun start(
        state: ThanosEffectState,
        config: ThanosEffectConfig,
        density: Float,
        onComplete: () -> Unit,
    ) {
        val request = ThanosEffectRequest(state, config, density, onComplete)
        requests.add(request)
        request.captureJob = scope.launch {
            try {
                // 首次组合就触发时，等待内容完成绘制后再捕获。
                val snapshot = withTimeout(1_000) {
                    checkNotNull(state.source) { "Thanos effect has no attached content" }
                        .capture { checkNotNull(coordinates) }
                }
                if (!request.finished) {
                    request.snapshot = snapshot
                    // 不可见或零尺寸的渲染表面可能无法呈现首帧，超时后直接结束。
                    delay(2_000)
                    if (!state.contentHidden) finish(request)
                }
            } catch (cancelled: CancellationException) {
                finish(request)
                throw cancelled
            } catch (error: RuntimeException) {
                Log.w("ThanosEffect", "Content capture failed; completing without animation", error)
                finish(request)
            }
        }
    }

    fun finish(request: ThanosEffectRequest) {
        if (request.finished) return
        request.finished = true
        request.captureJob?.cancel()
        request.state.finish()
        requests.remove(request)
        request.snapshot = null
        request.onComplete()
    }

    fun finishAll() {
        requests.toList().forEach(::finish)
    }
}

private val LocalThanosEffectHost = staticCompositionLocalOf<ThanosEffectHostState?> { null }
