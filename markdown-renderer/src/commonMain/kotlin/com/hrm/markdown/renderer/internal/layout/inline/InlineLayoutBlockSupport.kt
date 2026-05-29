package com.hrm.markdown.renderer.internal.layout.inline

import com.hrm.markdown.renderer.inline.InlinePlaceholderId
import com.hrm.markdown.renderer.internal.core.identity.RenderIdentity
import com.hrm.markdown.renderer.internal.core.identity.renderIdentityFromText
import com.hrm.markdown.renderer.internal.core.model.InlineModel
import com.hrm.markdown.renderer.internal.core.model.InlineWidgetModel
import com.hrm.markdown.renderer.internal.core.model.WidgetAtom
import com.hrm.markdown.renderer.internal.layout.model.LayoutInlineBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutInlineLine
import com.hrm.markdown.renderer.internal.layout.model.LayoutRect
import com.hrm.markdown.renderer.internal.layout.model.LayoutTextRun
import com.hrm.markdown.renderer.internal.layout.model.LayoutWidgetRun
import com.hrm.markdown.renderer.inline.InlineWidgetPaintPayload
import androidx.compose.ui.text.TextStyle

internal fun inlineWidgetByPlaceholderId(
    model: InlineModel,
): Map<InlinePlaceholderId, InlineWidgetModel> {
    return model.atoms.asSequence()
        .mapNotNull { atom -> (atom as? WidgetAtom)?.widget }
        .associateBy { widget -> InlinePlaceholderId.from(widget) }
}

internal fun buildInlineLayoutLines(
    identity: RenderIdentity,
    contentLeft: Float,
    contentTop: Float,
    layout: InlineFlowLayout,
    widgetById: Map<InlinePlaceholderId, InlineWidgetModel>,
): List<LayoutInlineLine> {
    var lineTop = contentTop
    return layout.lines.map { line ->
        var cursorX = contentLeft
        val runs = line.items.map { item ->
            when (item) {
                is LineItem.TextItem -> {
                    val run = LayoutTextRun(
                        identity = RenderIdentity(
                            stableId = renderIdentityFromText(item.text.text, identity.stableId + cursorX.toLong()),
                            contentRevision = identity.contentRevision,
                            layoutRevision = identity.layoutRevision,
                            paintRevision = identity.paintRevision,
                        ),
                        frame = LayoutRect(cursorX, lineTop, item.widthPx, item.heightPx),
                        text = item.text,
                    )
                    cursorX += item.widthPx
                    run
                }

                is LineItem.InlineItem -> {
                    val widget = widgetById[item.id]
                    val run = LayoutWidgetRun(
                        identity = widget?.identity ?: identity,
                        frame = LayoutRect(cursorX, lineTop, item.widthPx, item.heightPx),
                        id = item.id,
                        widget = widget ?: throw IllegalStateException("Missing inline widget for placeholder ${item.id}"),
                        alternateText = item.alternateText,
                    )
                    cursorX += item.widthPx
                    run
                }
            }
        }
        LayoutInlineLine(
            frame = LayoutRect(contentLeft, lineTop, line.lineWidthPx, line.lineHeightPx),
            baseline = line.baselinePx,
            runs = runs,
        ).also {
            lineTop += line.lineHeightPx
        }
    }
}

internal fun buildInlineLayoutBlockModel(
    identity: RenderIdentity,
    frame: LayoutRect,
    contentFrame: LayoutRect,
    style: TextStyle,
    layout: InlineFlowLayout,
    inlinePayloads: Map<InlinePlaceholderId, InlineWidgetPaintPayload>,
    widgetById: Map<InlinePlaceholderId, InlineWidgetModel>,
    showDivider: Boolean = false,
): LayoutInlineBlockModel {
    return LayoutInlineBlockModel(
        identity = identity,
        frame = frame,
        contentFrame = contentFrame,
        style = style,
        inlinePayloads = inlinePayloads,
        showDivider = showDivider,
        lines = buildInlineLayoutLines(
            identity = identity,
            contentLeft = contentFrame.left,
            contentTop = contentFrame.top,
            layout = layout,
            widgetById = widgetById,
        ),
    )
}
