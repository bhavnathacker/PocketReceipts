package com.bhavnathacker.pocketreceipts.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Module-level privacy checks (see [PrivacyRules]). Applied by every module
 * convention plugin.
 */
class PrivacyConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.registerHttpClientVerification()
    }
}
