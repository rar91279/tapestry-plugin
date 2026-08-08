package com.github.rar91279.plugin.tapestry.intellij

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope

/**
 * Holds the plugin's project-level [CoroutineScope].
 *
 * The platform injects and owns this scope: it is cancelled when the project closes, which is the
 * cancellation story for every coroutine the plugin launches. Never replace it with `GlobalScope` or an
 * ad-hoc `CoroutineScope(...)` — those outlive the project and leak.
 *
 * Work belonging to a UI component that dies before the project does (a tool-window tab, the view pane)
 * should run in a child scope tied to that component's `Disposable` instead, so it is cancelled with the
 * component:
 *
 * ```kotlin
 * private val uiScope = project.tapestryScope.childScope("DocumentationTab")
 * init { Disposer.register(this) { uiScope.cancel() } }
 * ```
 */
@Service(Service.Level.PROJECT)
class TapestryCoroutineService(val scope: CoroutineScope)

/** The plugin's project-level coroutine scope. Cancelled automatically when the project closes. */
internal val Project.tapestryScope: CoroutineScope
    get() = service<TapestryCoroutineService>().scope
