package com.github.rar91279.plugin.tapestry.tests.core.util

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.runReadAction
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils
import com.github.rar91279.plugin.tapestry.tests.core.JavaModuleFixtureSpec
import com.intellij.testFramework.MapDataContext
import io.kotest.matchers.shouldBe

class IdeaUtilsTest : JavaModuleFixtureSpec({

    "isModuleNode" {
        runReadAction {
            val dataContext = MapDataContext()
            dataContext.put(CommonDataKeys.PROJECT.name, fixture.project)
            dataContext.put(LangDataKeys.MODULE_CONTEXT.name, module)

            var actionEvent = AnActionEvent(null, dataContext, "", Presentation(), ActionManager.getInstance(), 0)
            IdeaUtils.isModuleNode(actionEvent) shouldBe true

            dataContext.put(CommonDataKeys.PROJECT.name, null)
            dataContext.put(LangDataKeys.MODULE_CONTEXT.name, null)

            actionEvent = AnActionEvent(null, dataContext, "", Presentation(), ActionManager.getInstance(), 0)
            IdeaUtils.isModuleNode(actionEvent) shouldBe false
        }
    }
})
