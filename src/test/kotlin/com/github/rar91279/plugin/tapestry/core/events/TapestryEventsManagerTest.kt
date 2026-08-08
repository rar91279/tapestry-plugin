package com.github.rar91279.plugin.tapestry.core.events

import com.github.rar91279.plugin.tapestry.core.mocks.psiFileMock
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import io.kotest.core.spec.style.FreeSpec
import io.mockk.mockk
import io.mockk.verify

class TapestryEventsManagerTest : FreeSpec({

    lateinit var fileListener: FileSystemListener
    lateinit var modelListener: TapestryModelChangeListener
    lateinit var eventsManager: TapestryEventsManager
    lateinit var parent: Disposable

    beforeTest {
        fileListener = mockk(relaxed = true)
        modelListener = mockk(relaxed = true)
        eventsManager = TapestryEventsManager()
        parent = Disposer.newDisposable("TapestryEventsManagerTest")
    }

    afterTest {
        Disposer.dispose(parent)
    }

    "modelChanged" {
        eventsManager.addTapestryModelListener(modelListener, parent)
        eventsManager.modelChanged()

        verify { modelListener.modelChanged() }
    }

    "classDeleted" {
        eventsManager.addFileSystemListener(fileListener, parent)
        eventsManager.classDeleted("com.app.pages.Page1")

        verify { fileListener.classDeleted("com.app.pages.Page1") }
    }

    "fileContentsChanged" {
        val resource = psiFileMock("Home.tml")

        eventsManager.addFileSystemListener(fileListener, parent)
        eventsManager.fileContentsChanged(resource)

        verify { fileListener.fileContentsChanged(resource) }
    }

    "classCreated" {
        eventsManager.addFileSystemListener(fileListener, parent)
        eventsManager.classCreated("com.app.pages.Page1")

        verify { fileListener.classCreated("com.app.pages.Page1") }
    }

    "fileCreated" {
        eventsManager.addFileSystemListener(fileListener, parent)
        eventsManager.fileCreated("some/path")

        verify { fileListener.fileCreated("some/path") }
    }

    "fileDeleted" {
        eventsManager.addFileSystemListener(fileListener, parent)
        eventsManager.fileDeleted("some/path")

        verify { fileListener.fileDeleted("some/path") }
    }

    "disposing the parent unregisters a file system listener" {
        eventsManager.addFileSystemListener(fileListener, parent)
        eventsManager.classDeleted("com.app.pages.Page1")

        Disposer.dispose(parent)
        eventsManager.classDeleted("com.app.pages.Page1")

        // still exactly once: the second fire must not reach the disposed listener
        verify(exactly = 1) { fileListener.classDeleted("com.app.pages.Page1") }
    }

    "disposing the parent unregisters a model change listener" {
        eventsManager.addTapestryModelListener(modelListener, parent)
        eventsManager.modelChanged()

        Disposer.dispose(parent)
        eventsManager.modelChanged()

        verify(exactly = 1) { modelListener.modelChanged() }
    }

    "registering a file system listener twice notifies it once" {
        eventsManager.addFileSystemListener(fileListener, parent)
        eventsManager.addFileSystemListener(fileListener, parent)

        eventsManager.fileCreated("some/path")

        verify(exactly = 1) { fileListener.fileCreated("some/path") }
    }

    "registering a model change listener twice notifies it once" {
        eventsManager.addTapestryModelListener(modelListener, parent)
        eventsManager.addTapestryModelListener(modelListener, parent)

        eventsManager.modelChanged()

        verify(exactly = 1) { modelListener.modelChanged() }
    }
})
