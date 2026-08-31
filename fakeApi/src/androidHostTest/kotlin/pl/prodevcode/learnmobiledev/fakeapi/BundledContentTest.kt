package pl.prodevcode.learnmobiledev.fakeapi

import java.io.File
import pl.prodevcode.learnmobiledev.fakeapi.routes.CONTENT_RESOURCES
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards the service's data set: every document, in every language it claims to serve.
 *
 * The routing tests all substitute a fixture, which is right for testing routing but leaves
 * the shipped documents unexercised — moving or renaming one would keep the whole suite
 * green and break the app on launch. This test is what turns that into a build failure.
 *
 * It asserts against the resource directory rather than through [BundledContentStorage],
 * because `Res.readBytes` needs an Android runtime that a JVM test does not have. What it
 * cannot prove is that the packaging still works; that is what
 * `ContentApiIntegrationTest` in `:shared` and the release build cover.
 */
class BundledContentTest {

    private val root = File("src/commonMain/composeResources/files")

    private val languages = listOf("en", "pl")

    @Test
    fun `every published resource exists in every served language`() {
        languages.forEach { language ->
            CONTENT_RESOURCES.forEach { resource ->
                val document = File(root, "$language/$resource.json")

                assertTrue(
                    document.isFile && document.length() > 0,
                    "'$resource' is missing from the bundle for '$language': $document",
                )
            }
        }
    }

    /**
     * A stray file would be served by nothing and quietly rot. The route only answers for
     * [CONTENT_RESOURCES], so anything else in the directory is dead weight or a typo.
     */
    @Test
    fun `the bundle holds nothing the service does not publish`() {
        languages.forEach { language ->
            val documents = File(root, language).listFiles().orEmpty()
                .map { it.name.removeSuffix(".json") }

            assertTrue(
                documents.toSet() == CONTENT_RESOURCES,
                "[$language] bundle is $documents but the service publishes $CONTENT_RESOURCES",
            )
        }
    }

    /**
     * The user table is not localized — names and email addresses read the same in every
     * language — so it sits beside the language folders rather than inside them.
     */
    @Test
    fun `the user table ships with the service`() {
        val table = File(root, "users.json")

        assertTrue(
            table.isFile && table.length() > 0,
            "the user table is missing from the bundle: $table",
        )
    }
}
