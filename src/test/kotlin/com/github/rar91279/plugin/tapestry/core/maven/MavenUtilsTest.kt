package com.github.rar91279.plugin.tapestry.core.maven

import com.intellij.openapi.util.io.FileUtil
import io.kotest.core.spec.style.FreeSpec
import org.hamcrest.MatcherAssert
import org.xmlunit.matchers.HasXPathMatcher
import java.io.File

private val NAMESPACE_CONTEXT = mapOf("ns" to "http://maven.apache.org/POM/4.0.0")

private fun hasXPathWithNs(xPath: String): HasXPathMatcher =
    HasXPathMatcher.hasXPath(xPath).withNamespaceContext(NAMESPACE_CONTEXT)

private fun generatePomXmlText(mavenConfiguration: MavenConfiguration): String {
    val targetDirectory = FileUtil.createTempDirectory("MavenUtilsTest", null)
    MavenUtils.createMavenSupport(targetDirectory.absolutePath, mavenConfiguration, "5")
    return FileUtil.loadFile(File(targetDirectory, "pom.xml"))
}

class MavenUtilsTest : FreeSpec({


    "createMavenSupport_check_dependencies" {
        val mavenConfiguration = MavenConfiguration(true, false, null, null, null, "group", "artifact", "1.1", null)

        val pom = generatePomXmlText(mavenConfiguration)
        MatcherAssert.assertThat(pom, hasXPathWithNs("/ns:project/ns:dependencies/ns:dependency/ns:groupId[text()='org.apache.tapestry']"))
        MatcherAssert.assertThat(pom, hasXPathWithNs("/ns:project/ns:dependencies/ns:dependency/ns:artifactId[text()='tapestry-core']"))
        MatcherAssert.assertThat(pom, hasXPathWithNs("/ns:project/ns:dependencies/ns:dependency/ns:version[text()='5']"))
    }

    "createMavenSupport_with_remote_repositories" {
        val repositories = ArrayList<RemoteRepositoryDescription>()
        repositories.add(RemoteRepositoryDescription("url1", "id1", "name1", true, true))

        val mavenConfiguration = MavenConfiguration(true, true, null, null, null, "group", "artifact", "1.1", repositories)

        var pom = generatePomXmlText(mavenConfiguration)

        MatcherAssert.assertThat(pom, hasXPathWithNs("/ns:project/ns:repositories/ns:repository/ns:id[text()='id1']"))
        MatcherAssert.assertThat(pom, hasXPathWithNs("/ns:project/ns:repositories/ns:repository/ns:name[text()='name1']"))
        MatcherAssert.assertThat(pom, hasXPathWithNs("/ns:project/ns:repositories/ns:repository/ns:url[text()='url1']"))
        MatcherAssert.assertThat(pom, hasXPathWithNs("/ns:project/ns:repositories/ns:repository/ns:releases"))
        MatcherAssert.assertThat(pom, hasXPathWithNs("/ns:project/ns:repositories/ns:repository/ns:snapshots"))

        repositories.clear()
        repositories.add(RemoteRepositoryDescription("url2", "id2", "name2", false, false))
        pom = generatePomXmlText(mavenConfiguration)

        MatcherAssert.assertThat(pom, hasXPathWithNs("/ns:project/ns:repositories/ns:repository/ns:id[text()='id2']"))
        MatcherAssert.assertThat(pom, hasXPathWithNs("/ns:project/ns:repositories/ns:repository/ns:name[text()='name2']"))
        MatcherAssert.assertThat(pom, hasXPathWithNs("/ns:project/ns:repositories/ns:repository/ns:url[text()='url2']"))
    }

    "createMavenSupport_with_parent_pom" {
        val mavenConfiguration = MavenConfiguration(true, false, "parentGroup", "parentArtifact", "1.0", "group", "artifact", "1.1", null)

        val pom = generatePomXmlText(mavenConfiguration)

        MatcherAssert.assertThat(pom, hasXPathWithNs("/ns:project/ns:parent/ns:groupId[text()='parentGroup']"))
        MatcherAssert.assertThat(pom, hasXPathWithNs("/ns:project/ns:parent/ns:artifactId[text()='parentArtifact']"))
        MatcherAssert.assertThat(pom, hasXPathWithNs("/ns:project/ns:parent/ns:version[text()='1.0']"))
    }

    "createMavenSupport_default_version" {
        var mavenConfiguration = MavenConfiguration(false, false, null, null, null, "group", "artifact", null, null)
        val pom = generatePomXmlText(mavenConfiguration)
        MatcherAssert.assertThat(pom, hasXPathWithNs("/ns:project/ns:version[text()='1.0-SNAPSHOT']"))

        mavenConfiguration = MavenConfiguration(false, false, null, null, null, "group", "artifact", "", null)
        MatcherAssert.assertThat(generatePomXmlText(mavenConfiguration), hasXPathWithNs("/ns:project/ns:version[text()='1.0-SNAPSHOT']"))

        mavenConfiguration = MavenConfiguration(false, false, null, null, null, "group", "artifact", "1.0", null)
        MatcherAssert.assertThat(generatePomXmlText(mavenConfiguration), hasXPathWithNs("/ns:project/ns:version[text()='1.0']"))
    }

    "createMavenSupport_valid_header" {
        val mavenConfiguration = MavenConfiguration(false, false, null, null, null, "group", "artifact", null, null)

        val pom = generatePomXmlText(mavenConfiguration)
        MatcherAssert.assertThat(pom, hasXPathWithNs("/ns:project/ns:modelVersion[text()='4.0.0']"))
    }
})
