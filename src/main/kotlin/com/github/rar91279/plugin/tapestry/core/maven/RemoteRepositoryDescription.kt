package com.github.rar91279.plugin.tapestry.core.maven

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
