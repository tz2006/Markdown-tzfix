package com.hrm.markdown.renderer.internal.core.compile

import com.hrm.markdown.parser.ast.Abbreviation
import com.hrm.markdown.parser.ast.Autolink
import com.hrm.markdown.parser.ast.CitationReference
import com.hrm.markdown.parser.ast.ContainerNode
import com.hrm.markdown.parser.ast.DirectiveInline
import com.hrm.markdown.parser.ast.Emoji
import com.hrm.markdown.parser.ast.Emphasis
import com.hrm.markdown.parser.ast.EscapedChar
import com.hrm.markdown.parser.ast.FootnoteReference
import com.hrm.markdown.parser.ast.HardLineBreak
import com.hrm.markdown.parser.ast.Highlight
import com.hrm.markdown.parser.ast.HtmlEntity
import com.hrm.markdown.parser.ast.Image
import com.hrm.markdown.parser.ast.InlineCode
import com.hrm.markdown.parser.ast.InlineHtml
import com.hrm.markdown.parser.ast.InlineMath
import com.hrm.markdown.parser.ast.InsertedText
import com.hrm.markdown.parser.ast.KeyboardInput
import com.hrm.markdown.parser.ast.Link
import com.hrm.markdown.parser.ast.Node
import com.hrm.markdown.parser.ast.RubyText
import com.hrm.markdown.parser.ast.SoftLineBreak
import com.hrm.markdown.parser.ast.Spoiler
import com.hrm.markdown.parser.ast.Strikethrough
import com.hrm.markdown.parser.ast.StrongEmphasis
import com.hrm.markdown.parser.ast.StyledText
import com.hrm.markdown.parser.ast.Subscript
import com.hrm.markdown.parser.ast.Superscript
import com.hrm.markdown.parser.ast.Text
import com.hrm.markdown.parser.ast.WikiLink
import com.hrm.markdown.renderer.internal.core.identity.RenderIdentity
import com.hrm.markdown.renderer.internal.core.identity.renderIdentityMix
import com.hrm.markdown.renderer.internal.core.identity.renderIdentitySeed
import com.hrm.markdown.renderer.internal.core.model.DirectiveInlineWidgetModel
import com.hrm.markdown.renderer.internal.core.model.ImageWidgetModel
import com.hrm.markdown.renderer.internal.core.model.InlineAtom
import com.hrm.markdown.renderer.internal.core.model.InlineCodeWidgetModel
import com.hrm.markdown.renderer.internal.core.model.InlineMathWidgetModel
import com.hrm.markdown.renderer.internal.core.model.InlineModel
import com.hrm.markdown.renderer.internal.core.model.RubyTextWidgetModel
import com.hrm.markdown.renderer.internal.core.model.SpanMark
import com.hrm.markdown.renderer.internal.core.model.SpoilerWidgetModel
import com.hrm.markdown.renderer.internal.core.model.TextAtom
import com.hrm.markdown.renderer.internal.core.model.WidgetAtom

internal fun compileInlineModel(
    nodes: List<Node>,
    inlineRevision: Long,
): InlineModel {
    return InlineModel(
        identity = RenderIdentity(
            stableId = stableInlineListId(nodes),
            contentRevision = inlineRevision,
            layoutRevision = inlineRevision,
            paintRevision = 0L,
        ),
        atoms = buildList {
            compileInlineNodes(
                nodes = nodes,
                activeMarks = emptyList(),
                sink = this,
            )
        }
    )
}

private fun compileInlineNodes(
    nodes: List<Node>,
    activeMarks: List<SpanMark>,
    sink: MutableList<InlineAtom>,
) {
    for (node in nodes) {
        compileInlineNode(node, activeMarks, sink)
    }
}

