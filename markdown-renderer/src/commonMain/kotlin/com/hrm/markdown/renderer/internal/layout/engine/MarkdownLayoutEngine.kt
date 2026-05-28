package com.hrm.markdown.renderer.internal.layout.engine

import com.hrm.markdown.renderer.internal.core.model.InternalRenderDocumentModel
import com.hrm.markdown.renderer.internal.layout.model.InternalLayoutDocumentModel

interface MarkdownLayoutEngine {
    fun layout(
        document: InternalRenderDocumentModel,
        environment: LayoutEnvironment,
    ): InternalLayoutDocumentModel
}
