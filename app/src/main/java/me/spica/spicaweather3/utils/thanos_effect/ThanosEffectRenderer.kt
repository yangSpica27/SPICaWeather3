package me.spica.spicaweather3.utils.thanos_effect

import android.graphics.Bitmap
import android.graphics.Color
import android.opengl.GLES30
import android.opengl.GLUtils
import androidx.compose.ui.graphics.asAndroidBitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 粒子消散渲染器
 *
 * 参考 Aghajari/ThanosEffect 的粒子着色器，实现逐列消散、随机寿命、向外扩散和向上加速。
 * 粒子参数只上传一次，着色器根据时间计算运动。所有方法均在所属 GL 线程执行。
 */
internal class ThanosEffectRenderer(
    private val snapshot: ThanosEffectSnapshot,
    private val config: ThanosEffectConfig,
    private val density: Float,
) {
    private var textureProgram = 0
    private var particleProgram = 0
    private val texture = IntArray(1)
    private val buffer = IntArray(1)
    private val vertexArray = IntArray(1)
    private var particleCount = 0
    private var durationMillis = config.sweepDurationMillis.toFloat()
    private val textureUniforms = HashMap<String, Int>()
    private val particleUniforms = HashMap<String, Int>()

    fun initialize() {
        textureProgram = createProgram(TEXTURE_VERTEX, TEXTURE_FRAGMENT)
        particleProgram = createProgram(PARTICLE_VERTEX, PARTICLE_FRAGMENT)
        listOf("viewport", "bounds", "sweep", "image").forEach {
            textureUniforms[it] = GLES30.glGetUniformLocation(textureProgram, it)
        }
        listOf("viewport", "bounds", "sourceSize", "time", "sweepDuration", "maxPointSize", "rectangles")
            .forEach { particleUniforms[it] = GLES30.glGetUniformLocation(particleProgram, it) }

        // GraphicsLayer 截图可能是硬件位图，先在渲染线程创建软件副本，再采样像素和上传纹理。
        val bitmap = checkNotNull(snapshot.image.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false))
        try {
            GLES30.glGenTextures(1, texture, 0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture[0])
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
            initializeParticles(bitmap)
        } finally {
            bitmap.recycle()
        }
        GLES30.glEnable(GLES30.GL_BLEND)
        // TextureView 和上传的位图都使用预乘透明度。
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        setPointSizeLimit()
        check(GLES30.glGetError() == GLES30.GL_NO_ERROR) { "Unable to initialize dust renderer" }
    }

    private fun initializeParticles(bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height
        var step = max(
            ceil(config.particleSize.value * density).toInt(),
            ceil(sqrt(width.toDouble() * height / config.maxParticles)).toInt(),
        ).coerceAtLeast(1)
        fun gridCount(): Long = ((width + step - 1L) / step) * ((height + step - 1L) / step)
        while (gridCount() > config.maxParticles) step++

        val particles = ByteBuffer.allocateDirect(gridCount().toInt() * STRIDE)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val random = Random.Default
        for (top in 0 until height step step) {
            for (left in 0 until width step step) {
                val x = (left + step / 2).coerceAtMost(width - 1)
                val y = (top + step / 2).coerceAtMost(height - 1)
                val color = pixels[y * width + x]
                if (Color.alpha(color) <= 10) continue
                val delay = x.toFloat() / width * config.sweepDurationMillis
                val lifetime = config.minParticleLifetimeMillis + random.nextFloat() *
                    (config.maxParticleLifetimeMillis - config.minParticleLifetimeMillis)
                durationMillis = max(durationMillis, delay + lifetime)

                particles.put(x.toFloat()).put(y.toFloat())
                particles.put(Color.red(color) / 255f).put(Color.green(color) / 255f)
                    .put(Color.blue(color) / 255f).put(Color.alpha(color) / 255f)
                particles.put(delay).put(lifetime)
                particles.put(step * (0.7f + random.nextFloat() * 0.5f))
                    .put((4f + random.nextFloat() * 4f) * density)
                fun translation(minDp: Float): Float {
                    val distance = (minDp + random.nextFloat() * 56f) * density
                    return if (random.nextInt(10) == 0) -distance / 4f else distance
                }
                particles.put(translation(96f)).put(translation(32f))
                particleCount++
            }
        }
        particles.flip()
        GLES30.glGenVertexArrays(1, vertexArray, 0)
        GLES30.glBindVertexArray(vertexArray[0])
        GLES30.glGenBuffers(1, buffer, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffer[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, particleCount * STRIDE, particles, GLES30.GL_STATIC_DRAW)
        val sizes = intArrayOf(2, 4, 2, 2, 2)
        var offset = 0
        sizes.forEachIndexed { index, size ->
            GLES30.glEnableVertexAttribArray(index)
            GLES30.glVertexAttribPointer(index, size, GLES30.GL_FLOAT, false, STRIDE, offset)
            offset += size * Float.SIZE_BYTES
        }
        GLES30.glBindVertexArray(0)
    }

    /** 绘制当前帧，最后一帧清空画面并返回 false。 */
    fun draw(width: Int, height: Int, elapsedMillis: Float): Boolean {
        GLES30.glViewport(0, 0, width, height)
        GLES30.glClearColor(0f, 0f, 0f, 0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        if (elapsedMillis >= durationMillis) return false

        val bounds = snapshot.bounds
        if (elapsedMillis < config.sweepDurationMillis) {
            GLES30.glUseProgram(textureProgram)
            GLES30.glUniform2f(textureUniforms.getValue("viewport"), width.toFloat(), height.toFloat())
            GLES30.glUniform4f(textureUniforms.getValue("bounds"), bounds.left, bounds.top, bounds.width, bounds.height)
            GLES30.glUniform1f(textureUniforms.getValue("sweep"), elapsedMillis / config.sweepDurationMillis)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture[0])
            GLES30.glUniform1i(textureUniforms.getValue("image"), 0)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        }

        GLES30.glUseProgram(particleProgram)
        GLES30.glUniform2f(particleUniforms.getValue("viewport"), width.toFloat(), height.toFloat())
        GLES30.glUniform4f(particleUniforms.getValue("bounds"), bounds.left, bounds.top, bounds.width, bounds.height)
        GLES30.glUniform2f(particleUniforms.getValue("sourceSize"), snapshot.image.width.toFloat(), snapshot.image.height.toFloat())
        GLES30.glUniform1f(particleUniforms.getValue("time"), elapsedMillis)
        GLES30.glUniform1f(particleUniforms.getValue("sweepDuration"), config.sweepDurationMillis.toFloat())
        GLES30.glUniform1i(particleUniforms.getValue("rectangles"), if (config.drawRectParticles) 1 else 0)
        GLES30.glBindVertexArray(vertexArray[0])
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, particleCount)
        GLES30.glBindVertexArray(0)
        return true
    }

    private fun setPointSizeLimit() {
        val range = FloatArray(2)
        GLES30.glGetFloatv(GLES30.GL_ALIASED_POINT_SIZE_RANGE, range, 0)
        GLES30.glUseProgram(particleProgram)
        GLES30.glUniform1f(particleUniforms.getValue("maxPointSize"), range[1].coerceAtLeast(1f))
    }

    fun release() {
        GLES30.glDeleteBuffers(1, buffer, 0)
        GLES30.glDeleteVertexArrays(1, vertexArray, 0)
        GLES30.glDeleteTextures(1, texture, 0)
        if (textureProgram != 0) GLES30.glDeleteProgram(textureProgram)
        if (particleProgram != 0) GLES30.glDeleteProgram(particleProgram)
    }

    private fun createProgram(vertex: String, fragment: String): Int {
        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertex)
        val fragmentShader = try {
            compileShader(GLES30.GL_FRAGMENT_SHADER, fragment)
        } catch (error: RuntimeException) {
            GLES30.glDeleteShader(vertexShader)
            throw error
        }
        val program = GLES30.glCreateProgram()
        try {
            GLES30.glAttachShader(program, vertexShader)
            GLES30.glAttachShader(program, fragmentShader)
            GLES30.glLinkProgram(program)
            val status = IntArray(1)
            GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0)
            check(status[0] != 0) { GLES30.glGetProgramInfoLog(program) }
            return program
        } catch (error: RuntimeException) {
            GLES30.glDeleteProgram(program)
            throw error
        } finally {
            GLES30.glDeleteShader(vertexShader)
            GLES30.glDeleteShader(fragmentShader)
        }
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val error = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            error(error)
        }
        return shader
    }

    private companion object {
        const val STRIDE = 12 * Float.SIZE_BYTES

        const val TEXTURE_VERTEX = """#version 300 es
            precision highp float;
            uniform vec2 viewport;
            uniform vec4 bounds;
            out vec2 uv;
            void main() {
                uv = vec2(float(gl_VertexID & 1), float(gl_VertexID >> 1));
                vec2 p = bounds.xy + uv * bounds.zw;
                gl_Position = vec4(p.x / viewport.x * 2.0 - 1.0, 1.0 - p.y / viewport.y * 2.0, 0.0, 1.0);
            }
        """
        const val TEXTURE_FRAGMENT = """#version 300 es
            precision highp float;
            uniform sampler2D image;
            uniform float sweep;
            in vec2 uv;
            out vec4 color;
            void main() {
                if (uv.x < sweep) discard;
                color = texture(image, uv);
            }
        """
        const val PARTICLE_VERTEX = """#version 300 es
            precision highp float;
            layout(location = 0) in vec2 position;
            layout(location = 1) in vec4 pixelColor;
            layout(location = 2) in vec2 timing;
            layout(location = 3) in vec2 sizeVelocity;
            layout(location = 4) in vec2 translation;
            uniform vec2 viewport;
            uniform vec4 bounds;
            uniform vec2 sourceSize;
            uniform float time;
            uniform float sweepDuration;
            uniform float maxPointSize;
            out vec4 dustColor;
            void main() {
                float age = time - timing.x;
                float fraction = age / timing.y;
                if (age < 0.0 || fraction >= 1.0) {
                    gl_Position = vec4(2.0, 2.0, 0.0, 1.0);
                    gl_PointSize = 1.0;
                    dustColor = vec4(0.0);
                    return;
                }
                float rendering = sqrt(clamp(time / sweepDuration, 0.0, 1.0));
                vec2 p = position + (position / sourceSize * 2.0 - 1.0) * translation * fraction * rendering;
                p.y -= pow(fraction * sizeVelocity.y, 2.0) * sqrt(rendering);
                p = bounds.xy + p / sourceSize * bounds.zw;
                gl_Position = vec4(p.x / viewport.x * 2.0 - 1.0, 1.0 - p.y / viewport.y * 2.0, 0.0, 1.0);
                float diameter = sizeVelocity.x;
                if (age <= 200.0) diameter *= mix(1.5, 1.0, age / 200.0);
                else diameter *= 1.0 - (age - 200.0) / (timing.y - 200.0);
                gl_PointSize = clamp(diameter * bounds.z / sourceSize.x, 1.0, maxPointSize);
                float alpha = pixelColor.a * min(1.2 - fraction, 1.0) * smoothstep(0.0, 0.1, 1.0 - fraction);
                dustColor = vec4(pixelColor.rgb * alpha, alpha);
            }
        """
        const val PARTICLE_FRAGMENT = """#version 300 es
            precision highp float;
            uniform bool rectangles;
            in vec4 dustColor;
            out vec4 color;
            void main() {
                if (dustColor.a <= 0.0) discard;
                float mask = rectangles ? 1.0 : 1.0 - smoothstep(0.8, 1.0, length(gl_PointCoord * 2.0 - 1.0));
                color = dustColor * mask;
            }
        """
    }
}
