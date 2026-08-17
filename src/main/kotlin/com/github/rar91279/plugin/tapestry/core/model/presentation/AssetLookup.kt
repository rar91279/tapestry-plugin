package com.github.rar91279.plugin.tapestry.core.model.presentation

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.util.PathUtils
import com.intellij.psi.PsiFile

/**
 * The file behind a Tapestry asset path, or nothing when the path names no local file.
 *
 * `context:` resolves against the web roots and `classpath:` against the classpath root, as at runtime. An
 * unprefixed path is ambiguous — where it lands depends on the Tapestry version and on how the application
 * lays its assets out — so every layout in use is tried, nearest first: relative to [fromPackage],
 * `META-INF/assets/<fromPackage>/`, `META-INF/assets/`, and finally the web context.
 *
 * `webjars:`, URLs and paths holding a `${symbol}` yield nothing: there is no file to navigate to.
 *
 * @param fromPackage the package an unprefixed relative path is resolved against — the package of the class
 *                    that declares the import.
 */
fun TapestryProject.findAssets(rawPath: String, fromPackage: String?): Collection<PsiFile> {
    if (rawPath.isEmpty() || rawPath.contains("\${")) return emptyList()

    val prefix = rawPath.substringBefore(':', "")
    val path = rawPath.substringAfter(':')

    if (prefix == "context") return listOfNotNull(resourceFinder.findContextResource(normalizeAssetPath(path)))
    if (prefix.isNotEmpty() && prefix != "classpath") return emptyList()

    val absolute = prefix == "classpath" || path.startsWith("/")
    val fromRoot = normalizeAssetPath(path)
    val packageRelative = normalizeAssetPath(PathUtils.packageIntoPath(fromPackage, true) + path)

    // Nearest first: next to the class (the pre-5.4 layout, a real package, so the package index finds it),
    // then under the 5.4 asset root, per package and plain. `META-INF` is not a valid package name, so those
    // two have to be looked up by root-relative path instead.
    val lookups: List<() -> Collection<PsiFile>> =
        if (absolute) listOf({ resourceFinder.findClasspathResource(fromRoot, true) })
        else listOf(
            { resourceFinder.findClasspathResource(packageRelative, true) },
            { resourceFinder.findRootRelativeResource("$ASSET_ROOT_PATH/$packageRelative") },
            { resourceFinder.findRootRelativeResource("$ASSET_ROOT_PATH/$fromRoot") },
            // Nowhere on the classpath: an application that keeps its assets in the web context and imports
            // them without the `context:` prefix.
            { listOfNotNull(resourceFinder.findContextResource(fromRoot)) }
        )

    return lookups.firstNotNullOfOrNull { it().ifEmpty { null } }.orEmpty()
}

/**
 * The file behind a JavaScript module name — `BB/ContextMenu/contextmenu` is
 * `META-INF/modules/BB/ContextMenu/contextmenu.js`.
 *
 * A module is named, not pathed: no prefix, no extension. Tapestry resolves it against the module root on the
 * classpath, so unlike an asset there is only one place to look.
 *
 * Nothing found means the name was no module — which is also how a caller tells module names apart from other
 * strings, there being nothing in the name itself to go by.
 */
fun TapestryProject.findJavaScriptModule(name: String): Collection<PsiFile> {
    if (name.isEmpty() || name.contains(':') || name.contains("\${")) return emptyList()

    return MODULE_EXTENSIONS.firstNotNullOfOrNull {
        resourceFinder.findRootRelativeResource("$MODULE_ROOT_PATH/$name$it").ifEmpty { null }
    }.orEmpty()
}

/** Whether a string looks like an asset path at all — used to sift asset paths out of arbitrary literals. */
fun looksLikeAssetPath(value: String): Boolean =
    ASSET_EXTENSIONS.any { value.endsWith(it, ignoreCase = true) }

/**
 * What kind of asset a file is, by extension. Stylesheets and scripts are worth keeping apart wherever assets
 * are listed: they are looked for separately.
 */
enum class AssetKind { STYLESHEET, SCRIPT, OTHER }

/** The kind of asset a file name denotes. */
fun assetKindOf(fileName: String): AssetKind = when (fileName.substringAfterLast('.', "").lowercase()) {
    "css", "less", "scss" -> AssetKind.STYLESHEET
    "js", "mjs", "coffee" -> AssetKind.SCRIPT
    else -> AssetKind.OTHER
}

/** The kind of asset a file is. */
fun PsiFile.assetKind(): AssetKind = assetKindOf(name)

/** The module root every JavaScript module is resolved under, relative to a source or resource root. */
const val MODULE_ROOT_PATH: String = "META-INF/modules"

/** The asset root Tapestry 5.4 and later expect classpath assets under. */
const val ASSET_ROOT_PATH: String = "META-INF/assets"

private val ASSET_EXTENSIONS = listOf(".css", ".js", ".mjs", ".less", ".coffee")

private val MODULE_EXTENSIONS = listOf(".js", ".coffee")

/** Collapses `.` and `..` segments and drops the leading separator, which classpath lookups don't take. */
private fun normalizeAssetPath(path: String): String {
    val segments = ArrayList<String>()

    for (segment in path.split(PathUtils.TAPESTRY_PATH_SEPARATOR)) when (segment) {
        "", "." -> {}
        ".." -> segments.removeLastOrNull()
        else -> segments.add(segment)
    }

    return segments.joinToString(PathUtils.TAPESTRY_PATH_SEPARATOR)
}
