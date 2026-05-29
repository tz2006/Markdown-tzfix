package com.hrm.markdown.renderer.internal.layout.inline

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

private val WHITESPACE_BOUNDARY_REGEX = Regex("(?<=\\s)|(?=\\s)")

internal fun computeInlineFlowLayout(
    input: InlineFlowInput,
    style: TextStyle,
    density: Density,
    textMeasurer: TextMeasurer,
    maxWidthPx: Float,
    maxLines: Int,
): InlineFlowLayout {
    val textStyle = textMeasurementStyle(style)
    val baseLineHeightPx = baseLineHeightPx(style, density)
    val baseMetrics = textMeasurer.measure(
        text = "Ag",
        style = textStyle,
        constraints = Constraints(maxWidth = Int.MAX_VALUE),
        maxLines = 1,
        softWrap = false,
    )
    val baseTextHeightPx = baseMetrics.size.height.toFloat()
    val baseTextBaselinePx = baseMetrics.firstBaseline

    data class MeasuredText(val widthPx: Float, val heightPx: Float, val baselinePx: Float)

    fun measureText(a: AnnotatedString): MeasuredText {
        if (a.isEmpty()) return MeasuredText(0f, 0f, 0f)
        val res = textMeasurer.measure(
            text = a,
            style = textStyle,
            constraints = Constraints(maxWidth = Int.MAX_VALUE),
            maxLines = 1,
            softWrap = false,
        )
        return MeasuredText(
            widthPx = res.size.width.toFloat(),
            heightPx = res.size.height.toFloat(),
            baselinePx = res.firstBaseline,
        )
    }

    fun inlineSizePx(placeholder: InlinePlaceholderLayoutSpec): Pair<Float, Float> {
        return placeholderSizePx(placeholder)
    }

    val lines = ArrayList<InlineFlowLine>()
    var currentItems = ArrayList<LineItem>()
    var currentWidth = 0f
    var maxItemHeight = 0f
    var lineCount = 0

    fun flushLine(force: Boolean = false) {
        if (!force && currentItems.isEmpty()) return
        val lineHeightPx = maxOf(baseLineHeightPx, maxItemHeight)
        val firstTextBaselinePx = currentItems.firstNotNullOfOrNull { item ->
            (item as? LineItem.TextItem)?.let { textItem ->
                ((lineHeightPx - textItem.heightPx) / 2f).coerceAtLeast(0f) + textItem.baselinePx
            }
        }
        lines += InlineFlowLine(
            textStyle = textStyle,
            lineWidthPx = currentWidth,
            lineHeightPx = lineHeightPx,
            baselinePx = firstTextBaselinePx
                ?: (((lineHeightPx - baseTextHeightPx) / 2f).coerceAtLeast(0f) + baseTextBaselinePx),
            textAlign = style.textAlign,
            items = currentItems.toList(),
        )
        currentItems = ArrayList()
        currentWidth = 0f
        maxItemHeight = 0f
        lineCount++
    }

    for (token in input.segments) {
        if (lineCount >= maxLines) break
        when (token) {
            InlineFlowSegment.Newline -> flushLine(force = true)
            is InlineFlowSegment.InlineRun -> {
                val (w, h) = inlineSizePx(token.placeholder)
                if (w <= 0f || h <= 0f) continue
                if (currentWidth > 0f && currentWidth + w > maxWidthPx) {
                    flushLine(force = true)
                    if (lineCount >= maxLines) break
                }
                currentItems.add(
                    LineItem.InlineItem(
                        id = token.id,
                        widthPx = w,
                        heightPx = h,
                        alternateText = token.placeholder.alternateText,
                    )
                )
                currentWidth += w
                maxItemHeight = maxOf(maxItemHeight, h)
            }

            is InlineFlowSegment.TextRun -> {
                var remaining = token.annotated
                while (remaining.isNotEmpty() && lineCount < maxLines) {
                    val measured = measureText(remaining)
                    val w = measured.widthPx
                    val h = measured.heightPx
                    val used = currentWidth
                    if (used + w <= maxWidthPx) {
                        currentItems.add(
                            LineItem.TextItem(
                                text = remaining,
                                widthPx = w,
                                heightPx = h,
                                baselinePx = measured.baselinePx,
                            )
                        )
                        currentWidth += w
                        maxItemHeight = maxOf(maxItemHeight, h)
                        break
                    }

                    val fit = splitTextToFit(
                        text = remaining,
                        style = textStyle,
                        textMeasurer = textMeasurer,
                        maxWidthPx = (maxWidthPx - used).coerceAtLeast(1f),
                    )
                    if (fit.fit.isNotEmpty()) {
                        val fitMeasured = measureText(fit.fit)
                        val fw = fitMeasured.widthPx
                        val fh = fitMeasured.heightPx
                        currentItems.add(
                            LineItem.TextItem(
                                text = fit.fit,
                                widthPx = fw,
                                heightPx = fh,
                                baselinePx = fitMeasured.baselinePx,
                            )
                        )
                        currentWidth += fw
                        maxItemHeight = maxOf(maxItemHeight, fh)
                    }
                    flushLine(force = true)
                    remaining = fit.rest.trimLeadingSpaces()
                }
            }
        }
    }
    if (lineCount < maxLines) flushLine(force = false)

    val totalHeightPx = lines.sumOf { it.lineHeightPx.toDouble() }.toFloat()
    val firstBaselinePx = lines.firstOrNull()?.baselinePx
    val lastBaselinePx = if (lines.isEmpty()) null else {
        var y = 0f
        for (i in 0 until lines.lastIndex) {
            y += lines[i].lineHeightPx
        }
        y + lines.last().baselinePx
    }

    return InlineFlowLayout(
        widthPx = maxWidthPx,
        heightPx = totalHeightPx,
        firstBaselinePx = firstBaselinePx,
        lastBaselinePx = lastBaselinePx,
        lines = lines,
    )
}

