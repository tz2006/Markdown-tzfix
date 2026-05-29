package com.hrm.markdown.renderer

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.Modifier
import com.hrm.codehigh.theme.CodeTheme
import com.hrm.latex.renderer.measure.rememberLatexMeasurer
import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.renderer.internal.MarkdownEngineHost
import com.hrm.markdown.renderer.internal.RendererFacadeState
import com.hrm.markdown.renderer.internal.compose.ComposeRenderEnvironment
import com.hrm.markdown.runtime.MarkdownDirectiveRegistry

@Composable
internal fun MarkdownDocumentRenderer(
    document: Document,
    modifier: Modifier = Modifier,
    theme: MarkdownTheme = MarkdownTheme.auto(),
    codeTheme: CodeTheme? = null,
    config: MarkdownConfig = MarkdownConfig.Default,
    scrollState: ScrollState = rememberScrollState(),
    isStreaming: Boolean = false,
    enablePagination: Boolean = false,
    enableScroll: Boolean = true,
    enableSelection: Boolean = true,
    initialBlockCount: Int = 100,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    imageContent: MarkdownImageRenderer? = null,
    onLinkClick: ((String) -> Unit)? = null,
    directiveRegistry: MarkdownDirectiveRegistry = MarkdownDirectiveRegistry.Empty,
) {
    val renderMode = remember(enableSelection, enableScroll, isStreaming) {
        resolveMarkdownRenderMode(
            enableSelection = enableSelection,
            enableScroll = enableScroll,
            isStreaming = isStreaming,
        )
    }
    val lazyListState = rememberLazyListState()
    val renderDocument = rememberRenderDocument(
        document = document,
        isStreaming = isStreaming,
    )
    ProvideMarkdownTheme(theme) {
        val engineHost = remember { MarkdownEngineHost() }
        val facadeState = remember(
            theme,
            config,
            codeTheme,
            imageContent,
            onLinkClick,
            directiveRegistry,
            isStreaming,
            enableSelection,
        ) {
            RendererFacadeState(
                theme = theme,
                config = config,
                codeTheme = codeTheme,
                imageRenderer = imageContent,
                onLinkClick = onLinkClick,
                directiveRegistry = directiveRegistry,
                isStreaming = isStreaming,
                enableSelection = enableSelection,
            )
        }
        val internalRenderDocument = remember(
            engineHost,
            renderDocument,
            theme,
            config,
            directiveRegistry,
            isStreaming,
        ) {
            engineHost.compile(
                document = renderDocument,
                facadeState = facadeState,
            )
        }
        val density = LocalDensity.current
        val latexMeasurer = rememberLatexMeasurer()
        BoxWithConstraints(modifier = modifier) {
            val viewportWidthPx = with(density) { maxWidth.toPx() }
            val blockSpacingPx = with(density) { theme.blockSpacing.toPx() }
            val textMeasurer = rememberTextMeasurer()
            val layoutDocument = remember(
                engineHost,
                internalRenderDocument,
                facadeState,
                viewportWidthPx,
                blockSpacingPx,
                density,
                textMeasurer,
                latexMeasurer,
            ) {
                engineHost.layout(
                    renderDocument = internalRenderDocument,
                    facadeState = facadeState,
                    viewportWidth = viewportWidthPx,
                    blockSpacing = blockSpacingPx,
                    density = density,
                    textMeasurer = textMeasurer,
                    latexMeasurer = latexMeasurer,
                )
            }
            val renderState = rememberMarkdownBlockRenderState(
                blocks = layoutDocument.blocks,
                renderMode = renderMode,
                enablePagination = enablePagination,
                initialBlockCount = initialBlockCount,
                scrollState = scrollState,
                isStreaming = isStreaming,
            )
            val navigationHandlers = rememberMarkdownNavigationHandlers(
                renderMode = renderMode,
                enableScroll = enableScroll,
                scrollState = scrollState,
                lazyListState = lazyListState,
                effectivePagination = renderState.effectivePagination,
                footnoteDefinitionItemIndexes = layoutDocument.metadata.footnoteDefinitionItemIndexes,
                expandAllBlocks = renderState.expandAllBlocks,
                onLinkClick = onLinkClick,
            )
            ProvideRendererContext(
                document = renderDocument,
                onLinkClick = onLinkClick,
                onFootnoteClick = navigationHandlers.onFootnoteClick,
                onFootnoteBackClick = navigationHandlers.onFootnoteBackClick,
                footnoteNavigationState = navigationHandlers.footnoteNavigationState,
                imageContent = imageContent,
                config = config,
                codeTheme = codeTheme,
                isStreaming = isStreaming,
                directiveRegistry = directiveRegistry,
            ) {
                engineHost.composePainter.Paint(
                    document = layoutDocument,
                    environment = ComposeRenderEnvironment(
                        modifier = Modifier.fillMaxWidth(),
                        renderMode = renderMode,
                        visibleBlockCount = renderState.visibleBlockCount,
                        enableScroll = enableScroll,
                        scrollState = scrollState,
                        lazyListState = lazyListState,
                        header = header,
                        footer = footer,
                    ),
                )
            }
        }
    }
}
