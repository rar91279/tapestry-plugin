package com.intellij.tapestry.core.events

import com.intellij.tapestry.core.resource.TestableResource
import io.kotest.core.spec.style.FreeSpec
import io.mockk.mockk
import io.mockk.verify

class TapestryEventsManagerTest : FreeSpec({

    lateinit var fileListener: FileSystemListener
    lateinit var modelListener: TapestryModelChangeListener
    lateinit var eventsManager: TapestryEventsManager

    beforeTest {
        fileListener = mockk(relaxed = true)
        modelListener = mockk(relaxed = true)
        eventsManager = TapestryEventsManager()
    }

    "modelChanged" {
        eventsManager.addTapestryModelListener(modelListener)
        eventsManager.modelChanged()

        verify { modelListener.modelChanged() }
    }

    "classDeleted" {
        eventsManager.addFileSystemListener(fileListener)
        eventsManager.classDeleted("com.app.pages.Page1")

        verify { fileListener.classDeleted("com.app.pages.Page1") }
    }

    "fileContentsChanged" {
        val resource = TestableResource("", "")

        eventsManager.addFileSystemListener(fileListener)
        eventsManager.fileContentsChanged(resource)

        verify { fileListener.fileContentsChanged(resource) }
    }

    "classCreated" {
        eventsManager.addFileSystemListener(fileListener)
        eventsManager.classCreated("com.app.pages.Page1")

        verify { fileListener.classCreated("com.app.pages.Page1") }
    }

    "removeFileSystemListener" {
        eventsManager.addFileSystemListener(fileListener)
        eventsManager.classDeleted("com.app.pages.Page1")

        eventsManager.removeFileSystemListener(fileListener)
        eventsManager.classDeleted("com.app.pages.Page1")

        // still exactly once: the second fire must not reach the removed listener
        verify(exactly = 1) { fileListener.classDeleted("com.app.pages.Page1") }
    }

    "removeTapestryModelListener" {
        eventsManager.addTapestryModelListener(modelListener)
        eventsManager.modelChanged()

        eventsManager.removeTapestryModelListener(modelListener)
        eventsManager.modelChanged()

        verify(exactly = 1) { modelListener.modelChanged() }
    }

    "fileCreated" {
        eventsManager.addFileSystemListener(fileListener)
        eventsManager.fileCreated("some/path")

        verify { fileListener.fileCreated("some/path") }
    }

    "fileDeleted" {
        eventsManager.addFileSystemListener(fileListener)
        eventsManager.fileDeleted("some/path")

        verify { fileListener.fileDeleted("some/path") }
    }
})
