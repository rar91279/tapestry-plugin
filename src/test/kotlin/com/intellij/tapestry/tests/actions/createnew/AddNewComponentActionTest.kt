package com.intellij.tapestry.tests.actions.createnew

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiPackage
import com.intellij.tapestry.core.TapestryProject
import com.intellij.tapestry.intellij.TapestryModuleSupportLoader
import com.intellij.tapestry.intellij.actions.createnew.AddNewComponentAction
import com.intellij.tapestry.intellij.util.IdeaUtils
import com.intellij.tapestry.intellij.util.TapestryUtils
import com.intellij.tapestry.tests.core.EmptyFixtureSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.awt.event.InputEvent

class AddNewComponentActionTest : EmptyFixtureSpec({

    beforeTest {
        mockkStatic(TapestryUtils::class, TapestryModuleSupportLoader::class, IdeaUtils::class, JavaPsiFacade::class)
    }
    afterTest { unmockkAll() }

    "update_not_tapestry_module" {
        runReadAction {
            val projectMock = mockk<Project>(relaxed = true)
            val moduleMock = mockk<Module>(relaxed = true)
            every { moduleMock.project } returns projectMock
            every { TapestryUtils.isTapestryModule(moduleMock) } returns false

            val dataContext = SimpleDataContext.builder()
                .add(PlatformCoreDataKeys.MODULE, moduleMock)
                .build()
            val presentation = Presentation()
            val event = AnActionEvent(
                mockk<InputEvent>(relaxed = true), dataContext, "", presentation, ActionManager.getInstance(), 0
            )

            AddNewComponentAction().update(event)

            event.presentation.isEnabled shouldBe false
            event.presentation.isVisible shouldBe false
        }
    }

    "update_from_project_view_not_inside_components_package" {
        runReadAction {
            val projectMock = mockk<Project>(relaxed = true)
            val moduleMock = mockk<Module>(relaxed = true)
            every { moduleMock.project } returns projectMock
            every { TapestryUtils.isTapestryModule(moduleMock) } returns true

            val psiPackageMock = mockk<PsiPackage>(relaxed = true)
            every { psiPackageMock.qualifiedName } returns "com.app"

            val psiDirectoryMock = mockk<PsiDirectory>(relaxed = true)
            every { IdeaUtils.getPackage(psiDirectoryMock) } returns psiPackageMock

            val tapestryProjectMock = mockk<TapestryProject>(relaxed = true)
            every { TapestryModuleSupportLoader.getTapestryProject(moduleMock) } returns tapestryProjectMock
            every { tapestryProjectMock.applicationRootPackage } returns "com.app"
            every { tapestryProjectMock.componentsRootPackage } returns "com.app.components"

            val psiComponentsPackageMock = mockk<PsiPackage>(relaxed = true)
            every { psiComponentsPackageMock.qualifiedName } returns "com.app.components"
            every { JavaPsiFacade.getInstance(projectMock).findPackage("com.app.components") } returns psiComponentsPackageMock

            val dataContext = SimpleDataContext.builder()
                .add(PlatformCoreDataKeys.MODULE, moduleMock)
                .add(CommonDataKeys.PSI_ELEMENT, psiDirectoryMock)
                .build()
            val presentation = Presentation()
            val event = AnActionEvent(
                mockk<InputEvent>(relaxed = true), dataContext, "", presentation, ActionManager.getInstance(), 0
            )

            AddNewComponentAction().update(event)

            event.presentation.isEnabled shouldBe false
            event.presentation.isVisible shouldBe true
        }
    }

    "update_from_project_view_inside_components_package" {
        runReadAction {
            val projectMock = mockk<Project>(relaxed = true)
            val moduleMock = mockk<Module>(relaxed = true)
            every { moduleMock.project } returns projectMock
            every { TapestryUtils.isTapestryModule(moduleMock) } returns true

            val psiPackageMock = mockk<PsiPackage>(relaxed = true)
            every { psiPackageMock.qualifiedName } returns "com.app.components.test"

            val psiDirectoryMock = mockk<PsiDirectory>(relaxed = true)
            every { IdeaUtils.getPackage(psiDirectoryMock) } returns psiPackageMock

            val tapestryProjectMock = mockk<TapestryProject>(relaxed = true)
            every { TapestryModuleSupportLoader.getTapestryProject(moduleMock) } returns tapestryProjectMock
            every { tapestryProjectMock.applicationRootPackage } returns "com.app"
            every { tapestryProjectMock.componentsRootPackage } returns "com.app.components"

            val psiComponentsPackageMock = mockk<PsiPackage>(relaxed = true)
            every { psiComponentsPackageMock.qualifiedName } returns "com.app.components"
            every { JavaPsiFacade.getInstance(projectMock).findPackage("com.app.components") } returns psiComponentsPackageMock

            val dataContext = SimpleDataContext.builder()
                .add(PlatformCoreDataKeys.MODULE, moduleMock)
                .add(CommonDataKeys.PSI_ELEMENT, psiDirectoryMock)
                .build()
            val presentation = Presentation()
            val event = AnActionEvent(
                mockk<InputEvent>(relaxed = true), dataContext, "", presentation, ActionManager.getInstance(), 0
            )

            AddNewComponentAction().update(event)

            event.presentation.isEnabled shouldBe true
            event.presentation.isVisible shouldBe true
        }
    }
})
