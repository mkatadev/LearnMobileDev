package pl.prodevcode.learnmobiledev.fakeapi

import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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

    /** Roles are values stored on user rows, so they are data and not localized either. */
    @Test
    fun `the role catalogue ships with the service`() {
        val catalogue = File(root, "roles.json")

        assertTrue(
            catalogue.isFile && catalogue.length() > 0,
            "the role catalogue is missing from the bundle: $catalogue",
        )
        assertTrue(roles().isNotEmpty(), "the role catalogue is empty")
    }

    /**
     * The service refuses a role it does not publish, so a seed row holding one would be a
     * user the app cannot save without first changing their role — a broken row shipped as
     * demo data.
     */
    @Test
    fun `every seed user holds a role the service publishes`() {
        val roles = roles()

        seedRoles().forEach { role ->
            assertTrue(role in roles, "seed user holds unpublished role '$role'; published: $roles")
        }
    }

    /**
     * An infographic that is listed but not stored is a `404` the reader meets instead of
     * the content, and the listing is the only thing claiming the picture should be there.
     */
    @Test
    fun `every published infographic has its image in the bundle`() {
        val infographics = infographics()

        assertTrue(infographics.isNotEmpty(), "the infographic catalogue is empty")
        infographics.forEach { record ->
            val image = File(root.parentFile, record.path)

            assertTrue(
                image.isFile && image.length() > 0,
                "'${record.id}' points at a missing image: $image",
            )
            // The app reserves space from these before the bytes arrive, so a zero here
            // would collapse the layout rather than merely look wrong.
            assertTrue(record.width > 0 && record.height > 0, "'${record.id}' has no dimensions")
        }
    }

    private fun roles(): Set<String> =
        json.decodeFromString<BundledRoleTable>(File(root, "roles.json").readText()).roles.toSet()

    private fun infographics(): List<BundledInfographic> =
        json.decodeFromString<BundledInfographicTable>(
            File(root, "infographics.json").readText(),
        ).infographics

    private fun seedRoles(): List<String> =
        json.decodeFromString<BundledUserTable>(File(root, "users.json").readText()).users.map { it.role }
}

private val json = Json { ignoreUnknownKeys = true }

@Serializable
private data class BundledRoleTable(val roles: List<String>)

@Serializable
private data class BundledUserTable(val users: List<BundledSeedUser>)

@Serializable
private data class BundledSeedUser(val role: String)

@Serializable
private data class BundledInfographicTable(val infographics: List<BundledInfographic>)

@Serializable
private data class BundledInfographic(
    val id: String,
    val width: Int,
    val height: Int,
    val path: String,
)
