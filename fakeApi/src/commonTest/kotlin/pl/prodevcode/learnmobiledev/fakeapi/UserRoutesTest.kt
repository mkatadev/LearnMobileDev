package pl.prodevcode.learnmobiledev.fakeapi

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import pl.prodevcode.learnmobiledev.fakeapi.routes.REJECTED_FAVORITE_USER_ID

/**
 * The user service, tested the way its client sees it: URLs, status codes and payloads.
 *
 * The directory is stood up on a fixture rather than the shipped table, so these tests
 * describe the *contract* and do not break every time a demo user is renamed.
 */
class UserRoutesTest {

    private val seed = """
        {
          "users": [
            {"id":"u-1","name":"Anna Nowak","email":"anna@example.com","role":"Android Developer"},
            {"id":"u-2","name":"Bob Smith","email":"bob@example.com","role":"iOS Developer"},
            {"id":"$REJECTED_FAVORITE_USER_ID","name":"Dana Fixed","email":"dana@example.com","role":"QA Engineer"}
          ]
        }
    """.trimIndent()

    private val languages = LanguageCatalog(supported = setOf("en"), default = "en")

    /**
     * The roles are a fixture too. Without one the catalogue would be read from the
     * bundled resource, which needs an Android runtime a JVM test does not have — every
     * write would then be refused for holding a role the service does not know.
     */
    private val roles = """{"roles":["Android Developer","iOS Developer","QA Engineer","Tech Lead"]}"""

    private fun client() = HttpClient(
        FakeBackend.createEngine(
            languages = languages,
            config = FakeBackendConfig(latencyMillis = 0),
            users = UserDirectory(UserStorage { seed }),
            roles = RoleCatalog(RoleStorage { roles }),
        ),
    ) {
        install(DefaultRequest) { url(FakeBackend.BASE_URL) }
    }

