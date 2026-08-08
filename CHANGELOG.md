<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Tapestry Changelog

## [Unreleased]

### Fixed

- No longer freezes the IDE after a Maven/Gradle reimport: the Tapestry module scan that
  ran on the write thread on every roots change (re-reading each module's `pom.xml` and every dependency
  jar's `MANIFEST.MF`) now runs in the background.
- Selecting a class or template in the Tapestry view pane no longer resolves the element model on the UI
  thread, and waits for indexing to finish instead of silently resolving to nothing during it.
- Live documentation and the project view no longer refresh on every keystroke; content changes are debounced.
- Creating a new page, component or mixin is now a single undoable command, and an error is reported after the
  write action rather than opening a modal dialog while the write lock is held.
- Cancellation (`ProcessCanceledException`) is no longer swallowed during reference resolution, completion,
  drag-and-drop and action updates.
- Fixed a crash when generating documentation for an element whose description had no space in its first 100
  characters.
- `pom.xml` generated for a new Tapestry facet is no longer written twice, with the second (less complete)
  write clobbering the first — it now keeps the file-template output and its real source-root path.

- `@SupportsInformalParameters` is no longer cached without invalidation: adding or removing the annotation is
  picked up instead of keeping the verdict from when the component was first inspected.
- Values bound to a component parameter declared with an unnamed annotation value (`@Component("id")`) are read
  again; the attribute was previously filed under a key nothing looked up.

### Changed

- The per-module persisted state component was renamed from `Loomy` to `TapestrySupport`. The directories last
  used for new pages/components/mixins reset once as a result.
- Removed the last of the IDE-portability layer the plugin inherited from the standalone Tapestry model: the
  `IJava*` interfaces and their `Intellij*` implementations are gone, and the model works on IntelliJ PSI
  (`PsiClass`, `PsiType`, `PsiField`, `PsiMethod`, `PsiAnnotation`) directly. Long-lived model objects now hold
  their class through a `SmartPsiElementPointer` instead of re-resolving it from a stored file URL.

## [1.0.0] - 2026-08-05

First release of the maintained port of the Tapestry plugin JetBrains retired into
[intellij-obsolete-plugins](https://github.com/JetBrains/intellij-obsolete-plugins/tree/master/tapestry).

### Added

- Gutter markers for injected beans (`@Inject`, including `jakarta`/`javax` JSR-330) and Tapestry event handlers,
  resolved through UAST so Kotlin sources are covered too.
- *Used By* reverse-dependency view in the tool window's Dependencies tab, plus navigation to templates and message
  catalogs from the dependency tree.
- Live documentation for project modules, their Tapestry IoC services and library-provided modules, including the
  Maven coordinates of the contributing library.
- Tapestry module detection without a facet, from `Tapestry-Module-Classes` declared in a module's `pom.xml` or
  `META-INF/MANIFEST.MF`.

### Changed

- Ported all production and test code to Kotlin; only the generated JFlex lexer remains Java.
- Moved to the IntelliJ Platform Gradle Plugin 2.x and IntelliJ IDEA 2026.2, including the 2026.2 module system
  (explicit `intellij.platform.ui.jcef` and `intellij.xml.structureView.impl` module dependencies).
- Replaced all internal and override-only platform API usage, so the plugin verifies clean.
- Migrated tests to kotest + MockK; TestNG, EasyMock and commons-chain are gone.
- Rewrote the create-element dialogs and the facet editor in the Kotlin UI DSL, replacing the GUI Designer forms.
- Switched to platform icons instead of bundled bitmaps.
- Renamed the plugin's package and ID out of the `com.intellij` namespace to `com.github.rar91279.plugin.tapestry`.

### Fixed

- Tool window no longer fails to open on 2026.2, where JCEF moved out of the platform core.
- TML structure view, which needed an explicit dependency on the XML structure-view module.
- New Page / Component / Mixin actions, which were unavailable in most project view contexts.
- Documentation tab navigation links, which broke on names containing characters that need escaping.
- Tapestry project view pane crash on selection.
