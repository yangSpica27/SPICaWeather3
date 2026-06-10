package me.spica.spicaweather3.ui.widget.cloud

import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.EaseInOutBounce
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.isActive
import me.spica.spicaweather3.ui.widget.ShowOnIdleContent
import org.intellij.lang.annotations.Language

@Composable
fun ShaderCloudView(
    collapsedFraction: Float,
    show: Boolean,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        CloudView(collapsedFraction = collapsedFraction, show = show)
        return
    }

    ShaderCloudViewContent(
        collapsedFraction = collapsedFraction,
        show = show
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderCloudViewContent(
    collapsedFraction: Float,
    show: Boolean,
) {
    val shader = remember {
        runCatching { RuntimeShader(shaderCloudAgsl) }
            .getOrElse {
                Log.w("ShaderCloudView", "Failed to compile cloud shader, falling back to Canvas cloud", it)
                null
            }
    }

    if (shader == null) {
        CloudView(collapsedFraction = collapsedFraction, show = show)
        return
    }

    ShowOnIdleContent(
        visible = show,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
        modifier = Modifier.fillMaxSize()
    ) {
        val showProgress by animateFloatAsState(
            targetValue = if (show) 1f else 0f,
            animationSpec = spring(dampingRatio = .45f, stiffness = 500f),
            label = "shader_cloud_show_progress"
        )
        val frameTime = remember { mutableLongStateOf(0L) }
        val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }

        LaunchedEffect(shader) {
            val startTime = withFrameNanos { it }
            while (isActive) {
                withFrameNanos {
                    shader.setFloatUniform("uTime", (it - startTime) / 1.0E9f)
                    frameTime.longValue = it
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = -size.height / 2f * EaseInOutBounce.transform(collapsedFraction)
                }
        ) {
            frameTime.longValue
            shader.setFloatUniform(
                "uResolution",
                size.width.coerceAtLeast(1f),
                size.height.coerceAtLeast(1f)
            )
            shader.setFloatUniform("uShowProgress", showProgress)

            drawIntoCanvas { canvas ->
                paint.shader = shader
                canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
            }
        }
    }
}

@Language("AGSL")
private val shaderCloudAgsl =
    """
    uniform float2 uResolution;
    uniform float uTime;
    uniform float uShowProgress;

    const float PI = 3.14159265358979323846;
    const float TPI = 6.28318530717958647692;

    float noise3D(float3 p) {
        const float3 s = float3(7.0, 157.0, 113.0);
        float3 ip = floor(p);
        float4 h = float4(0.0, s.y, s.z, s.y + s.z) + dot(ip, s);

        p -= ip;
        p = p * p * (3.0 - 2.0 * p);

        float4 h0 = fract(sin(h) * 43758.5453);
        float4 h1 = fract(sin(h + s.x) * 43758.5453);
        h = mix(h0, h1, p.x);

        float2 yz = mix(h.xz, h.yw, p.y);
        return mix(yz.x, yz.y, p.z);
    }

    float fbm(float3 p) {
        float amp = 1.0;
        float fre = 1.0;
        float res = 0.0;
        float div = 0.0;
        float tim = 0.8;

        for (int i = 0; i < 5; ++i) {
            res += amp * noise3D(p * fre);
            p += 0.025 * tim * uTime;
            div += amp;
            amp *= 0.5;
            fre *= 2.0;
            tim *= 0.5;
        }

        return res / div;
    }

    float densityMap(float3 p) {
        return p.y - 1.5 + fbm(p) * 5.0;
    }

    float2 ray(float3 ro, float3 rd) {
        float t = 0.0;
        float maxDensity = 0.0;

        for (int i = 0; i < 64; ++i) {
            float3 p = ro + rd * t;
            float density = 0.5 * densityMap(p);
            maxDensity = max(maxDensity, density);

            if (density > 0.99 || t > 20.0) {
                break;
            }

            t += max(maxDensity * 0.05, 0.05);
        }

        return float2(t, maxDensity);
    }

    float2 rotate(float2 p, float a) {
        float s = sin(a);
        float c = cos(a);
        return float2(c * p.x - s * p.y, s * p.x + c * p.y);
    }

    half4 main(float2 fragCoord) {
        float2 resolution = float2(max(uResolution.x, 1.0), max(uResolution.y, 1.0));
        float2 screen = fragCoord / resolution;
        float2 uv = (-resolution + 2.0 * fragCoord) / resolution.y;
        float viewAngle = sin(uTime * 0.035) * 0.10;

        float3 ro = float3(0.0, 0.25, 0.0);
        ro.xz = rotate(ro.xz, viewAngle * TPI);

        float3 rd = normalize(float3(uv, 1.0));
        rd.xz = rotate(rd.xz, viewAngle * TPI);

        float density = clamp(ray(ro, rd).y, 0.0, 1.0);
        float textSafeMask = mix(0.06, 1.0, smoothstep(0.42, 0.86, screen.x));
        float lowerContentFade = 1.0 - smoothstep(0.58, 0.92, screen.y);
        float edgeAtmosphere = max(
            1.0 - smoothstep(0.0, 0.18, screen.y),
            smoothstep(0.72, 1.0, screen.x) * 0.72
        );

        float cloudAlpha = smoothstep(0.18, 0.95, density) * 0.28 * uShowProgress;
        cloudAlpha *= textSafeMask;
        cloudAlpha *= max(lowerContentFade, edgeAtmosphere * 0.55);

        float3 cloudColor = mix(
            float3(0.58, 0.70, 0.86),
            float3(0.86, 0.93, 1.0),
            smoothstep(0.15, 1.0, density)
        );

        return half4(half3(cloudColor), half(cloudAlpha));
    }
    """.trimIndent()
