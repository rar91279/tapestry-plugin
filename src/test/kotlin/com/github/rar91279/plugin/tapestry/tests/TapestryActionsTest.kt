package com.github.rar91279.plugin.tapestry.tests

import com.intellij.codeInsight.actions.MultiCaretCodeInsightAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.github.rar91279.plugin.tapestry.intellij.actions.navigation.ClassTemplateNavigation
import com.intellij.testFramework.EditorTestUtil
import junit.framework.Assert

/**
 * @author Alexey Chmutov
 */
class TapestryActionsTest : TapestryBaseTestCase() {

    override fun getBasePath(): String = "actions/"

    fun testNavigateToTemplate() {
        val tmlFile = initByComponent(true)
        val psiFile = myFixture.psiManager.findFile(tmlFile)
        Assert.assertNotNull("No PsiFile for template", psiFile)
        val fileFoundByAction =
            ClassTemplateNavigation.findNavigationTarget(psiFile!!, myFixture.module, "Class <-> Template Navigation")
        Assert.assertNotNull("Java file not found by the action", fileFoundByAction)
        Assert.assertEquals(getElementClassFileName(), fileFoundByAction!!.name)
    }

    fun testNavigateToClass() {
        val javaFile = initByComponent(false)
        val psiFile = myFixture.psiManager.findFile(javaFile)
        Assert.assertNotNull("No PsiFile for java file", psiFile)
        val fileFoundByAction =
            ClassTemplateNavigation.findNavigationTarget(psiFile!!, myFixture.module, "Class <-> Template Navigation")
        Assert.assertNotNull("Template file not found by the action", fileFoundByAction)
        Assert.assertEquals(getElementTemplateFileName(), fileFoundByAction!!.name)
    }

    fun testNavigateToTemplateFromSuper() {
        val pageTemplates = addPageToProject("StartPage")
        val javaFile = initByComponent(false)

        val psiFile = myFixture.psiManager.findFile(javaFile)
        Assert.assertNotNull("No PsiFile for java file", psiFile)
        val fileFoundByAction =
            ClassTemplateNavigation.findNavigationTarget(psiFile!!, myFixture.module, "Class <-> Template Navigation")
        Assert.assertNotNull("Template file not found by the action", fileFoundByAction)
        Assert.assertEquals(pageTemplates.name, fileFoundByAction!!.name)
    }

    fun testCommentBlock() {
        doTest(IdeActions.ACTION_COMMENT_BLOCK)
    }

    fun testCommentLine() {
        doTest(IdeActions.ACTION_COMMENT_LINE)
    }

    fun testUncommentBlock() {
        doTest(IdeActions.ACTION_COMMENT_BLOCK)
    }

    fun testUncommentLine() {
        doTest(IdeActions.ACTION_COMMENT_LINE)
    }

    fun testInsertPairingRBrace() {
        initByComponent(true)
        EditorTestUtil.performTypingAction(myFixture.editor, '{')
        checkResultByFile()
    }

    fun testInsertPairingRBrace2() {
        initByComponent(true)
        EditorTestUtil.performTypingAction(myFixture.editor, '{')
        checkResultByFile()
    }

    private fun doTest(actionId: String) {
        initByComponent(true)
        val action = ActionManager.getInstance().getAction(actionId) as MultiCaretCodeInsightAction
        action.actionPerformedImpl(myModule!!.project, myFixture.editor)
        checkResultByFile()
    }
}
