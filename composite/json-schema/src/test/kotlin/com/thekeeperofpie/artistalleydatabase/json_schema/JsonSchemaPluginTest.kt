package com.thekeeperofpie.artistalleydatabase.json_schema

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * A simple unit test for the 'com.thekeeperofpie.artistalleydatabase.json_schema' plugin.
 */
class JsonSchemaPluginTest {
    @Test fun `plugin registers task`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.thekeeperofpie.artistalleydatabase.json_schema")

        // Verify the result
        assertNotNull(project.tasks.findByName("generateJsonSchemaClasses"))
    }
}
