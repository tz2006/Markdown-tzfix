package com.hrm.markdown.renderer.inline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.hrm.markdown.renderer.internal.layout.inline.InlineFlowInput
import com.hrm.markdown.renderer.internal.layout.inline.LineItem
import com.hrm.markdown.renderer.internal.layout.inline.computeInlineFlowLayout
import com.hrm.markdown.renderer.internal.layout.inline.computeIntrinsicHeightPx
import com.hrm.markdown.renderer.internal.layout.inline.computeMaxIntrinsicWidthPx
import com.hrm.markdown.renderer.internal.layout.inline.computeMinIntrinsicWidthPx
import kotlin.math.roundToInt

@Composable
internal fun InlinePaintPayloadText(
    annotated: AnnotatedString,
    paintPayloads: Map<String, InlineWidgetPaintPayload>,
    flowInput: InlineFlowInput,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    if (paintPayloads.isEmpty()) {
        BasicText(
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
                annotated = annotated,
                paintPayloads = paintPayloads,
                input = flowInput,
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
    annotated: AnnotatedString,
    paintPayloads: Map<String, InlineWidgetPaintPayload>,
    input: InlineFlowInput,
    style: TextStyle,
    density: Density,
    textMeasurer: TextMeasurer,
    maxLines: Int,
) {
    BoxWithConstraints {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val flowLayout = remember(input, style, maxWidthPx, density, textMeasurer, maxLines) {
            computeInlineFlowLayout(
                input = input,
                style = style,
                density = density,
                textMeasurer = textMeasurer,
                maxWidthPx = maxWidthPx,
                maxLines = maxLines,
            )
        }

        Layout(
            content = {
                for (line in flowLayout.lines) {
                    for (item in line.items) {
                        when (item) {
                            is LineItem.TextItem -> {
                                BasicText(
                                    text = item.text,
                                    style = line.textStyle,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }

                            is LineItem.InlineItem -> {
                                val widthDp = with(density) { item.widthPx.toDp() }
                                val heightDp = with(density) { item.heightPx.toDp() }
                                val payload = paintPayloads[item.id]
                                key(item.id) {
                                    Box(modifier = Modifier.size(widthDp, heightDp)) {
                                        payload?.content?.invoke()
                                    }
                                }
                            }
                        }
                    }
                }
            },
        ) { measurables, constraints ->
            val placeables = ArrayList<androidx.compose.ui.layout.Placeable>(measurables.size)
            var index = 0
            for (line in flowLayout.lines) {
                for (item in line.items) {
                    val measurable = measurables[index++]
                    val width = item.widthPx.roundToInt().coerceAtLeast(0)
                    val height = item.heightPx.roundToInt().coerceAtLeast(0)
                    placeables += measurable.measure(
                        if (width == 0 || height == 0) Constraints.fixed(0, 0) else Constraints.fixed(width, height)
                    )
                }
            }

            val width = flowLayout.widthPx.roundToInt().coerceIn(constraints.minWidth, constraints.maxWidth)
            val height = flowLayout.heightPx.roundToInt().coerceIn(constraints.minHeight, constraints.maxHeight)
            val alignmentLines = mutableMapOf<AlignmentLine, Int>().apply {
                flowLayout.firstBaselinePx?.let { put(FirstBaseline, it.roundToInt()) }
                flowLayout.lastBaselinePx?.let { put(LastBaseline, it.roundToInt()) }
            }

            layout(width, height, alignmentLines = alignmentLines) {
                var y = 0f
                var placeableIndex = 0
                for (line in flowLayout.lines) {
                    val lineStartX = when (line.textAlign) {
                        TextAlign.Center -> ((maxWidthPx - line.lineWidthPx) / 2f).coerceAtLeast(0f)
                        TextAlign.End, TextAlign.Right -> (maxWidthPx - line.lineWidthPx).coerceAtLeast(0f)
                        else -> 0f
                    }
                    var x = lineStartX
                    for (item in line.items) {
                        val placeable = placeables[placeableIndex++]
                        val itemY = y + ((line.lineHeightPx - item.heightPx) / 2f).coerceAtLeast(0f)
                        placeable.placeRelative(x.roundToInt(), itemY.roundToInt())
                        x += item.widthPx
                    }
                    y += line.lineHeightPx
                }
            }
        }
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
