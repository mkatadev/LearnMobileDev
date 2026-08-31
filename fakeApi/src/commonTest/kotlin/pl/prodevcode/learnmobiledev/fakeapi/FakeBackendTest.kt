package pl.prodevcode.learnmobiledev.fakeapi

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

/**
 * The fake backend is production code for this app, so it is tested like a backend:
 * through its HTTP surface, by status code and payload — never by reaching into the
 * routing internals.
 */
class FakeBackendTest {

    private val storage = ContentStorage { language, resource ->
        when (language to resource) {
            "en" to "lessons" -> """{"lessons":["en"]}"""
            "pl" to "lessons" -> """{"lessons":["pl"]}"""
            "en" to "questions" -> """{"questions":[]}"""
            else -> null
        }
    }

    private val languages = LanguageCatalog(supported = setOf("en", "pl"), default = "en")

    private fun client(config: FakeBackendConfig = FakeBackendConfig(latencyMillis = 0)) =
        FakeBackend.createClient(languages, config, storage)

    @Test
    fun `a stored document is served verbatim`() = runTest {
        val response = client().get("/api/v1/content/lessons") { url.parameters.append("lang", "pl") }

        assertEquals(200, response.status.value)
        assertEquals("""{"lessons":["pl"]}""", response.bodyAsText())
    }

    /** Language negotiation is the server's job, so an unknown tag must not 404. */
    @Test
    fun `an unsupported language falls back to the default`() = runTest {
        val response = client().get("/api/v1/content/lessons") { url.parameters.append("lang", "de") }

        assertEquals(200, response.status.value)
        assertEquals("""{"lessons":["en"]}""", response.bodyAsText())
        assertEquals("en", response.headers["Content-Language"])
    }

    @Test
    fun `a regional tag is narrowed to its base language`() = runTest {
        val response = client().get("/api/v1/content/lessons") { url.parameters.append("lang", "pl-PL") }

        assertEquals("pl", response.headers["Content-Language"])
    }

    @Test
    fun `an unknown resource is not found`() = runTest {
        val response = client().get("/api/v1/content/recipes")

        assertEquals(404, response.status.value)
    }

    @Test
    fun `an unknown path is not found`() = runTest {
        val response = client().get("/api/v1/nonsense")

        assertEquals(404, response.status.value)
    }

    /** A document the storage does not hold is a 404, not an empty body. */
    @Test
    fun `a resource missing for a language is not found`() = runTest {
        val response = client().get("/api/v1/content/questions") { url.parameters.append("lang", "pl") }

        assertEquals(404, response.status.value)
    }

    @Test
    fun `the outage switch fails every request`() = runTest {
        val response = client(FakeBackendConfig(latencyMillis = 0, unavailable = { true }))
            .get("/api/v1/content/lessons")

        assertEquals(503, response.status.value)
    }

    @Test
    fun `errors carry a machine readable body`() = runTest {
        val body = client().get("/api/v1/content/recipes").bodyAsText()

        assertEquals("""{"error":{"status":404,"message":"unknown resource 'recipes'"}}""", body)
    }
}
