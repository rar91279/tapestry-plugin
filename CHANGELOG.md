<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Tapestry Changelog

## [Unreleased]

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