    @Test
    fun `the directory is served as json`() = runTest {
        val response = client().get("/api/v1/users")

        assertEquals(200, response.status.value)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"u-1\""))
        assertTrue(body.contains("\"isFavorite\":false"))
    }

    /** Searching is the server's job: the client must never download the whole table. */
    @Test
    fun `the query filters server side`() = runTest {
        val body = client().get("/api/v1/users?q=ios").bodyAsText()

        assertTrue(body.contains("\"u-2\""))
        assertFalse(body.contains("\"u-1\""))
    }

    @Test
    fun `a stored favorite comes back on the next search`() = runTest {
        val client = client()

        val saved = client.put("/api/v1/users/u-1/favorite") {
            contentType(ContentType.Application.Json)
            setBody("""{"favorite":true}""")
        }
        assertEquals(200, saved.status.value)
        assertEquals("""{"id":"u-1","favorite":true}""", saved.bodyAsText())

        val body = client.get("/api/v1/users?q=anna").bodyAsText()
        assertTrue(body.contains("\"isFavorite\":true"))
    }

    @Test
    fun `the locked user is refused with a conflict`() = runTest {
        val response = client().put("/api/v1/users/$REJECTED_FAVORITE_USER_ID/favorite") {
            contentType(ContentType.Application.Json)
            setBody("""{"favorite":true}""")
        }

        assertEquals(409, response.status.value)
    }

    @Test
    fun `an unknown user cannot be favorited`() = runTest {
        val response = client().put("/api/v1/users/u-999/favorite") {
            contentType(ContentType.Application.Json)
            setBody("""{"favorite":true}""")
        }

        assertEquals(404, response.status.value)
    }

    @Test
    fun `a malformed body is a bad request`() = runTest {
        val response = client().put("/api/v1/users/u-1/favorite") {
            contentType(ContentType.Application.Json)
            setBody("not json")
        }

        assertEquals(400, response.status.value)
    }


    @Test
    fun `an edit is stored and answered with the whole row`() = runTest {
        val client = client()

        val saved = client.put("/api/v1/users/u-1") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Anna Kowalska","email":"anna@example.com","role":"Tech Lead"}""")
        }

        assertEquals(200, saved.status.value)
        assertTrue(saved.bodyAsText().contains("Tech Lead"))
        assertTrue(client.get("/api/v1/users").bodyAsText().contains("Anna Kowalska"))
    }

    /** The edit must survive alongside the flag, not overwrite it. */
    @Test
    fun `an edit keeps the favorite the server already accepted`() = runTest {
        val client = client()
        client.put("/api/v1/users/u-1/favorite") {
            contentType(ContentType.Application.Json)
            setBody("""{"favorite":true}""")
        }

        val saved = client.put("/api/v1/users/u-1") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Anna K","email":"anna@example.com","role":"Tech Lead"}""")
        }

        assertTrue(saved.bodyAsText().contains("\"isFavorite\":true"))
    }

    @Test
    fun `values the server will not store are unprocessable`() = runTest {
        val client = client()
        val rejected = listOf(
            """{"name":"  ","email":"anna@example.com","role":"Tech Lead"}""",
            """{"name":"Anna","email":"anna@example.com","role":""}""",
            """{"name":"Anna","email":"not-an-email","role":"Tech Lead"}""",
        )

        rejected.forEach { body ->
            val response = client.put("/api/v1/users/u-1") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            assertEquals(422, response.status.value, "expected 422 for $body")
        }
    }

    @Test
    fun `an unknown user cannot be edited`() = runTest {
        val response = client().put("/api/v1/users/u-999") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Anna","email":"anna@example.com","role":"Tech Lead"}""")
        }

        assertEquals(404, response.status.value)
    }

    /** Fault injection is per request, so one screen's outage is not the whole backend's. */
    @Test
    fun `the fault header makes only that call fail`() = runTest {
        val client = client()

        val failed = client.get("/api/v1/users") {
            header(FakeBackend.FAULT_HEADER, FakeBackend.FAULT_UNAVAILABLE)
        }
        assertEquals(503, failed.status.value)

        assertEquals(200, client.get("/api/v1/users").status.value)
    }

    @Test
    fun `the roles the service accepts are published`() = runTest {
        val response = client().get("/api/v1/roles")

        assertEquals(200, response.status.value)
        assertTrue(response.bodyAsText().contains("Tech Lead"))
    }

    /** `201` and the stored row: the client cannot know the id it has just created. */
    @Test
    fun `creating a user assigns an id and returns the row`() = runTest {
        val client = client()

        val created = client.post("/api/v1/users") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Nina Fresh","email":"nina@example.com","role":"Tech Lead"}""")
        }

        assertEquals(201, created.status.value)
        assertTrue(created.bodyAsText().contains("\"id\""), "the created row must carry its id")
        assertTrue(client.get("/api/v1/users").bodyAsText().contains("Nina Fresh"))
    }

    /** An id the seed already uses must never be handed to a new row. */
    @Test
    fun `a created user does not collide with the seed`() = runTest {
        val client = client()

        repeat(3) { index ->
            client.post("/api/v1/users") {
                contentType(ContentType.Application.Json)
                setBody("""{"name":"New $index","email":"new$index@example.com","role":"Tech Lead"}""")
            }
        }

        val ids = Regex("\"id\":\"([^\"]+)\"")
            .findAll(client.get("/api/v1/users").bodyAsText())
            .map { it.groupValues[1] }
            .toList()

        assertEquals(ids.size, ids.toSet().size, "duplicate ids: $ids")
    }

    @Test
    fun `creating a user validates the same way editing does`() = runTest {
        val client = client()
        val rejected = listOf(
            """{"name":"  ","email":"nina@example.com","role":"Tech Lead"}""",
            """{"name":"Nina","email":"not-an-email","role":"Tech Lead"}""",
            """{"name":"Nina","email":"nina@example.com","role":"Astronaut"}""",
        )

        rejected.forEach { body ->
            val response = client.post("/api/v1/users") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            assertEquals(422, response.status.value, "expected 422 for $body")
        }
    }

    /** A role outside the published list is not a role, however well-formed the request. */
    @Test
    fun `a role the service does not offer is refused`() = runTest {
        val response = client().put("/api/v1/users/u-1") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Anna","email":"anna@example.com","role":"Astronaut"}""")
        }

        assertEquals(422, response.status.value)
    }

    @Test
    fun `deleting a user removes it from the directory`() = runTest {
        val client = client()

        val deleted = client.delete("/api/v1/users/u-1")

        assertEquals(204, deleted.status.value)
        assertFalse(client.get("/api/v1/users").bodyAsText().contains("\"u-1\""))
    }

    /** Saying `404` is what lets a client reload a list it disagrees with the server about. */
    @Test
    fun `deleting a user that is gone reports it`() = runTest {
        val client = client()
        client.delete("/api/v1/users/u-1")

        assertEquals(404, client.delete("/api/v1/users/u-1").status.value)
    }

    /** An id is never recycled, so a new row cannot inherit a deleted user's favorite. */
    @Test
    fun `a deleted user does not leave its favorite behind`() = runTest {
        val client = client()
        client.put("/api/v1/users/u-1/favorite") {
            contentType(ContentType.Application.Json)
            setBody("""{"favorite":true}""")
        }
        client.delete("/api/v1/users/u-1")

        val created = client.post("/api/v1/users") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Nina Fresh","email":"nina@example.com","role":"Tech Lead"}""")
        }

        assertTrue(created.bodyAsText().contains("\"isFavorite\":false"))
    }
}
