# Outstanding review follow-ups

The `IJava*` removal this file used to plan is **done**: `core/java/**` and `intellij/core/java/**` are gone,
the model works on PSI directly, and both suites are green. What is left below is the work that was parked
behind it, plus the review items that were never part of it. Nothing here is a blocker.

## What the removal left behind

- `core/util/PsiExtensions.kt` holds the handful of accessors that were more than delegation:
  `attributeValues`, `tapestryFields`, `publicMethods`/`tapestryMethods`/`findPublicMethods`,
  `supportsInformalParameters` (now on `CachedValuesManager`), `classType`, `javadocDescription`,
  `erasedIfTypeVariable`.
- `TapestryProject` absorbed `IJavaTypeFinder` (`findType`, `findClassType`, `classTypeOf`,
  `findTypesInPackage[Recursively]`) — as members rather than a collaborator, so the fast `kotest` specs can
  still mock them.
- `IJavaTypeCreator`/`IntellijJavaTypeCreator` became the concrete `intellij/util/JavaTypeCreator`.
- `AssignableToAll` became `PsiTypes.nullType()`, special-cased in `TypeCoercionValidator` exactly where the
  sentinel used to be.
- `core/mocks/PsiMocks.kt` (was `XmlMocks.kt`) builds the `mockk` PSI stand-ins the core specs need:
  `psiClassMock`, `psiFieldMock`, `psiMethodMock`, `psiAnnotationMock`, `psiClassTypeMock`, plus
  `stubFields`/`stubMethods`. `psiClassMock` wires a stub `SmartPointerManager` into its `project`, because
  `PresentationLibraryElement` holds its class through a smart pointer.

Two traps worth remembering, both found the hard way:

- The fast `kotest` task needs `testSourceSet.compileClasspath` on its classpath: the bundled-plugin jars
  (java PSI in particular) are not on `intellijPlatformClasspath`, so mocking `PsiParameter` failed to
  *discover* tests, not to run them.
- Dropping the wrapper's "treat a generic `T` as `Object`" rule made every value bound to a generic component
  parameter light up as an impossible coercion. That is what `erasedIfTypeVariable()` restores, and only the
  fixture suite caught it.

## Deferred work that depended on the removal

- **Generic `TapestryNode<T>`** — 11 unchecked `getValue() as X` casts, 4 `!!`. Only 7 are inside node
  classes; the other 4 are in `SafeDeleteProvider`/`ViewTransferHandler`, where the node type is genuinely
  unknown and generics do not help.
- `DocumentationTab`'s `Array<Any?>`-as-tuple returns → data classes, and its 8-copy `show*` preamble.
- Remaining `StringBuilder` loops → `buildString`/`joinToString` (`DocNav.js`, and the `Scanner`-over-a-String
  in `TemplateCompletionContributor.qualifierOf`).
- Three remaining hand-rolled file-stamp caches (`PresentationLibraryElement`, `ParameterReceiverElement`,
  `ModuleBuilder`), all keyed on `virtualFile?.timeStamp`. **Careful:** `ParameterReceiverElement` writes
  its timestamp *before* the compute loop, so a mid-compute edit is missed. Preserve or change that
  knowingly, not by accident.
- `InjectedElement`'s two mutually-exclusive nullable fields (`field` XOR `tag`) → sealed class.
- `PropResolver` resolves a path segment against `(type as? PsiClassType)?.resolve()`, which for a generic
  property yields the `PsiTypeParameter` rather than its bound. The old wrapper substituted `Object` here
  too. Harmless today (member lookup on a type parameter just finds nothing), but it is the one place
  `erasedIfTypeVariable()` is *not* applied.

## Other outstanding review items

**Threading / async** — the async architecture is sound (no `runBlocking`/`GlobalScope`/`invokeAndWait`;
caches on `CachedValuesManager`; the High-severity EDT freezes are already fixed on this branch). Remaining:

- `ProgressManager.checkCanceled()` is still missing from five long loops: `TapestryProject` (every
  component + page), `DocumentationTab` (module × library; `<root>.services` recursion; every `pom.xml` in
  the project), and `ClassLocator` (`walkTopDown` / full jar-entry enumeration).
- `DocumentationTab` holds one read action across jar mounting and XML parsing
  (`CoreLibraryDocumentation.indexSection` → `ClassLocator.locate` → `DocumentBuilderFactory.parse`). None of
  it touches PSI; split it — I/O outside, PSI inside — so it stops blocking write actions (i.e. typing).
