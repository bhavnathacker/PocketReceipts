package com.bhavnathacker.pocketreceipts.buildlogic

/**
 * Module dependency direction rules: features depend only on `:domain` and
 * `:core:*`; data modules never depend on features. `:app` is the composition
 * root and may depend on anything.
 */
internal object ModuleDependencyRules {

    /**
     * Returns one message per forbidden dependency of [projectPath], or an empty
     * list when every dependency in [dependencyPaths] is allowed.
     */
    fun violations(projectPath: String, dependencyPaths: Collection<String>): List<String> =
        dependencyPaths
            .filter { it != projectPath }
            .mapNotNull { dependency ->
                when {
                    projectPath.isFeature() && (dependency.isFeature() || dependency.isData()) ->
                        "$projectPath must not depend on $dependency " +
                            "(features may only depend on :domain and :core:*)"

                    projectPath.isData() && dependency.isFeature() ->
                        "$projectPath must not depend on $dependency " +
                            "(data modules must not depend on features)"

                    else -> null
                }
            }

    private fun String.isFeature() = startsWith(":feature:")

    private fun String.isData() = startsWith(":data:")
}
