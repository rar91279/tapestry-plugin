package com.github.rar91279.plugin.tapestry.core.maven

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