private fun compileInlineNode(
    node: Node,
    activeMarks: List<SpanMark>,
    sink: MutableList<InlineAtom>,
) {
    when (node) {
        is Text -> sink += TextAtom(nodeIdentity(node), node.literal, activeMarks)
        is SoftLineBreak -> sink += TextAtom(nodeIdentity(node), " ", activeMarks)
        is HardLineBreak -> sink += TextAtom(nodeIdentity(node), "\n", activeMarks)
        is Emphasis -> compileInlineNodes(node.children, activeMarks + SpanMark("emphasis"), sink)
        is StrongEmphasis -> compileInlineNodes(node.children, activeMarks + SpanMark("strong"), sink)
        is Strikethrough -> compileInlineNodes(node.children, activeMarks + SpanMark("strikethrough"), sink)
        is Highlight -> compileInlineNodes(node.children, activeMarks + SpanMark("highlight"), sink)
        is Superscript -> compileInlineNodes(node.children, activeMarks + SpanMark("superscript"), sink)
        is Subscript -> compileInlineNodes(node.children, activeMarks + SpanMark("subscript"), sink)
        is InsertedText -> compileInlineNodes(node.children, activeMarks + SpanMark("inserted"), sink)
        is StyledText -> {
            compileInlineNodes(
                node.children,
                activeMarks + SpanMark(
                    kind = "styled",
                    payload = buildMap {
                        node.style?.let { put("style", it) }
                        if (node.cssClasses.isNotEmpty()) {
                            put("class", node.cssClasses.joinToString(" "))
                        }
                    }
                ),
                sink,
            )
        }

        is Link -> {
            compileInlineNodes(
                node.children,
                activeMarks + SpanMark(
                    kind = "link",
                    payload = mapOf(
                        "target" to node.destination,
                        "tag" to "link",
                    )
                ),
                sink,
            )
        }

        is Autolink -> {
            sink += TextAtom(
                identity = nodeIdentity(node),
                text = node.destination,
                marks = activeMarks + SpanMark(
                    kind = "link",
                    payload = mapOf(
                        "target" to node.destination,
                        "tag" to "link",
                    )
                ),
            )
        }

        is WikiLink -> {
            sink += TextAtom(
                identity = nodeIdentity(node),
                text = node.label ?: node.target,
                marks = activeMarks + SpanMark(
                    kind = "link",
                    payload = mapOf(
                        "target" to node.target,
                        "tag" to "wikilink",
                    )
                ),
            )
        }

        is InlineCode -> {
            sink += WidgetAtom(
                identity = nodeIdentity(node),
                widget = InlineCodeWidgetModel(
                    identity = nodeIdentity(node),
                    code = node.literal,
                )
            )
        }

        is Image -> {
            sink += WidgetAtom(
                identity = nodeIdentity(node),
                widget = ImageWidgetModel(
                    identity = nodeIdentity(node),
                    url = node.destination,
                    altText = extractPlainText(node),
                    title = node.title,
                    width = node.imageWidth,
                    height = node.imageHeight,
                    attributes = node.attributes,
                )
            )
        }

//        is InlineMath -> {
//            sink += WidgetAtom(
//                identity = nodeIdentity(node),
//                widget = InlineMathWidgetModel(
//                    identity = nodeIdentity(node),
//                    latex = node.literal,
//                )
//            )
//        }

        is InlineMath -> {
            val baseIdentity = nodeIdentity(node)
            // Mix in atom position to break stableId collisions when sourceRange is unset
            val uniqueStableId = renderIdentityMix(baseIdentity.stableId, sink.size.toLong())
            val uniqueIdentity = baseIdentity.copy(stableId = uniqueStableId)
            sink += WidgetAtom(
                identity = uniqueIdentity,
                widget = InlineMathWidgetModel(
                    identity = uniqueIdentity,
                    latex = node.literal,
                    display = node.display,        // ← pass the display flag too
                )
            )
        }

        is FootnoteReference -> {
            sink += TextAtom(
                identity = nodeIdentity(node),
                text = "[${node.index}]",
                marks = activeMarks + SpanMark(
                    kind = "footnote",
                    payload = mapOf(
                        "label" to node.label,
                        "index" to node.index.toString(),
                    )
                ),
            )
        }

        is CitationReference -> {
            sink += TextAtom(
                identity = nodeIdentity(node),
                text = "[${node.key}]",
                marks = activeMarks + SpanMark(
                    kind = "citation",
                    payload = mapOf("key" to node.key),
                ),
            )
        }

        is InlineHtml -> sink += TextAtom(nodeIdentity(node), node.literal, activeMarks + SpanMark("inline_html"))
        is HtmlEntity -> sink += TextAtom(nodeIdentity(node), node.resolved.ifEmpty { node.literal }, activeMarks)
        is EscapedChar -> sink += TextAtom(nodeIdentity(node), node.literal, activeMarks)
        is Emoji -> sink += TextAtom(nodeIdentity(node), node.unicode ?: node.literal.ifEmpty { ":${node.shortcode}:" }, activeMarks)
        is Abbreviation -> {
            sink += TextAtom(
                identity = nodeIdentity(node),
                text = node.abbreviation,
                marks = activeMarks + SpanMark(
                    kind = "abbreviation",
                    payload = mapOf("fullText" to node.fullText),
                ),
            )
        }

        is KeyboardInput -> sink += TextAtom(nodeIdentity(node), node.literal, activeMarks + SpanMark("kbd"))
        is Spoiler -> {
            sink += WidgetAtom(
                identity = nodeIdentity(node),
                widget = SpoilerWidgetModel(
                    identity = nodeIdentity(node),
                    content = compileInlineModel(node.children, node.contentHash),
                    alternateText = extractPlainText(node),
                ),
            )
        }

        is DirectiveInline -> {
            sink += WidgetAtom(
                identity = nodeIdentity(node),
                widget = DirectiveInlineWidgetModel(
                    identity = nodeIdentity(node),
                    tagName = node.tagName,
                    args = node.args,
                    alternateText = buildInlineDirectiveFallbackText(node),
                ),
            )
        }

        is RubyText -> {
            sink += WidgetAtom(
                identity = nodeIdentity(node),
                widget = RubyTextWidgetModel(
                    identity = nodeIdentity(node),
                    base = node.base,
                    annotation = node.annotation,
                ),
            )
        }

        else -> {
            if (node is ContainerNode) {
                compileInlineNodes(node.children, activeMarks, sink)
            }
        }
    }
}

