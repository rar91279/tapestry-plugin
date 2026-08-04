package com.github.rar91279.plugin.tapestry.core.maven

import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import org.apache.maven.model.Build
import org.apache.maven.model.Dependency
import org.apache.maven.model.Model
import org.apache.maven.model.Parent
import org.apache.maven.model.Repository
import org.apache.maven.model.RepositoryPolicy
import org.apache.maven.model.Resource
import org.apache.maven.model.io.xpp3.MavenXpp3Writer
import java.io.File
import java.io.FileWriter
import java.io.IOException

/**
 * Description of a remote repository.
 */
class RemoteRepositoryDescription(
    val url: String?,
    val id: String?,
    val name: String?,
    val isCreatingSnapshots: Boolean,
    val isCreatingReleases: Boolean
)

/**
 * Holds the configuration settings given in the wizard.
 */
class MavenConfiguration(
    val isCreateParentPom: Boolean,
    val isAddRemoteRepository: Boolean,
    val groupIdParentPom: String?,
    val artifactIdParentPom: String?,
    val versionParentPom: String?,
    val groupId: String?,
    val artifactId: String?,
    val version: String?,
    val remoteRepositoryList: List<RemoteRepositoryDescription>?
)

/**
 * Maven related utilities.
 */
object MavenUtils {

    /**
     * Creates a Maven pom.xml.
     *
     * @param path               the path to the directory where the pom.xml will be created.
     * @param mavenConfiguration all maven configurations.
     * @param tapestryVersion    the selected Tapestry version.
     * @throws IOException if an error occurs creating the pom.xml file.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun createMavenSupport(path: String, mavenConfiguration: MavenConfiguration, tapestryVersion: String) {
        val model = Model()

        model.modelVersion = "4.0.0"
        model.packaging = "war"
        model.groupId = mavenConfiguration.groupId
        model.artifactId = mavenConfiguration.artifactId
        model.version = mavenConfiguration.version?.ifEmpty { null } ?: "1.0-SNAPSHOT"

        // Add dependencies of tapestry
        model.addDependency(Dependency().apply {
            groupId = "org.apache.tapestry"
            artifactId = "tapestry-core"
            version = tapestryVersion
        })

        // Add resources build configuration
        model.build = Build().apply {
            addResource(Resource().apply {
                directory = "src/main/java"
                addInclude("**/*.${TapestryConstants.TEMPLATE_FILE_EXTENSION}")
                addInclude("**/*.properties")
            })
        }

        if (mavenConfiguration.isCreateParentPom) {
            model.parent = Parent().apply {
                artifactId = mavenConfiguration.artifactIdParentPom
                groupId = mavenConfiguration.groupIdParentPom
                version = mavenConfiguration.versionParentPom
            }
        }

        if (mavenConfiguration.isAddRemoteRepository) {
            for (description in mavenConfiguration.remoteRepositoryList.orEmpty()) {
                model.addRepository(Repository().apply {
                    name = description.name
                    id = description.id
                    url = description.url
                    releases = RepositoryPolicy().apply { isEnabled = description.isCreatingReleases }
                    snapshots = RepositoryPolicy().apply { isEnabled = description.isCreatingSnapshots }
                })
            }
        }

        FileWriter(File("$path/pom.xml")).use { MavenXpp3Writer().write(it, model) }
    }
}
