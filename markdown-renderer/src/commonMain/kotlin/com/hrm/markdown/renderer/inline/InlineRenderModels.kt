package com.hrm.markdown.renderer.inline

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.TextUnit
import com.hrm.markdown.renderer.internal.layout.inline.InlineFlowInput
import com.hrm.markdown.renderer.internal.layout.inline.InlinePlaceholderLayoutSpec

internal const val INLINE_PLACEHOLDER_TAG = "markdown-inline-placeholder"
internal const val INLINE_PLACEHOLDER_CHAR = '\uFFFC'

internal data class InlineWidgetPaintPayload(
    val alternateText: String,
    val placeholder: InlinePlaceholderLayoutSpec,
    val content: @Composable () -> Unit,
)

internal data class InlineContentResult(
    val annotated: AnnotatedString,
    val paintPayloads: Map<String, InlineWidgetPaintPayload>,
    val flowInput: InlineFlowInput,
)

internal fun inlineWidgetPaintPayload(
    alternateText: String,
    width: TextUnit,
    height: TextUnit,
    content: @Composable () -> Unit,
): InlineWidgetPaintPayload {
    return InlineWidgetPaintPayload(
        alternateText = alternateText,
        placeholder = InlinePlaceholderLayoutSpec(
            alternateText = alternateText,
            width = width,
            height = height,
        ),
        content = content,
    )
}

internal fun AnnotatedString.Builder.appendInlinePlaceholder(id: String) {
    pushStringAnnotation(tag = INLINE_PLACEHOLDER_TAG, annotation = id)
    append(INLINE_PLACEHOLDER_CHAR)
    pop()
}