private data class SplitResult(val fit: AnnotatedString, val rest: AnnotatedString)

private fun splitTextToFit(
    text: AnnotatedString,
    style: TextStyle,
    textMeasurer: TextMeasurer,
    maxWidthPx: Float,
): SplitResult {
    if (text.isEmpty()) return SplitResult(AnnotatedString(""), AnnotatedString(""))
    val full = text.text
    var lo = 0
    var hi = full.length
    var best = 0
    while (lo <= hi) {
        val mid = (lo + hi) / 2
        val sub = text.subSequence(0, mid)
        val w = textMeasurer.measure(
            text = sub,
            style = style,
            constraints = Constraints(maxWidth = Int.MAX_VALUE),
            maxLines = 1,
            softWrap = false,
        ).size.width.toFloat()
        if (w <= maxWidthPx) {
            best = mid
            lo = mid + 1
        } else {
            hi = mid - 1
        }
    }
    if (best <= 0) best = 1.coerceAtMost(full.length)

    val region = full.substring(0, best)
    val lastSpace = region.indexOfLast { it.isWhitespace() }
    val cut = if (lastSpace >= 1) lastSpace + 1 else best
    return SplitResult(
        fit = text.subSequence(0, cut),
        rest = text.subSequence(cut, full.length),
    )
}

private fun AnnotatedString.trimLeadingSpaces(): AnnotatedString {
    val s = text
    var i = 0
    while (i < s.length && s[i].isWhitespace() && s[i] != '\n') i++
    return if (i == 0) this else subSequence(i, s.length)
}

internal fun textMeasurementStyle(style: TextStyle): TextStyle {
    return if (style.lineHeight.value.isNaN()) style else style.copy(lineHeight = TextUnit.Unspecified)
}

internal fun baseLineHeightPx(style: TextStyle, density: Density): Float = with(density) {
    val lh = style.lineHeight.value.takeUnless { it.isNaN() }
        ?: style.fontSize.value.takeUnless { it.isNaN() }?.times(1.5f)
        ?: 0f
    lh.sp.toPx()
}.coerceAtLeast(0f)

internal fun placeholderSizePx(
    placeholder: InlinePlaceholderLayoutSpec,
): Pair<Float, Float> {
    return placeholder.widthPx to placeholder.heightPx
}

internal fun computeMaxIntrinsicWidthPx(
    input: InlineFlowInput,
    style: TextStyle,
    density: Density,
    textMeasurer: TextMeasurer,
): Int {
    val textStyle = textMeasurementStyle(style)
    var lineWidth = 0f
    var maxLineWidth = 0f
    for (token in input.segments) {
        when (token) {
            InlineFlowSegment.Newline -> {
                maxLineWidth = maxOf(maxLineWidth, lineWidth)
                lineWidth = 0f
            }

            is InlineFlowSegment.InlineRun -> {
                lineWidth += placeholderSizePx(token.placeholder).first
            }

            is InlineFlowSegment.TextRun -> {
                if (token.annotated.isEmpty()) continue
                val width = textMeasurer.measure(
                    text = token.annotated,
                    style = textStyle,
                    constraints = Constraints(maxWidth = Int.MAX_VALUE),
                    maxLines = 1,
                    softWrap = false,
                ).size.width.toFloat()
                lineWidth += width
            }
        }
    }
    maxLineWidth = maxOf(maxLineWidth, lineWidth)
    return ceil(maxLineWidth).toInt()
}

internal fun computeMinIntrinsicWidthPx(
    input: InlineFlowInput,
    style: TextStyle,
    density: Density,
    textMeasurer: TextMeasurer,
): Int {
    val textStyle = textMeasurementStyle(style)
    var maxPieceWidth = 0f
    for (token in input.segments) {
        when (token) {
            InlineFlowSegment.Newline -> Unit
            is InlineFlowSegment.InlineRun -> {
                maxPieceWidth =
                    maxOf(maxPieceWidth, placeholderSizePx(token.placeholder).first)
            }

            is InlineFlowSegment.TextRun -> {
                val pieces = token.annotated.text.split(WHITESPACE_BOUNDARY_REGEX)
                var cursor = 0
                for (piece in pieces) {
                    if (piece.isEmpty()) continue
                    val end = cursor + piece.length
                    val sub = token.annotated.subSequence(cursor, end)
                    val width = textMeasurer.measure(
                        text = sub,
                        style = textStyle,
                        constraints = Constraints(maxWidth = Int.MAX_VALUE),
                        maxLines = 1,
                        softWrap = false,
                    ).size.width.toFloat()
                    maxPieceWidth = maxOf(maxPieceWidth, width)
                    cursor = end
                }
            }
        }
    }
    return ceil(maxPieceWidth).toInt()
}

internal fun computeIntrinsicHeightPx(
    input: InlineFlowInput,
    style: TextStyle,
    density: Density,
    textMeasurer: TextMeasurer,
    maxLines: Int,
    widthPx: Int,
): Int {
    val targetWidth = if (widthPx == Constraints.Infinity || widthPx <= 0) {
        computeMaxIntrinsicWidthPx(
            input,
            style,
            density,
            textMeasurer
        ).coerceAtLeast(1)
    } else {
        widthPx
    }
    val layout = computeInlineFlowLayout(
        input = input,
        style = style,
        density = density,
        textMeasurer = textMeasurer,
        maxWidthPx = targetWidth.toFloat(),
        maxLines = maxLines,
    )
    return ceil(layout.heightPx).toInt()
}
