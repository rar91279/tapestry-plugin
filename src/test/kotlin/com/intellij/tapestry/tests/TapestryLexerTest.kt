package com.intellij.tapestry.tests

import com.intellij.lexer.Lexer
import com.intellij.psi.tree.IElementType
import com.intellij.tapestry.psi.TmlHighlightingLexer
import com.intellij.tapestry.psi.TmlLexer
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory

/**
 * @author Alexey Chmutov
 */
class TapestryLexerTest : UsefulTestCase() {

    fun testTemplateNoEL() {
        doTest()
    }

    fun testSingleELInAttributeValue() {
        doTest()
    }

    fun testELHighlightingInXmlText() {
        doHighlightingTest()
    }

    fun testRangeOpHighlighting() {
        doHighlightingTest()
    }

    fun testELHighlightingInAttr() {
        doHighlightingTest()
    }


    private fun doTest() {
        doTest(TmlLexer())
    }

    private fun doHighlightingTest() {
        doTest(TmlHighlightingLexer())
    }

    private fun doTest(lexer: Lexer) {
        doTest(lexer, getTestInput(), getExpectedTextFilePath())
    }

    private fun getTestInput(): String {
        return Util.getFileText(getDataSubpath() + getTestName(false) + Util.DOT_TML)
    }

    private fun getExpectedTextFilePath(): String {
        return getDataSubpath() + getTestName(false) + Util.DOT_EXPECTED
    }

    private fun doTest(lexer: Lexer, testText: String, expectedTextFileName: String) {
        lexer.start(testText)
        var result = ""
        while (true) {
            val tokenType: IElementType = lexer.tokenType ?: break
            val tokenText = getTokenText(lexer)
            val tokenTypeName = tokenType.toString()
            val line = "$tokenTypeName ('$tokenText')\n"
            result += line
            lexer.advance()
        }
        //if (!(new File(expectedTextFileName).exists())) {
        //  final FileWriter writer = new FileWriter(expectedTextFileName);
        //  writer.write(result);
        //  writer.close();
        //}
        assertSameLinesWithFile(expectedTextFileName, result)
    }

    protected fun getDataSubpath(): String {
        return Util.getCommonTestDataPath() + "lexer/"
    }


    private lateinit var myFixture: IdeaProjectTestFixture

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        // needed for various XML extension points registration
        myFixture = IdeaTestFixtureFactory.getFixtureFactory()
            .createLightFixtureBuilder(LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR, getTestName(false)).fixture
        myFixture.setUp()
    }

    @Throws(Exception::class)
    override fun tearDown() {
        try {
            myFixture.tearDown()
        } catch (e: Throwable) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
        }
    }

    companion object {
        private fun getTokenText(lexer: Lexer): String {
            return lexer.bufferSequence.subSequence(lexer.tokenStart, lexer.tokenEnd).toString()
        }
    }
}
