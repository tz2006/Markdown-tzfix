package com.hrm.markdown.renderer.inline

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals

class InlineRenderModelsTest {
    @Test
    fun should_convert_inline_content_entry_to_layout_placeholder_spec() {
        val entry = InlineContentEntry(
            alternateText = "latex",
            inlineTextContent = InlineTextContent(
                placeholder = Placeholder(
                    width = 12.sp,
                    height = 18.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
            ) { }
        )

        val spec = entry.toPlaceholderLayoutSpec()

        assertEquals("latex", spec.alternateText)
        assertEquals(12.sp, spec.width)
        assertEquals(18.sp, spec.height)
    }
}