- 11 `ReadAction.nonBlocking(...).submit(...)` sites (`DocumentationTab` ×5, `DependenciesTab` ×2,
  `TapestryToolWindow`, `ViewMouseListener`) are correct as written; migrating them to
  `uiScope.launch { smartReadAction { … } }` is optional modernisation. Two deserve a real behaviour change,
  not a syntax swap: `DependenciesTab`'s hand-rolled "Loading dependencies…" busy state wants
  `withBackgroundProgress`, and `DocumentationTab`'s `if (DumbService.isDumb) return emptyList()` should be
  `smartReadAction` so docs stop silently rendering with zero services during indexing.
- `DocumentationTab.kt` `DumbService.runWhenSmart { }` is un-cancellable and un-scoped; subsumed by the
  above, or `uiScope.launch { project.waitForSmartMode(); … }`.
- `TapestryToolWindow`'s `SwingUtilities.invokeLater` is **fine as-is** — a pure-Swing JCEF paint
  workaround with no platform state. Don't "fix" it. Likewise `ViewMouseListener` is correct: its
  `HashMap`/`HashSet` are EDT-only by construction.

**Platform / descriptor**

- Action texts are hardcoded English in `plugin.xml` while `messages.TapestryBundle` exists and is used for
  inspections. Add `<resource-bundle>` and switch actions to `key=`/`descriptionKey=`.
- `TapestryModuleSupportLoader`'s static `getInstance(...)` accessors are boilerplate over
  `module.service<…>()` (the XML registration itself is correct — `@Service` has no `Level.MODULE`).
- JUnit 3 residue kept alive by `junit-vintage-engine`: `UsefulTestCase` subclasses and deprecated
  `junit.framework.Assert` across ~7 test classes. Finishing the move onto `JavaModuleFixtureSpec` + kotest
  matchers would let `junit`, `hamcrest` and `junit-vintage-engine` leave `libs.versions.toml`. Its own
  project, and note `TapestryBaseTestCase` is where the Tapestry facet fixture lives, so it must be ported
  rather than dropped.

**Explicitly rejected on evidence, do not re-raise**

- "Remove the unused `relaxng` and `kotlin` Gradle dependencies" — both are load-bearing.
  `TapestryResolveTest` imports `org.intellij.plugins.relaxNG.compact.RncElementTypes`, and the plugin
  registers a `language="UAST"` line-marker provider with a Kotlin page (`StartPage3.kt`) under test.
  A `src/main`-only import grep misses both.
- "`TapestryInspectionBase` uses the slowest possible visitor dispatch" — overstated. The inspection is
  registered `language="TEL"`, so the platform already scopes it to TEL PSI (small injected attribute
  expressions), and no generated TEL visitor exists to return.
- The generated `src/main/gen/**` (JFlex lexer, icon class) — no `.flex`/`.bnf` sources are checked in, so
  it cannot be regenerated. Leave it alone.

## Build hygiene

`:instrumentCode` / `:instrumentTestCode` fail spuriously right after file deletions
(`1 >= 1`, or `typedef doesn't support the nested "typedef" element`) and pass on an unchanged re-run.
Stale incremental state in the IntelliJ Platform Gradle Plugin — re-run, don't hunt a code cause.

Kotlin's incremental compile also under-reports here: after a type-wide change, `compileTestKotlin` can go
`UP-TO-DATE` while test sources no longer compile. Use `--rerun-tasks` when verifying a step.

## Not covered by tests

`TapestryInjectedBeanLineMarkerProvider`'s `@ImportModule` and framework-module discovery have no automated test. A fixture test
would need tapestry-ioc on `JavaModuleFixtureSpec`'s classpath (it only has `dep1.jar`), a Tapestry facet so
`isTapestryModule` passes, and a module class + `bind(...)` + injection point — more scaffolding than the
~20 lines it would cover. Verified by hand in `runIde` against a project that uses `@ImportModule`.

## Known limit: services bound in a jar with no sources attached

`bindSources` reads `bind(...)` calls out of the `bind(ServiceBinder)` method **body**. For a compiled
module class that body only exists in an attached sources jar (the class-file stub renders
`{ /* compiled code */ }`, and IntelliJ's FernFlower output is not reachable from PSI). So a library or
framework service provided by `bind(...)` shows no gutter marker unless sources for that artifact are
attached — e.g. `SelectModelFactory`, bound by `org.apache.tapestry5.modules.TapestryModule`, with no
`tapestry-core-<v>-sources.jar` in the local repository. Services provided by `build*` methods are
unaffected: those need only the signature, which the stub has.
