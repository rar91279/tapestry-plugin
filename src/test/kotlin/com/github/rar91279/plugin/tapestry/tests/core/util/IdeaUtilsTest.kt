package com.github.rar91279.plugin.tapestry.tests.core.util

import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.readActionBlocking
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils
import com.github.rar91279.plugin.tapestry.tests.core.JavaModuleFixtureSpec
import io.kotest.matchers.shouldBe

/**
 * Test suite for [IdeaUtils] utility methods.
 *
 * This test class verifies the behavior of utility methods in the IdeaUtils class,
 * specifically focusing on action event handling and module node detection.
 */
class IdeaUtilsTest : JavaModuleFixtureSpec({

    /**
     * Creates an [AnActionEvent] for testing purposes.
     *
     * @param dataContext the data context to associate with the event
     * @return a new AnActionEvent instance with the provided context
     */
    fun eventFor(dataContext: DataContext) =
        AnActionEvent.createEvent(dataContext, Presentation(), "", ActionUiKind.NONE, null)

    /**
     * Tests the [IdeaUtils.isModuleNode] method.
     *
     * Verifies that the method correctly identifies when an action event
     * represents a module node by checking:
     * - Returns true when the event contains a valid module context
     * - Returns false when the event has an empty data context
     */
    "isModuleNode" {
        readActionBlocking {
            val moduleContext = SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, fixture.project)
                .add(LangDataKeys.MODULE_CONTEXT, module)
                .build()

            IdeaUtils.isModuleNode(eventFor(moduleContext)) shouldBe true
            IdeaUtils.isModuleNode(eventFor(DataContext.EMPTY_CONTEXT)) shouldBe false
        }
    }
})
