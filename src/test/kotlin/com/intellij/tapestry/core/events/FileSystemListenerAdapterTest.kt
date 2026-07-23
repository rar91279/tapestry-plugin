package com.intellij.tapestry.core.events

import io.kotest.core.spec.style.FreeSpec

private class TestableFileSystemListenerAdapter : FileSystemListenerAdapter()

class FileSystemListenerAdapterTest : FreeSpec({

    "testAll" {
        val adapter: FileSystemListenerAdapter = TestableFileSystemListenerAdapter()
        adapter.classCreated(null)
        adapter.classDeleted(null)
        adapter.fileCreated(null)
        adapter.fileDeleted(null)
    }
})
