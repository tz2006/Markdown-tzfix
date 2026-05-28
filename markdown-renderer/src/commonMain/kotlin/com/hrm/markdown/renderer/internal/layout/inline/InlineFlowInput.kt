package com.hrm.markdown.renderer.internal.layout.inline

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.TextUnit

internal data class InlineFlowInput(
    val segments: List<InlineFlowSegment>,
)

internal data class InlinePlaceholderLayoutSpec(
    val alternateText: String,
    val width: TextUnit,
    val height: TextUnit,
)

internal sealed interface InlineFlowSegment {
    data class TextRun(val annotated: AnnotatedString) : InlineFlowSegment
    data class InlineRun(
        val id: String,
        val placeholder: InlinePlaceholderLayoutSpec,
    ) : InlineFlowSegment

    data object Newline : InlineFlowSegment
}
