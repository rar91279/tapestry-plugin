package com.github.rar91279.plugin.tapestry.tests.actions.createnew

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiPackage
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.actions.createnew.action.AddNewComponentAction
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.github.rar91279.plugin.tapestry.tests.core.EmptyFixtureSpec
import com.intellij.openapi.application.readActionBlocking
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.awt.event.InputEvent

/**
 * Tests for [AddNewComponentAction].
 *
 * Verifies that the action correctly updates its presentation (enabled/visible state)
 * based on the current context, including module type and selected package location
 * relative to the Tapestry components package.
 */
class AddNewComponentActionTest : EmptyFixtureSpec({

    /**
     * Sets up mocks before each test.
     *
     * Mocks utility objects and static methods used by AddNewComponentAction to control
     * behavior during tests. Uses mockkObject for Kotlin objects (which have different
     * call-site behavior than Java statics) and mockkStatic for genuine JVM static methods.
     */
    beforeTest {
        // mockkObject for our own Kotlin objects/companions: it intercepts the members Kotlin call sites
        // actually invoke. mockkStatic only replaces JVM static bridges, which exist solely for Java callers
        // and which this (Java-free) plugin no longer generates.
        mockkObject(TapestryUtils, IdeaUtils, TapestryModuleSupportLoader.Companion)
        // JavaPsiFacade is a platform Java class with a genuine static getInstance.
        mockkStatic(JavaPsiFacade::class)
    }
    /**
     * Cleans up all mocks after each test.
     */
    afterTest { unmockkAll() }

    /**
     * Verifies that the action is disabled and hidden when invoked on a non-Tapestry module.
     */
    "update_not_tapestry_module" {
        readActionBlocking {
            val projectMock = mockk<Project>(relaxed = true)
            val moduleMock = mockk<Module>(relaxed = true)
            every { moduleMock.project } returns projectMock
            every { TapestryUtils.isTapestryModule(moduleMock) } returns false

            val dataContext = SimpleDataContext.builder()
                .add(PlatformCoreDataKeys.MODULE, moduleMock)
                .build()
            val presentation = Presentation()
            val event = AnActionEvent(
                dataContext,
                presentation,
                "",
                ActionUiKind.NONE,
                mockk<InputEvent>(relaxed = true),
                0,
                ActionManager.getInstance()
            )

            AddNewComponentAction().update(event)

            event.presentation.isEnabled shouldBe false
            event.presentation.isVisible shouldBe false
        }
    }

    /**
     * Verifies that the action is enabled and visible when invoked from an ancestor package
     * of the components package (e.g., the application root package).
     *
     * The action deliberately enables on ancestor packages so that "New -> Tapestry Component"
     * remains available without drilling down to the exact components folder.
     */
    "update_from_project_view_at_app_root_package" {
        readActionBlocking {
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
            every {
                JavaPsiFacade.getInstance(projectMock).findPackage("com.app.components")
            } returns psiComponentsPackageMock

            val dataContext = SimpleDataContext.builder()
                .add(PlatformCoreDataKeys.MODULE, moduleMock)
                .add(CommonDataKeys.PSI_ELEMENT, psiDirectoryMock)
                .build()
            val presentation = Presentation()
            val event = AnActionEvent(
                dataContext,
                presentation,
                "",
                ActionUiKind.NONE,
                mockk<InputEvent>(relaxed = true),
                0,
                ActionManager.getInstance()
            )

            AddNewComponentAction().update(event)

            event.presentation.isEnabled shouldBe true
            event.presentation.isVisible shouldBe true
        }
    }

    /**
     * Verifies that the action is disabled but visible when invoked from a package that is
     * neither an ancestor nor a descendant of the components package (e.g., a services package).
     */
    "update_from_project_view_unrelated_package" {
        readActionBlocking {
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
            every {
                JavaPsiFacade.getInstance(projectMock).findPackage("com.app.components")
            } returns psiComponentsPackageMock

            val dataContext = SimpleDataContext.builder()
                .add(PlatformCoreDataKeys.MODULE, moduleMock)
                .add(CommonDataKeys.PSI_ELEMENT, psiDirectoryMock)
                .build()
            val presentation = Presentation()
            val event = AnActionEvent(
                dataContext,
                presentation,
                "",
                ActionUiKind.NONE,
                mockk<InputEvent>(relaxed = true),
                0,
                ActionManager.getInstance()
            )

            AddNewComponentAction().update(event)

            event.presentation.isEnabled shouldBe false
            event.presentation.isVisible shouldBe true
        }
    }

    /**
     * Verifies that the action is enabled and visible when invoked from a package inside
     * the components package (e.g., a subpackage like "com.app.components.test").
     */
    "update_from_project_view_inside_components_package" {
        readActionBlocking {
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
            every {
                JavaPsiFacade.getInstance(projectMock).findPackage("com.app.components")
            } returns psiComponentsPackageMock

            val dataContext = SimpleDataContext.builder()
                .add(PlatformCoreDataKeys.MODULE, moduleMock)
                .add(CommonDataKeys.PSI_ELEMENT, psiDirectoryMock)
                .build()
            val presentation = Presentation()
            val event = AnActionEvent(
                dataContext,
                presentation,
                "",
                ActionUiKind.NONE,
                mockk<InputEvent>(relaxed = true),
                0,
                ActionManager.getInstance()
            )

            AddNewComponentAction().update(event)

            event.presentation.isEnabled shouldBe true
            event.presentation.isVisible shouldBe true
        }
    }
})
