package com.github.rar91279.plugin.tapestry.tests

import com.github.rar91279.plugin.tapestry.core.events.FileSystemListener
import com.github.rar91279.plugin.tapestry.core.events.TapestryEventsManager
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.view.TapestryProjectViewPane
import com.intellij.openapi.util.Disposer
import junit.framework.Assert

/**
 * The events manager lives on the module and outlives the UI components that listen to it, so a listener that
 * fails to unregister keeps receiving events on a dead component. These tests cover that wiring end to end
 * against a real module's manager — the pure unit specs in `core/events` only cover the manager in isolation.
 */
class TapestryEventsListenerLifecycleTest : TapestryBaseTestCase() {

    override fun getBasePath(): String = "events/"

    private val eventsManager: TapestryEventsManager
        get() = TapestryModuleSupportLoader.getTapestryProject(myModule)!!.eventsManager

    private class RecordingListener : FileSystemListener {
        var classCreatedCount = 0
        override fun classCreated(classFqn: String?) {
            classCreatedCount++
        }
    }

    fun testListenerStopsReceivingEventsOnceItsParentIsDisposed() {
        val listener = RecordingListener()
        val parent = Disposer.newDisposable("test parent")

        eventsManager.addFileSystemListener(listener, parent)
        eventsManager.classCreated("com.testapp.pages.Page1")
        Assert.assertEquals("listener should be notified while registered", 1, listener.classCreatedCount)

        Disposer.dispose(parent)
        eventsManager.classCreated("com.testapp.pages.Page1")
        Assert.assertEquals("disposed listener must not be notified", 1, listener.classCreatedCount)
    }

    fun testRegisteringTheSameListenerTwiceNotifiesItOnce() {
        val listener = RecordingListener()
        val parent = Disposer.newDisposable("test parent")
        try {
            eventsManager.addFileSystemListener(listener, parent)
            eventsManager.addFileSystemListener(listener, parent)

            eventsManager.classCreated("com.testapp.pages.Page1")

            Assert.assertEquals("duplicate registration must not double-notify", 1, listener.classCreatedCount)
        } finally {
            Disposer.dispose(parent)
        }
    }

    /**
     * The pane subscribes itself in its constructor. Disposing it must drop those subscriptions from every
     * module's manager — the manager outlives the pane, so a missed unregistration is a leak.
     */
    fun testProjectViewPaneUnregistersItselfWhenDisposed() {
        val pane = TapestryProjectViewPane(myFixture.project)
        try {
            Assert.assertTrue(
                "pane should have subscribed to the module's events manager in its constructor",
                eventsManager.hasFileSystemListener(pane)
            )
            Assert.assertTrue(
                "pane should have subscribed for model changes too",
                eventsManager.hasTapestryModelListener(pane)
            )
        } catch (e: Throwable) {
            Disposer.dispose(pane)
            throw e
        }

        Disposer.dispose(pane)

        Assert.assertFalse(
            "disposed pane must no longer be a file system listener",
            eventsManager.hasFileSystemListener(pane)
        )
        Assert.assertFalse(
            "disposed pane must no longer be a model change listener",
            eventsManager.hasTapestryModelListener(pane)
        )
    }
}
