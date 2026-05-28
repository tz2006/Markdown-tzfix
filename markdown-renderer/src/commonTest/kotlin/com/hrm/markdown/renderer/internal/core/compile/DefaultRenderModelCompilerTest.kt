package com.hrm.markdown.renderer.internal.core.compile

import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.parser.ast.Paragraph
import com.hrm.markdown.parser.ast.TableHead
import com.hrm.markdown.parser.ast.Text
import com.hrm.markdown.renderer.internal.core.model.FallbackContainerBlockModel
import com.hrm.markdown.renderer.internal.core.model.FallbackLeafBlockModel
import com.hrm.markdown.renderer.internal.core.model.ParagraphBlockModel
import kotlin.test.Test
import kotlin.test.assertIs

class DefaultRenderModelCompilerTest {
    @Test
    fun should_compile_unknown_container_node_into_internal_fallback_container() {
        val document = Document().apply {
            appendChild(
                TableHead().apply {
                    appendChild(
                        Paragraph().apply {
                            appendChild(Text("fallback child"))
                        }
                    )
                }
            )
        }

        val renderDocument = DefaultRenderModelCompiler.compile(document, RenderCompileEnvironment())

        val fallback = assertIs<FallbackContainerBlockModel>(renderDocument.blocks.single())
        assertIs<ParagraphBlockModel>(fallback.children.single())
    }

    @Test
    fun should_compile_unknown_leaf_node_into_internal_fallback_leaf() {
        val document = Document().apply {
            appendChild(Text("orphan inline"))
        }

        val renderDocument = DefaultRenderModelCompiler.compile(document, RenderCompileEnvironment())

        assertIs<FallbackLeafBlockModel>(renderDocument.blocks.single())
    }
}
