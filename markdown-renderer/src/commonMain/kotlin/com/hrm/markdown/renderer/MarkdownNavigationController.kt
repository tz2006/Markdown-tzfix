package com.hrm.markdown.renderer

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.launch

@Stable
internal data class MarkdownNavigationHandlers(
    val footnoteNavigationState: FootnoteNavigationState,
    val onFootnoteClick: (String) -> Unit,
    val onFootnoteBackClick: (String) -> Unit,
)

@Composable
internal fun rememberMarkdownNavigationHandlers(
    renderMode: MarkdownRenderMode,
    enableScroll: Boolean,
    scrollState: ScrollState,
    lazyListState: LazyListState,
    effectivePagination: Boolean,
    footnoteDefinitionItemIndexes: Map<String, Int>,
    expandAllBlocks: () -> Unit,
    onLinkClick: ((String) -> Unit)?,
): MarkdownNavigationHandlers {
    val footnoteNavigationState = remember { FootnoteNavigationState() }
    val coroutineScope = rememberCoroutineScope()
    val currentOnLinkClick = rememberUpdatedState(onLinkClick)
    val currentScrollState = rememberUpdatedState(scrollState)
    val currentLazyListState = rememberUpdatedState(lazyListState)
    val currentFootnoteIndexes = rememberUpdatedState(footnoteDefinitionItemIndexes)
    val currentEffectivePagination = rememberUpdatedState(effectivePagination)
    val currentExpandAllBlocks = rememberUpdatedState(expandAllBlocks)

    val onFootnoteClick = remember(
        footnoteNavigationState,
        renderMode,
        enableScroll,
        scrollState,
        lazyListState,
        effectivePagination,
        footnoteDefinitionItemIndexes,
    ) {
        { label: String ->
            coroutineScope.launch {
                when (renderMode) {
                    MarkdownRenderMode.LazyColumn -> {
                        val lazyState = currentLazyListState.value
                        footnoteNavigationState.rememberLazyListPosition(
                            label = label,
                            index = lazyState.firstVisibleItemIndex,
                            offset = lazyState.firstVisibleItemScrollOffset,
                        )
                        val targetIndex = currentFootnoteIndexes.value[label]
                        if (enableScroll && targetIndex != null) {
                            lazyState.animateScrollToItem(targetIndex)
                            withFrameNanos { }
                        }
                        if (!footnoteNavigationState.bringDefinitionIntoView(label)) {
                            currentOnLinkClick.value?.invoke("#fn-$label")
                        }
                    }

                    else -> {
                        footnoteNavigationState.rememberReturnPosition(label, currentScrollState.value.value)

                        if (currentEffectivePagination.value && !footnoteNavigationState.hasDefinition(label)) {
                            currentExpandAllBlocks.value.invoke()
                            withFrameNanos { }
                        }

                        if (!footnoteNavigationState.bringDefinitionIntoView(label)) {
                            currentOnLinkClick.value?.invoke("#fn-$label")
                        }
                    }
                }
            }
            Unit
        }
    }
    val onFootnoteBackClick = remember(footnoteNavigationState, enableScroll, renderMode, scrollState, lazyListState) {
        { label: String ->
            coroutineScope.launch {
                val returnPosition = footnoteNavigationState.getReturnPosition(label)
                if (returnPosition != null && enableScroll) {
                    when (returnPosition) {
                        is FootnoteReturnPosition.Scroll ->
                            currentScrollState.value.animateScrollTo(returnPosition.value)

                        is FootnoteReturnPosition.LazyList ->
                            currentLazyListState.value.animateScrollToItem(returnPosition.index, returnPosition.offset)
                    }
                }
            }
            Unit
        }
    }

    return MarkdownNavigationHandlers(
        footnoteNavigationState = footnoteNavigationState,
        onFootnoteClick = onFootnoteClick,
        onFootnoteBackClick = onFootnoteBackClick,
    )
}
