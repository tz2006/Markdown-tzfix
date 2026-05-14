package com.hrm.markdown.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hrm.markdown.renderer.Markdown

internal val paginationPreviewGroups = listOf(
    PreviewGroup(
        id = "pagination_demo",
        title = "分页加载演示",
        description = "选择能力开启时的分页渲染，以及禁用选择后的 LazyColumn 渲染",
        items = listOf(
            PreviewItem(
                id = "pagination_200",
                title = "200 段落分页加载",
                content = { PaginationDemo() }
            ),
            PreviewItem(
                id = "lazy_unselectable_200",
                title = "禁用选中后自动 Lazy",
                content = { UnselectableLazyDemo() }
            ),
        )
    ),
    PreviewGroup(
        id = "selection_disabled",
        title = "禁用选择验证",
        description = "专门验证 enableSelection=false 时文本不可选中，但普通交互仍可用",
        items = listOf(
            PreviewItem(
                id = "selection_disabled_basic",
                title = "长按/拖拽选择禁用",
                content = { EnableSelectionFalsePreview() }
            ),
        )
    ),
    PreviewGroup(
        id = "pagination_config",
        title = "不同初始加载数",
        description = "调整 initialBlockCount 参数",
        items = listOf(
            PreviewItem(
                id = "pagination_20",
                title = "初始 20 块",
                content = { PaginationDemoWithConfig(blockCount = 20) }
            ),
            PreviewItem(
                id = "pagination_100",
                title = "初始 100 块",
                content = { PaginationDemoWithConfig(blockCount = 100) }
            ),
        )
    ),
)

@Composable
private fun PaginationDemo() {
    val longDoc = buildString {
        repeat(200) { append("## 段落 $it\n\n这是第 $it 段内容。".repeat(3) + "\n\n") }
    }
    Column {
        Text(
            "分页加载 Demo - 200 个段落",
            Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
        Markdown(
            longDoc,
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            enablePagination = true,
            initialBlockCount = 50
        )
    }
}

@Composable
private fun UnselectableLazyDemo() {
    val longDoc = buildString {
        repeat(200) { append("## Lazy 段落 $it\n\n这是第 $it 段内容，禁用文本选中后将自动使用 LazyColumn 渲染。\n\n") }
    }
    Column {
        Text(
            "禁用选中后自动 LazyColumn",
            Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
        Markdown(
            longDoc,
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            enableSelection = false,
        )
    }
}

@Composable
private fun EnableSelectionFalsePreview() {
    val markdown = """
        # enableSelection = false
        
        请在这段内容上尝试长按、拖拽或复制选择，预期不会出现文本选中手柄。
        
        同时保留普通渲染与交互能力：
        - 这是普通段落文本
        - 这是一个 [可点击链接](https://github.com/huarangmeng/Markdown)
        - 这是较长的内容，用于验证禁用选择时仍能正常滚动和阅读
        
        > 这个 preview 的目标不是验证分页，而是直接验证“不可选中”这个用户感知行为。
        
        继续补充一些文本，方便你反复尝试：
        
        Kotlin Multiplatform Markdown renderer should remain readable and interactive even when text selection is disabled.
        
        再来一段中文，确保中英文混排场景下也不会弹出文本选择相关 UI。
    """.trimIndent()
    Column {
        Text(
            "验证文本不可选中，链接点击仍可用",
            Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
        Markdown(
            markdown,
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            enableSelection = false,
        )
    }
}

@Composable
private fun PaginationDemoWithConfig(blockCount: Int) {
    val longDoc = buildString {
        repeat(100) { append("## 段落 $it\n\n这是第 $it 段内容，用于测试 initialBlockCount=$blockCount 的效果。\n\n") }
    }
    Column {
        Text(
            "分页加载 - initialBlockCount=$blockCount",
            Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
        Markdown(
            longDoc,
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            enablePagination = true,
            initialBlockCount = blockCount
        )
    }
}
