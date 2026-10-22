package me.spica.spicaweather3.utils.thanos_effect

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import kotlinx.coroutines.CompletableDeferred

/** 为内容添加消散效果，放在外层变换和内边距之后、待捕获内容之前。 */
fun Modifier.thanosEffect(state: ThanosEffectState): Modifier = this then ThanosEffectElement(state)

private data class ThanosEffectElement(val state: ThanosEffectState) :
    ModifierNodeElement<ThanosEffectNode>() {
    override fun create() = ThanosEffectNode(state)

    override fun update(node: ThanosEffectNode) = node.update(state)

    override fun InspectorInfo.inspectableProperties() {
        name = "thanosEffect"
        properties["state"] = state
    }
}

internal class ThanosEffectNode(private var state: ThanosEffectState) : Modifier.Node(),
    DrawModifierNode, GlobalPositionAwareModifierNode {
    private var layer: GraphicsLayer? = null
    private var coordinates: LayoutCoordinates? = null
    private var recorded = CompletableDeferred<Unit>()
    private var frozen = false

    override fun onAttach() {
        check(state.source == null || state.source === this) {
            "A ThanosEffectState cannot capture multiple components"
        }
        state.source = this
        layer = requireGraphicsContext().createGraphicsLayer()
    }

    fun update(newState: ThanosEffectState) {
        if (state === newState) return
        if (state.source === this) state.source = null
        val currentCoordinates = coordinates
        resetRecording()
        coordinates = currentCoordinates
        state = newState
        if (isAttached) {
            check(newState.source == null || newState.source === this) {
                "A ThanosEffectState cannot capture multiple components"
            }
            newState.source = this
        }
    }

    override fun onReset() {
        resetRecording()
        invalidateDraw()
    }

    override fun onDetach() {
        if (state.source === this) state.source = null
        resetRecording()
        layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        layer = null
    }

    private fun resetRecording() {
        recorded.cancel()
        recorded = CompletableDeferred()
        coordinates = null
        frozen = false
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }

    override fun ContentDrawScope.draw() {
        if (state.contentHidden) return
        val graphicsLayer = layer ?: return drawContent()
        if (!frozen) {
            graphicsLayer.record { this@draw.drawContent() }
            if (size.width > 0f && size.height > 0f) recorded.complete(Unit)
        }
        drawLayer(graphicsLayer)
    }

    suspend fun capture(hostCoordinates: () -> LayoutCoordinates): ThanosEffectSnapshot {
        recorded.await()
        val sourceCoordinates = checkNotNull(coordinates)
        val host = hostCoordinates()
        check(isAttached && sourceCoordinates.isAttached && host.isAttached)
        val bounds = host.localBoundingBoxOf(sourceCoordinates, clipBounds = false)
        check(bounds.width > 0f && bounds.height > 0f)
        // 等待 GPU 回读时冻结图层，避免捕获过程中内容发生变化。
        frozen = true
        val image = checkNotNull(layer).toImageBitmap()
        return ThanosEffectSnapshot(image, bounds)
    }
}
