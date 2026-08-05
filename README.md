# Tapestry

IntelliJ IDEA support for the [Apache Tapestry™ 5](https://tapestry.apache.org) web framework: template and expression
language support, navigation between classes, templates and message catalogs, a project view, and a live documentation
tool window.

## Overview

This is a maintained port of the Tapestry plugin JetBrains retired into
[intellij-obsolete-plugins/tapestry](https://github.com/JetBrains/intellij-obsolete-plugins/tree/master/tapestry).

Compared to the original:

- **Ported to Kotlin** — production and test sources are Kotlin; only the generated lexer (`src/main/gen`) is still Java.
- **Modern plugin architecture** — IntelliJ Platform Gradle Plugin 2.x, current platform APIs (no internal or
  override-only API usage), the 2026.2 module system (explicit `intellij.platform.ui.jcef` /
  `intellij.xml.structureView.impl` module dependencies), and platform icons instead of bundled bitmaps.
- **Restored functionality** that was broken on recent IDE versions:
  - **Tool window** — no longer crashes on startup (JCEF moved out of the platform core in 2026.2).
  - **Dependencies tab** — rebuilt: embedded components, injected pages, a *Used By* reverse-dependency view,
    navigation to templates and message catalogs, empty-state and loading indicators.
  - **New Element actions** — Page / Component / Mixin creation works again, with dialogs rewritten in the Kotlin UI DSL
    and wider action enablement.
  - **Live Documentation** — the documentation generation chain was rewritten in Kotlin, with working navigation links,
    IoC service and library-module documentation, and Maven coordinates for library-provided modules.
- **Tests migrated** to [kotest](https://kotest.io) + [MockK](https://mockk.io) (TestNG and EasyMock are gone).

## Features

**Templates (`.tml`)**

- TML file type with XHTML-based highlighting, formatting, folding, commenting, structure view and parameter info.
- Tag and attribute completion driven by the actual component classes: components, pages, mixins, their parameters,
  required parameters and informal parameters.
- References from `t:type`, `t:id`, `t:mixins` and `page` attributes to the corresponding class, component instance
  field or page, with rename support.
- `message:` prefixes and `alt` attributes resolve to keys in the element's message catalog.
- Type-coercion validation: a parameter value that cannot be coerced to the parameter type is reported in the editor.

**Tapestry Expression Language (TEL)**

- TEL is injected into template attribute values (`${...}`, `prop:` bindings) with its own lexer, parser and
  highlighting.
- Completion and resolution of properties, `@Property` fields, generated accessors and public methods, including
  property chains and method calls.
- Find Usages, rename refactoring, and an inspection that flags unresolved TEL references.

**Java / Kotlin side**

- Gutter markers for Tapestry event handlers (`onXxx`, `@OnEvent`) and injected beans (`@Inject`, including
  `jakarta`/`javax` JSR-330, via UAST — so Kotlin sources work too).
- Tapestry entry points (component fields, event handlers, page classes) are not reported as unused.

**Navigation and actions**

| Action | Shortcut |
|--------|----------|
| Class ↔ Template navigation | `Ctrl+Shift+G` |
| Tag → documentation | `Ctrl+Shift+D` |
| *Go to* → Tapestry Template / Tapestry Class | editor popup |
| *New* → Tapestry Page / Component / Mixin | project view popup |

- Safe Delete of a Tapestry element removes its class, templates and message catalogs together.

**Views**

- **Tapestry project view pane** — pages, components, mixins and libraries as a Tapestry-shaped tree.
- **Tapestry tool window** (facet-bound, bottom):
  - *Documentation* — live documentation rendered in an embedded browser: project modules and their IoC services, the
    Tapestry core library, and the selected element's own documentation.
  - *Dependencies* — what an element embeds, injects, and which elements use it.

**Project setup**

- Tapestry facet (application package, filter name) with framework detection from an existing facet or from
  `Tapestry-Module-Classes` declared in the module's `pom.xml` / `META-INF/MANIFEST.MF`.
- *Add framework support* generates a `pom.xml` for a new Tapestry module.

## Requirements

- **IntelliJ IDEA Ultimate 2026.2** — the plugin depends on the Jakarta EE (`com.intellij.javaee`) and Properties
  plugins, so Community Edition is not supported.
- **JDK 25** — the Gradle toolchain the 2026.2 platform requires.

## Building and running

The Gradle wrapper is checked in; no local Gradle installation is needed.

| Task | Purpose |
|------|---------|
| `./gradlew buildPlugin` | Builds the distribution into `build/distributions/`. |
| `./gradlew runIde` | Starts a sandbox IDE with the plugin installed. |
| `./gradlew kotest` | Fast unit specs (`core/**`) — no IDE bootstrap, starts instantly. |
| `./gradlew test` | IDE-fixture integration specs (`tests/**`), full platform environment. |
| `./gradlew check` | `kotest` plus the standard verification lifecycle. |
| `./gradlew verifyPlugin` | Runs the IntelliJ Plugin Verifier against the recommended IDEs. |
| `./gradlew qodanaScan` | Qodana inspections (requires Docker). |

Predefined run configurations for the IDE, tests and verification live in `.run/`.

> [!NOTE]
> The two test tasks are split on purpose: `kotest` hand-builds its classpath and runs the pure unit specs in seconds,
> while `test` boots the platform test fixtures. Both run kotest specs on the JUnit Platform; `junit-vintage-engine`
> keeps the remaining `UsefulTestCase`-based tests discoverable.

### Source layout

```
.
├── .qodana/profiles/plugin.yaml     Qodana inspection profile
├── .run/                            Predefined Run/Debug configurations
├── gradle/
│   ├── libs.versions.toml           Version catalog (kotest, MockK, JUnit)
│   └── wrapper/                     Gradle wrapper
├── src
│   ├── main
│   │   ├── gen/                     Generated sources — not edited by hand
│   │   │   ├── com/github/rar91279/plugin/tapestry/psi/_TelLexer.java
│   │   │   └── icons/TapestryIcons.java
│   │   ├── kotlin/com/github/rar91279/plugin/tapestry/
│   │   │   ├── core/                IDE-independent model: elements, resources, IoC, coercion, externalizers
│   │   │   ├── intellij/            IDE integration: facet, actions, views, tool window, inspections
│   │   │   │   ├── core/            PSI-backed implementations of the core model interfaces
│   │   │   │   ├── lang/            Descriptors, annotator, completion, references, TEL injection
│   │   │   │   ├── toolwindow/      Documentation and Dependencies tabs
│   │   │   │   └── view/            Tapestry project view pane
│   │   │   ├── lang/                TML and TEL file types, languages and highlighters
│   │   │   └── psi/                 TML/TEL PSI: lexers, parsers, elements, references
│   │   └── resources/
│   │       ├── META-INF/            plugin.xml, plugin icon, Tapestry XSDs
│   │       ├── documentation/       Velocity templates, stylesheet and logos for the documentation tab
│   │       ├── fileTemplates/j2ee/  New page / component / mixin / pom templates
│   │       └── messages/            Message bundle
│   └── test
│       ├── java/                    Test *data* only (fixture sources and testData files)
│       └── kotlin/com/github/rar91279/plugin/tapestry/
│           ├── core/                Unit specs run by the `kotest` task
│           └── tests/               IDE-fixture specs run by the `test` task
├── build.gradle.kts                 Build configuration
├── gradle.properties                Gradle properties
└── qodana.yml                       Qodana configuration
```

The `core` package deliberately knows nothing about IntelliJ: it models Tapestry itself (pages, components, mixins,
parameters, resources, IoC services) behind small interfaces, and `intellij/core` provides the PSI-backed
implementations. Unit specs test `core` with mocks; everything that needs real PSI is tested through IDE fixtures.

## License

[Apache License 2.0](LICENSE) — the license of the original JetBrains plugin this project is derived from.
Portions are Copyright JetBrains s.r.o.

## Trademarks

Apache, Apache Tapestry, Tapestry and the Apache Tapestry logo are trademarks of
[The Apache Software Foundation](https://www.apache.org) (ASF). The official project site is
<https://tapestry.apache.org>.

This is an independent, community-maintained IntelliJ IDEA plugin **for working with** Apache Tapestry. It is not
affiliated with, sponsored by, or endorsed by the ASF or the Apache Tapestry project.

The plugin icon and the icons used for `.tml` files and the tool window depict the Apache Tapestry logo, solely to
identify the framework this plugin supports. The Apache License 2.0 covering this project's code does not grant rights
to ASF trademarks (see section 6 of the license).
