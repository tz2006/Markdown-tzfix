package com.hrm.markdown.renderer

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.hrm.markdown.parser.log.HLog
import com.hrm.markdown.renderer.internal.layout.model.InternalLayoutBlockModel

private const val TAG_RENDER = "MarkdownRender"

@Stable
internal data class MarkdownBlockRenderState(
    val totalBlockCount: Int,
    val visibleBlockCount: Int,
    val effectivePagination: Boolean,
    val expandAllBlocks: () -> Unit,
)

@Composable
internal fun rememberMarkdownBlockRenderState(
    blocks: List<InternalLayoutBlockModel>,
    renderMode: MarkdownRenderMode,
    enablePagination: Boolean,
    initialBlockCount: Int,
    scrollState: ScrollState,
    isStreaming: Boolean,
): MarkdownBlockRenderState {
    val blockIdsState = remember { mutableStateOf(emptyList<Long>(), neverEqualPolicy()) }
    val blockLayoutRevisionsState = remember { mutableStateOf(emptyList<Long>()) }
    val newIds = blocks.map { it.identity.stableId }
    val newRevisions = blocks.map { it.identity.layoutRevision }
    val shouldRefreshBlocks = !longListEqual(blockIdsState.value, newIds) ||
        !longListEqual(blockLayoutRevisionsState.value, newRevisions)
    if (shouldRefreshBlocks) {
        HLog.d(TAG_RENDER) { "layout blocks updated: ${blockIdsState.value.size} -> ${blocks.size}" }
        blockIdsState.value = newIds
        blockLayoutRevisionsState.value = newRevisions
    }
    val totalBlockCount = blockIdsState.value.size
    val effectivePagination = enablePagination && renderMode != MarkdownRenderMode.LazyColumn
    val paginationStateKey = if (effectivePagination && !isStreaming) newIds to newRevisions else Unit
    var visibleBlockCount by remember(paginationStateKey, initialBlockCount) {
        mutableIntStateOf(initialVisibleBlockCount(initialBlockCount, totalBlockCount))
    }
    val effectiveVisibleBlockCount = visibleBlockCount.coerceAtMost(totalBlockCount)

    LaunchedEffect(scrollState, effectivePagination, totalBlockCount) {
        if (!effectivePagination) return@LaunchedEffect

        snapshotFlow {
            if (scrollState.maxValue > 0) {
                scrollState.value.toFloat() / scrollState.maxValue.toFloat()
            } else {
                0f
            }
        }.collect { scrollProgress ->
            if (scrollProgress > 0.8f && visibleBlockCount < totalBlockCount) {
                val increment = 50
                visibleBlockCount = (visibleBlockCount + increment).coerceAtMost(totalBlockCount)
            }
        }
    }
    val currentBlockCount = rememberUpdatedState(totalBlockCount)
    val expandAllBlocks: () -> Unit = remember {
        { visibleBlockCount = currentBlockCount.value }
    }

    return MarkdownBlockRenderState(
        totalBlockCount = totalBlockCount,
        visibleBlockCount = if (effectivePagination) effectiveVisibleBlockCount else totalBlockCount,
        effectivePagination = effectivePagination,
        expandAllBlocks = expandAllBlocks,
    )
}

private fun longListEqual(a: List<Long>, b: List<Long>): Boolean {
    if (a.size != b.size) return false
    for (i in a.indices) {
        if (a[i] != b[i]) return false
    }
    return true
}

private fun initialVisibleBlockCount(initialBlockCount: Int, totalBlockCount: Int): Int {
    return initialBlockCount.coerceAtLeast(0).coerceAtMost(totalBlockCount.coerceAtLeast(0))
}
