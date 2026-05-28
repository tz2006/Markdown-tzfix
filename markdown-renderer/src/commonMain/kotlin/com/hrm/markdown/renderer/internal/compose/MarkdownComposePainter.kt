package com.hrm.markdown.renderer.internal.compose

import androidx.compose.runtime.Composable
import com.hrm.markdown.renderer.internal.layout.model.InternalLayoutDocumentModel

internal interface MarkdownComposePainter {
    @Composable
    fun Paint(
        document: InternalLayoutDocumentModel,
        environment: ComposeRenderEnvironment,
    )
}
