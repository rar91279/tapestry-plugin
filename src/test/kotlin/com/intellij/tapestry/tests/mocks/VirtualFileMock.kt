package com.intellij.tapestry.tests.mocks

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import java.io.InputStream
import java.io.OutputStream

class VirtualFileMock : VirtualFile() {

    private var _url: String? = null

    override fun getName(): String = throw UnsupportedOperationException()

    override fun getUrl(): String = _url!!

    fun setUrl(url: String?): VirtualFileMock {
        _url = url
        return this
    }

    override fun getFileSystem(): VirtualFileSystem = throw UnsupportedOperationException()

    override fun getPath(): String = throw UnsupportedOperationException()

    override fun isWritable(): Boolean = false

    override fun isDirectory(): Boolean = false

    override fun isValid(): Boolean = false

    override fun getParent(): VirtualFile? = null

    override fun getChildren(): Array<VirtualFile> = VirtualFile.EMPTY_ARRAY

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream =
        throw UnsupportedOperationException()

    override fun contentsToByteArray(): ByteArray = ByteArray(0)

    override fun getTimeStamp(): Long = 0

    override fun getLength(): Long = 0

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {
    }

    override fun getInputStream(): InputStream = throw UnsupportedOperationException()
}
