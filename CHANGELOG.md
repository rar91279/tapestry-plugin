<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Tapestry Changelog

## Unreleased

## 1.0.3 - 2026-08-18

### Added

- *Navigate | Related Symbol* (`Ctrl+Alt+Home`) hops between the files that make up one Tapestry element,
  grouped as class, template, message catalogs, stylesheets, javascript, other assets, and one group per
  imported JavaScript stack. Works from the class, the template and a message catalog.
- Assets imported with `@Import(stylesheet = …, library = …)` and injected with `@Path` are resolved across the
  layouts in use: next to the class, `META-INF/assets/<package>/`, the `META-INF/assets/` root, and the web
  context. `@Import(module = …)` resolves under `META-INF/modules/`.
- Each stack in `@Import(stack = …)` is resolved to the class the IoC contribution names, the stylesheets,
  libraries and modules it bundles, and the stacks it includes in turn — however the class passes them along,
  including constants handed to a base class constructor. Assets inside a webjar are left out: there is no
  file to open. Contents are read from the string literals a stack declares, so paths computed at runtime are
  not seen.
- The Tapestry view pane has a **Services** branch — the IoC services the module declares, each opening the
  `build*` method that declares it rather than the service interface. Services bound with `bind(...)` are not
  listed yet.
- The Tapestry view pane has an **Assets** branch: StyleSheets, Javascripts, Modules and JavaScriptStacks,
  filled from where Tapestry loads them — `META-INF/assets` and `META-INF/modules` on every source and resource
  root, plus the web context roots — so an asset that no element imports yet is visible too. Each stack lists
  the Css, JS and Modules it bundles.
- An element in the Tapestry view pane lists its imported assets alongside its class, templates and message
  catalogs.
- `IResourceFinder.findRootRelativeResource`, which resolves a path against the module's source and resource
  roots. Unlike the existing classpath lookup it doesn't go through the package index, so it reaches paths no
  package can name — `META-INF/assets`, `META-INF/modules`.

### Fixed

- Selecting a node in the Tapestry view pane, dragging one, or deleting one no longer throws a
  `ClassCastException`: a tree restoring itself from a cached presentation hands out placeholder nodes, which
  the pane, the mouse listener, the transfer handler and the safe-delete provider all cast without checking.
- Every node in the Tapestry view pane that stands for a file now opens it. Only elements used to, so an
  element's class, template, message catalogs and assets were all dead on click.
- Template usages of a private page/component/mixin member are found again: *Find Usages*, *Show Usages*, the
  usage-count hint and the in-file usage highlight all list the `.tml` occurrences. The references resolved to
  the member all along, but Java restricts the search of a private member to its own file, so nothing ever
  looked at the templates — they are added back to the member's use scope now.
- A `@Property` field is no longer reported as *assigned but never accessed*. Its write was already known to be
  implicit; its read was not, and Tapestry reads the field to render it. The same goes for `@Component`,
  `@Mixin`, `@Parameter`, `@Persist`, `@PageActivationContext`, `@SessionState`, `@ApplicationState`,
  `@SessionAttribute` and `@ActivationRequestParameter`. Injected fields (`@Inject`, `@InjectService`,
  `@InjectPage`, `@InjectComponent`, `@InjectContainer`, `@Environmental`) keep the report: an injection nothing
  reads is worth knowing about.
- The injected-bean gutter marker no longer risks a `PsiInvalidElementAccessException` when *Related Symbol*
  collection hands it an element from a superseded view provider; it and the related-symbol provider both
  check validity first.

### Changed

- The Tapestry view pane shows Tapestry entities only. It used to mirror the package hierarchy of each module,
  which put pages and components among plain packages and plain classes; a module is now presented as the fixed
  set of things it holds — Services, Pages, Components, Mixins, Assets, Libraries — with pages, components and
  mixins nested by subpackage. A branch that holds nothing is left out entirely, so a module providing only IoC
  services shows only Services.
- *New > Tapestry Page/Component/Mixin* is enabled on a module node in the Tapestry view, creating at the
  element root package. That is how the first page of a module is created now that an empty Pages branch is not
  shown.
- Clicking in the Tapestry view opens something only where there is something to open: the leaves — files, a
  service's build method, a stack's class. Categories, subpackage folders and an element listing its files
  expand instead, and a folder no longer jumps out to the Project view.
- *Group Element Files* now toggles whether an element expands to the files it is made of; with plain files gone
  from the tree it had nothing else left to switch. *Show From Base Package* is gone: the categories are derived
  from the application base package, so the tree always starts there.
- `@Path` is recognized in both packages it has lived in (`org.apache.tapestry5.ioc.annotations` and
  `org.apache.tapestry5.annotations`).
- Discovery of the IoC module classes visible from a module — manifest-declared, the application's `services`
  package, the framework's own, and everything they pull in with `@ImportModule` — moved out of the
  injected-bean gutter marker into a shared `TapestryModuleClasses`, which JavaScript stack resolution reads
  contributions from as well.
- Asset path resolution moved into a shared `TapestryProject.findAssets`, used both by an element's own
  imports and by the stacks it imports, so a stack's relative paths resolve against the stack's package.

## 1.0.2 - 2026-08-13

### Fixed

- *New > Page* no longer fails with a `ClassNotFoundException`: `AddNewPageAction` kept the package it had before
  actions and dialogs were split into their own packages, so the class was compiled somewhere other than where
  `plugin.xml` looked for it. `AddNewMixinDialog` had the same leftover package.
- Fields annotated with `@InjectService` or the JSR-330 `@Inject` (`jakarta.inject` / `javax.inject`) are no
  longer reported as *never assigned*: the implicit usage provider kept its own annotation list, which had
  drifted from the one the injection gutter icons use. Both now share a single list.

## 1.0.1 - 2026-08-11

### Added

- New inspection *Public field with Tapestry annotation*, which flags public fields annotated with `@Property`,
  `@Parameter`, `@Persist`, `@Component` and friends — Tapestry only instruments non-public fields, so those
  annotations are silently ignored.

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
- The embedded `tapestry_5_4.xsd` declared `name` instead of `id` on `extension-point` and `replace`, so valid
  IoC XML was reported as an error. The embedded 5.1/5.3/5.4 schemas now match the ones published online.
- TML templates no longer report irrelevant XML inspection warnings (empty tag bodies and the like).
- `@Parameter` on a protected field is recognized again; only public fields are excluded.

### Changed

- The release workflow now treats the version tag as the source of truth: pushing `vX.Y.Z` sets
  `version` in `gradle.properties`, closes the changelog's `[Unreleased]` section, and commits both back
  to the default branch. The tag must sit at the branch head or the release fails.
- The per-module persisted state component was renamed from `Loomy` to `TapestrySupport`. The directories last
  used for new pages/components/mixins reset once as a result.
- Removed the last of the IDE-portability layer the plugin inherited from the standalone Tapestry model: the
  `IJava*` interfaces and their `Intellij*` implementations are gone, and the model works on IntelliJ PSI
  (`PsiClass`, `PsiType`, `PsiField`, `PsiMethod`, `PsiAnnotation`) directly. Long-lived model objects now hold
  their class through a `SmartPsiElementPointer` instead of re-resolving it from a stored file URL.

## 1.0.0 - 2026-08-05

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
