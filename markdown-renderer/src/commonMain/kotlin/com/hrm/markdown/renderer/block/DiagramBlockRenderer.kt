package com.hrm.markdown.renderer.block

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.hrm.diagram.render.Diagram
import com.hrm.diagram.render.compose.DiagramPresentationMode
import com.hrm.diagram.render.compose.DiagramView
import com.hrm.markdown.parser.ast.DiagramBlock
import com.hrm.markdown.renderer.LocalMarkdownTheme
import com.hrm.markdown.renderer.diagram.DiagramFallback
import com.hrm.markdown.renderer.internal.core.model.DiagramBlockWidgetModel

/**
 * 图表块渲染器。
 *
 * 优先复用外部 `diagram` 库的统一 Compose 入口；当图表类型暂不受该库支持时，
 * 退回到代码展示 fallback，避免丢失原始内容。
 */
@Composable
internal fun DiagramBlockRenderer(
    node: DiagramBlock,
    modifier: Modifier = Modifier,
) {
    RenderDiagramBlockWidgetModel(
        model = DiagramBlockWidgetModel(
            identity = com.hrm.markdown.renderer.internal.core.identity.RenderIdentity(
                stableId = node.stableKey.toLong(),
                contentRevision = node.contentHash,
                layoutRevision = node.contentHash,
                paintRevision = 0L,
            ),
            diagramType = node.diagramType,
            code = node.literal,
        ),
        modifier = modifier,
    )
}

@Composable
internal fun RenderDiagramBlockWidgetModel(
    model: DiagramBlockWidgetModel,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val diagramBackground = Color(theme.diagramTheme.colors.canvas.argb)
    val code = model.code.trimEnd('\n')
    val diagramType = model.diagramType.lowercase()
    val detection = remember(code, diagramType) {
        Diagram.detectSource(
            source = code,
            hint = diagramType,
        )
    }
    val typeName = remember(model.diagramType) {
        model.diagramType.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.codeBlockCornerRadius))
            .background(diagramBackground)
            .padding(theme.codeBlockPadding),
    ) {
        if (code.isNotBlank() && detection.shouldRouteToDiagram) {
            DiagramView(
                source = code,
                theme = theme.diagramTheme,
                modifier = Modifier.fillMaxWidth(),
                zoomEnabled = false,
                presentationMode = DiagramPresentationMode.Embedded,
            )
        } else {
            DiagramFallback(code, typeName)
        }
    }
}
