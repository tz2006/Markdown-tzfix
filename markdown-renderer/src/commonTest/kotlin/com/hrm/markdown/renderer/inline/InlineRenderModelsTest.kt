package com.hrm.markdown.renderer.inline

import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals

class InlineRenderModelsTest {
    @Test
    fun should_build_inline_widget_paint_payload_with_placeholder_spec() {
        val payload = inlineWidgetPaintPayload(
            alternateText = "latex",
            width = 12.sp,
            height = 18.sp,
        ) { }

        val spec = payload.placeholder

        assertEquals("latex", spec.alternateText)
        assertEquals(12.sp, spec.width)
        assertEquals(18.sp, spec.height)
    }
}
