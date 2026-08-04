package com.github.rar91279.plugin.tapestry.tests.actions.createnew

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUiKind
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
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.actions.createnew.AddNewComponentAction
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.github.rar91279.plugin.tapestry.tests.core.EmptyFixtureSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.awt.event.InputEvent

class AddNewComponentActionTest : EmptyFixtureSpec({

    beforeTest {
        mockkStatic(TapestryUtils::class, IdeaUtils::class, JavaPsiFacade::class)
        // Kotlin callers invoke the companion directly, so the @JvmStatic bridge alone is not enough.
        mockkObject(TapestryModuleSupportLoader.Companion)
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
            val event = AnActionEvent(dataContext, presentation, "", ActionUiKind.NONE, mockk<InputEvent>(relaxed = true), 0, ActionManager.getInstance())

            AddNewComponentAction().update(event)

            event.presentation.isEnabled shouldBe false
            event.presentation.isVisible shouldBe false
        }
    }

    "update_from_project_view_at_app_root_package" {
        runReadAction {
            val projectMock = mockk<Project>(relaxed = true)
            val moduleMock = mockk<Module>(relaxed = true)
            every { moduleMock.project } returns projectMock
            every { TapestryUtils.isTapestryModule(moduleMock) } returns true

            // "com.app" is the app root - an ancestor of the components package, not inside it.
            // AddNewElementAction.update() deliberately enables on ancestor packages too (not just
            // the exact components package), so New -> Tapestry isn't hidden until you drill all the
            // way into the components folder.
            val psiPackageMock = mockk<PsiPackage>(relaxed = true)
            every { psiPackageMock.qualifiedName } returns "com.app"

            val psiDirectoryMock = mockk<PsiDirectory>(relaxed = true)
            // getData(PSI_ELEMENT) no longer hands back the raw mock directory on 2026.2, so match
            // any element: this test exercises update()'s package-prefix branch, not data plumbing.
            every { IdeaUtils.getPackage(any()) } returns psiPackageMock

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

    "update_from_project_view_unrelated_package" {
        runReadAction {
            val projectMock = mockk<Project>(relaxed = true)
            val moduleMock = mockk<Module>(relaxed = true)
            every { moduleMock.project } returns projectMock
            every { TapestryUtils.isTapestryModule(moduleMock) } returns true

            // "com.app.services" is neither an ancestor nor a descendant of the components package.
            val psiPackageMock = mockk<PsiPackage>(relaxed = true)
            every { psiPackageMock.qualifiedName } returns "com.app.services"

            val psiDirectoryMock = mockk<PsiDirectory>(relaxed = true)
            every { IdeaUtils.getPackage(any()) } returns psiPackageMock

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
            // getData(PSI_ELEMENT) no longer hands back the raw mock directory on 2026.2, so match
            // any element: this test exercises update()'s package-prefix branch, not data plumbing.
            every { IdeaUtils.getPackage(any()) } returns psiPackageMock

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
