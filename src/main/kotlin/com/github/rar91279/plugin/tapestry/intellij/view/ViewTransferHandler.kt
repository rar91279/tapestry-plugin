package com.github.rar91279.plugin.tapestry.intellij.view

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.github.rar91279.plugin.tapestry.core.model.externalizable.ExternalizableToClass
import com.github.rar91279.plugin.tapestry.core.model.externalizable.ExternalizableToTemplate
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaClassType
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.TapestryNode
import com.github.rar91279.plugin.tapestry.lang.TmlFileType
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import javax.swing.JComponent
import javax.swing.TransferHandler
import javax.swing.tree.DefaultMutableTreeNode

/**
 * Handles the drag&drop of Tapestry view elements.
 */
class ViewTransferHandler(private val viewPane: TapestryProjectViewPane) : TransferHandler() {

    override fun createTransferable(c: JComponent): Transferable {
        val node = viewPane.tree.selectionPath!!.lastPathComponent as DefaultMutableTreeNode
        return TapestryElementTransferable((node.userObject as TapestryNode).getValue())
    }

    override fun getSourceActions(c: JComponent): Int = COPY

    private inner class TapestryElementTransferable(private val data: Any?) : Transferable {

        override fun getTransferData(flavor: DataFlavor): Any {
            if (!isDataFlavorSupported(flavor)) {
                throw UnsupportedFlavorException(flavor)
            }

            val fileInEditor = PsiManager.getInstance(viewPane.project)
                .findFile(FileDocumentManager.getInstance().getFile(FileEditorManager.getInstance(viewPane.project).selectedTextEditor!!.document)!!)!!
            val typeFileInEditor = fileInEditor.fileType

            if (fileInEditor is PsiClassOwner && data is ExternalizableToClass) {
                val dropClass = IntellijJavaClassType(
                    viewPane.getSelectedModule() ?: throw UnsupportedFlavorException(flavor),
                    IdeaUtils.findPublicClass(fileInEditor)!!.containingFile
                )

                try {
                    return data.getClassRepresentation(dropClass) ?: throw UnsupportedFlavorException(flavor)
                } catch (ex: Exception) {
                    logger.error(ex)
                    throw UnsupportedFlavorException(flavor)
                }
            }

            if (typeFileInEditor == TmlFileType && data is ExternalizableToTemplate) {
                try {
                    return data.getTemplateRepresentation(TapestryUtils.getTapestryNamespacePrefix(fileInEditor as XmlFile))
                        ?: throw UnsupportedFlavorException(flavor)
                } catch (ex: Exception) {
                    logger.error(ex)
                    throw UnsupportedFlavorException(flavor)
                }
            }

            throw UnsupportedFlavorException(flavor)
        }

        override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.stringFlavor)

        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = DataFlavor.stringFlavor == flavor
    }

    companion object {
        private val logger = Logger.getInstance(ViewTransferHandler::class.java)
    }
}
