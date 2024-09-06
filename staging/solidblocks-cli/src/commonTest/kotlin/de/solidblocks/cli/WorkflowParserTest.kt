package de.solidblocks.cli

import de.solidblocks.cli.utils.Error
import de.solidblocks.cli.utils.Success
import de.solidblocks.cli.workflow.WorkflowParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkflowParserTest {

    @Test
    fun testParseWorkflow() {
        val rawYml = """
            requirements:
              - command: ping
              - file: /some/file

            tasks:
              - name: task1
                help: "some helpful hints"
                exec:
                  command: env
              - name: task2
                ghi: jkl
            """.trimIndent()

        val result = WorkflowParser.parse(rawYml)
        println(result)
        assertTrue(result is Success)
        assertEquals(2, result.data.tasks.size)

        assertEquals("task1", result.data.tasks[0].name)
        assertEquals("some helpful hints", result.data.tasks[0].help)
        assertEquals("task2", result.data.tasks[1].name)
    }

    @Test
    fun testTasksIsNotAList() {
        val rawYml = """
            tasks:
              task1:
              task2:
              task3:
              task4:
            """.trimIndent()

        val result = WorkflowParser.parse(rawYml)
        assertTrue(result is Error)
        assertEquals("failed to parse workflow file: 'tasks' should be a list line 1 colum 1", result.error)
    }

    @Test
    fun testInvalidSyntax() {

        val rawYml = """
            %§$"%"§$%§"
            """.trimIndent()

        val result = WorkflowParser.parse(rawYml)
        assertTrue(result is Error)
        assertTrue(result.error.startsWith("failed to parse workflow file"))
    }

    @Test
    fun testTaskIsNotAMap() {
        val rawYml = """
            tasks:
                - task1:
                    abcdef
            """.trimIndent()

        val result = WorkflowParser.parse(rawYml)
        assertTrue(result is Error)
        assertEquals("missing 'name' for task definition at line 2 colum 7", result.error)
    }

}