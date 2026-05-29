package com.hrm.markdown.renderer.inline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.hrm.markdown.renderer.internal.compose.PaintInlineLayoutContent
import com.hrm.markdown.renderer.internal.core.identity.RenderIdentity
import com.hrm.markdown.renderer.internal.core.model.InlineModel
import com.hrm.markdown.renderer.internal.layout.inline.InlineFlowInput
import com.hrm.markdown.renderer.internal.layout.inline.buildInlineLayoutBlockModel
import com.hrm.markdown.renderer.internal.layout.inline.computeInlineFlowLayout
import com.hrm.markdown.renderer.internal.layout.inline.computeIntrinsicHeightPx
import com.hrm.markdown.renderer.internal.layout.inline.computeMaxIntrinsicWidthPx
import com.hrm.markdown.renderer.internal.layout.inline.computeMinIntrinsicWidthPx
import com.hrm.markdown.renderer.internal.layout.inline.inlineWidgetByPlaceholderId
import com.hrm.markdown.renderer.internal.layout.model.LayoutRect

@Composable
internal fun InlinePaintPayloadText(
    annotated: AnnotatedString,
    paintPayloads: Map<InlinePlaceholderId, InlineWidgetPaintPayload>,
    flowInput: InlineFlowInput,
    inlineModel: InlineModel? = null,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    if (paintPayloads.isEmpty()) {
        androidx.compose.foundation.text.BasicText(
            text = annotated,
            modifier = modifier,
            style = style,
            maxLines = maxLines,
        )
        return
    }

    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val measurePolicy = remember(flowInput, style, density, textMeasurer, maxLines) {
        inlinePaintPayloadMeasurePolicy(
            input = flowInput,
            style = style,
            density = density,
            textMeasurer = textMeasurer,
            maxLines = maxLines,
        )
    }

    Layout(
        modifier = modifier,
        content = {
            InlinePaintPayloadMeasuredContent(
                paintPayloads = paintPayloads,
                input = flowInput,
                inlineModel = inlineModel,
                style = style,
                density = density,
                textMeasurer = textMeasurer,
                maxLines = maxLines,
            )
        },
        measurePolicy = measurePolicy,
    )
}

@Composable
private fun InlinePaintPayloadMeasuredContent(
    paintPayloads: Map<InlinePlaceholderId, InlineWidgetPaintPayload>,
    input: InlineFlowInput,
    inlineModel: InlineModel?,
    style: TextStyle,
    density: Density,
    textMeasurer: TextMeasurer,
    maxLines: Int,
) {
    androidx.compose.foundation.layout.BoxWithConstraints {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val layoutBlock = remember(input, inlineModel, style, maxWidthPx, density, textMeasurer, maxLines, paintPayloads) {
            val flowLayout = computeInlineFlowLayout(
                input = input,
                style = style,
                density = density,
                textMeasurer = textMeasurer,
                maxWidthPx = maxWidthPx,
                maxLines = maxLines,
            )
            val identity = inlineModel?.identity ?: RenderIdentity(0L, 0L, 0L, 0L)
            buildInlineLayoutBlockModel(
                identity = identity,
                frame = LayoutRect(0f, 0f, flowLayout.widthPx, flowLayout.heightPx),
                contentFrame = LayoutRect(0f, 0f, flowLayout.widthPx, flowLayout.heightPx),
                style = style,
                layout = flowLayout,
                inlinePayloads = paintPayloads,
                widgetById = inlineModel?.let(::inlineWidgetByPlaceholderId).orEmpty(),
            )
        }

        PaintInlineLayoutContent(
            block = layoutBlock,
            modifier = Modifier,
        )
    }
}

private fun inlinePaintPayloadMeasurePolicy(
    input: InlineFlowInput,
    style: TextStyle,
    density: Density,
    textMeasurer: TextMeasurer,
    maxLines: Int,
): MeasurePolicy = object : MeasurePolicy {
    override fun androidx.compose.ui.layout.MeasureScope.measure(
        measurables: List<androidx.compose.ui.layout.Measurable>,
        constraints: Constraints,
    ): androidx.compose.ui.layout.MeasureResult {
        val placeable = measurables.singleOrNull()?.measure(constraints)
        val width = placeable?.width ?: constraints.minWidth
        val height = placeable?.height ?: constraints.minHeight
        return layout(width, height) {
            placeable?.placeRelative(0, 0)
        }
    }

    override fun androidx.compose.ui.layout.IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<androidx.compose.ui.layout.IntrinsicMeasurable>,
        height: Int,
    ): Int = computeMinIntrinsicWidthPx(
        input = input,
        style = style,
        density = density,
        textMeasurer = textMeasurer,
    )

    override fun androidx.compose.ui.layout.IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<androidx.compose.ui.layout.IntrinsicMeasurable>,
        height: Int,
    ): Int = computeMaxIntrinsicWidthPx(
        input = input,
        style = style,
        density = density,
        textMeasurer = textMeasurer,
    )

    override fun androidx.compose.ui.layout.IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<androidx.compose.ui.layout.IntrinsicMeasurable>,
        width: Int,
    ): Int = computeIntrinsicHeightPx(
        input = input,
        style = style,
        density = density,
        textMeasurer = textMeasurer,
        maxLines = maxLines,
        widthPx = width,
    )

    override fun androidx.compose.ui.layout.IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<androidx.compose.ui.layout.IntrinsicMeasurable>,
        width: Int,
    ): Int = computeIntrinsicHeightPx(
        input = input,
        style = style,
        density = density,
        textMeasurer = textMeasurer,
        maxLines = maxLines,
        widthPx = width,
    )
}