private fun nodeIdentity(node: Node): RenderIdentity {
    val revision = if (node.contentHash != 0L) node.contentHash else stableInlineNodeId(node)
    return RenderIdentity(
        stableId = stableInlineNodeId(node),
        contentRevision = revision,
        layoutRevision = revision,
        paintRevision = 0L,
    )
}

private fun stableInlineListId(nodes: List<Node>): Long {
    if (nodes.isEmpty()) return 0L
    var acc = renderIdentitySeed()
    for (node in nodes) {
        acc = renderIdentityMix(acc, stableInlineNodeId(node))
    }
    return acc
}

private fun stableInlineNodeId(node: Node): Long {
    var acc = renderIdentitySeed()
    acc = renderIdentityMix(acc, node.sourceRange.start.offset.toLong())
    acc = renderIdentityMix(acc, node.sourceRange.end.offset.toLong())
    acc = renderIdentityMix(acc, node.lineRange.startLine.toLong())
    acc = renderIdentityMix(acc, node.lineRange.endLine.toLong())
    return acc
}

private fun buildInlineDirectiveFallbackText(node: DirectiveInline): String {
    val argsText = if (node.args.isNotEmpty()) {
        " " + node.args.entries.joinToString(" ") { (key, value) ->
            if (key.startsWith("_")) value else "$key=$value"
        }
    } else {
        ""
    }
    return "{% ${node.tagName}$argsText %}"
}

private fun extractPlainText(node: Node): String = buildString {
    when (node) {
        is Text -> append(node.literal)
        is InlineCode -> append(node.literal)
        is Image -> node.children.forEach { append(extractPlainText(it)) }
        is ContainerNode -> node.children.forEach { append(extractPlainText(it)) }
        else -> Unit
    }
}
