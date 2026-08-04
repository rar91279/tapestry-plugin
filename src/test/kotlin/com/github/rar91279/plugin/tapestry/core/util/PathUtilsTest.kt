package com.github.rar91279.plugin.tapestry.core.util

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class PathUtilsTest : FreeSpec({

    "packageIntoPath" - {
        "empty" {
            PathUtils.packageIntoPath(null, true).shouldBe("")
            PathUtils.packageIntoPath(null, false).shouldBe("")
            PathUtils.packageIntoPath("", true).shouldBe("")
            PathUtils.packageIntoPath("", false).shouldBe("")
        }
        "various" {
            PathUtils.packageIntoPath("a.b", true).shouldBe("a/b/")
            PathUtils.packageIntoPath("a.b", false).shouldBe("a/b")
            PathUtils.packageIntoPath("a", true).shouldBe("a/")
            PathUtils.packageIntoPath("a", false).shouldBe("a")
        }
    }

    "pathIntoPackage" - {
        "empty" {
            PathUtils.pathIntoPackage(null, true).shouldBe("")
            PathUtils.pathIntoPackage(null, false).shouldBe("")
            PathUtils.pathIntoPackage("", true).shouldBe("")
            PathUtils.pathIntoPackage("", false).shouldBe("")
        }
        "various" {
            PathUtils.pathIntoPackage("a/b", true).shouldBe("a")
            PathUtils.pathIntoPackage("a/b", false).shouldBe("a.b")
            PathUtils.pathIntoPackage("a/b/", true).shouldBe("a")
            PathUtils.pathIntoPackage("a/b/", false).shouldBe("a.b")
            PathUtils.pathIntoPackage("a", true).shouldBe("a")
            PathUtils.pathIntoPackage("a", false).shouldBe("a")
            PathUtils.pathIntoPackage("a/", true).shouldBe("a")
            PathUtils.pathIntoPackage("a/", false).shouldBe("a")
            PathUtils.pathIntoPackage("a/a.txt", true).shouldBe("a")
            PathUtils.pathIntoPackage("a/b/a.txt", true).shouldBe("a.b")
            PathUtils.pathIntoPackage("/a/b/a.txt", true).shouldBe("a.b")
        }
    }

    "getFullComponentPackage" - {
        "empty" {
            PathUtils.getFullComponentPackage(null, null).shouldBe("")
            PathUtils.getFullComponentPackage("", "").shouldBe("")
            PathUtils.getFullComponentPackage(null, "").shouldBe("")
            PathUtils.getFullComponentPackage("", null).shouldBe("")
        }
        "various" {
            PathUtils.getFullComponentPackage("com.myapp.pages", "admin/Login").shouldBe("com.myapp.pages.admin")
            PathUtils.getFullComponentPackage("com.myapp.pages", "Login").shouldBe("com.myapp.pages")
        }
    }

    "getLastPathElement" - {
        "empty" {
            PathUtils.getLastPathElement(null).shouldBe("")
            PathUtils.getLastPathElement("").shouldBe("")
        }
        "various" {
            PathUtils.getLastPathElement("admin/Login").shouldBe("Login")
            PathUtils.getLastPathElement("Login").shouldBe("Login")
        }
    }

    "getFirstPathElement" - {
        "empty" {
            PathUtils.getFirstPathElement(null).shouldBe("")
            PathUtils.getFirstPathElement("").shouldBe("")
        }
        "various" {
            PathUtils.getFirstPathElement("admin/Login").shouldBe("admin")
            PathUtils.getFirstPathElement("/admin/Login").shouldBe("admin")
            PathUtils.getFirstPathElement("Login").shouldBe("Login")
        }
    }

    "removeLastFilePathElement" - {
        "empty" {
            PathUtils.removeLastFilePathElement(null, true).shouldBe("")
            PathUtils.removeLastFilePathElement("", true).shouldBe("")
        }
        "various" {
            PathUtils.removeLastFilePathElement("admin/Login", true).shouldBe("admin")
            PathUtils.removeLastFilePathElement("admin\\Login", true).shouldBe("admin")
            PathUtils.removeLastFilePathElement("admin/Login", false).shouldBe("admin")
            PathUtils.removeLastFilePathElement("admin\\Login", false).shouldBe("admin")
            PathUtils.removeLastFilePathElement("Login", false).shouldBe("Login")
            PathUtils.removeLastFilePathElement("Login", true).shouldBe("")
        }
    }

    "getComponentFileName" - {
        "empty" {
            PathUtils.getComponentFileName(null).shouldBe("")
            PathUtils.getComponentFileName("").shouldBe("")
        }
        "various" {
            PathUtils.getComponentFileName("admin/Login").shouldBe("Login")
            PathUtils.getComponentFileName("Login").shouldBe("Login")
        }
    }

    "toUnixPath" {
        PathUtils.toUnixPath(null).shouldBe(null)
        PathUtils.toUnixPath("/path1/path2").shouldBe("/path1/path2")
    }
})
