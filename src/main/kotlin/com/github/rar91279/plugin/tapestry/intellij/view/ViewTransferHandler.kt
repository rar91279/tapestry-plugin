package com.github.rar91279.plugin.tapestry.intellij.view

import com.github.rar91279.plugin.tapestry.core.model.externalizable.ExternalizableToClass
import com.github.rar91279.plugin.tapestry.core.model.externalizable.ExternalizableToTemplate
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.github.rar91279.plugin.tapestry.intellij.util.currentPsiFileInEditor
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.TapestryNode
import com.github.rar91279.plugin.tapestry.lang.TmlFileType
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.xml.XmlFile
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import javax.swing.JComponent
import javax.swing.TransferHandler

/**
 * Handles the drag&drop of Tapestry view elements.
 */
class ViewTransferHandler(private val viewPane: TapestryProjectViewPane) : TransferHandler() {

    override fun createTransferable(c: JComponent): Transferable {
        // Same reason as in the pane and the safe-delete provider: a tree restoring itself from a cached
        // presentation hands out placeholder nodes, and a hard cast throws on the first drag after startup.
        val node = IdeaUtils.nodeOf(viewPane.tree.selectionPath)
        return TapestryElementTransferable((node?.userObject as? TapestryNode)?.getValue())
    }

    override fun getSourceActions(c: JComponent): Int = COPY

    private inner class TapestryElementTransferable(private val data: Any?) : Transferable {

        override fun getTransferData(flavor: DataFlavor): Any {
            if (!isDataFlavorSupported(flavor)) {
                throw UnsupportedFlavorException(flavor)
            }

            val fileInEditor = currentPsiFileInEditor(viewPane.project) ?: throw UnsupportedFlavorException(flavor)
            val typeFileInEditor = fileInEditor.fileType

            if (fileInEditor is PsiClassOwner && data is ExternalizableToClass) {
                val dropClass = IdeaUtils.findPublicClass(fileInEditor) ?: throw UnsupportedFlavorException(flavor)

                try {
                    return data.getClassRepresentation(dropClass) ?: throw UnsupportedFlavorException(flavor)
                } catch (ex: Exception) {
                    if (ex is ControlFlowException) throw ex
                    // A failed drag is not an IDE bug: warn rather than raising a fatal-error balloon.
                    logger.warn("Failed to build the class representation for drop", ex)
                    throw UnsupportedFlavorException(flavor)
                }
            }

            if (typeFileInEditor == TmlFileType && data is ExternalizableToTemplate) {
                try {
                    return data.getTemplateRepresentation(TapestryUtils.getTapestryNamespacePrefix(fileInEditor as XmlFile))
                        ?: throw UnsupportedFlavorException(flavor)
                } catch (ex: Exception) {
                    if (ex is ControlFlowException) throw ex
                    logger.warn("Failed to build the template representation for drop", ex)
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
