package pl.prodevcode.learnmobiledev.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import pl.prodevcode.learnmobiledev.data.remote.UsersApi
import pl.prodevcode.learnmobiledev.data.remote.createContentHttpClient
import pl.prodevcode.learnmobiledev.data.repository.ApiUserRepository
import pl.prodevcode.learnmobiledev.domain.model.AppLanguage
import pl.prodevcode.learnmobiledev.domain.repository.UserSyncException
import pl.prodevcode.learnmobiledev.fakeapi.FakeBackend
import pl.prodevcode.learnmobiledev.fakeapi.FakeBackendConfig
import pl.prodevcode.learnmobiledev.fakeapi.LanguageCatalog
import pl.prodevcode.learnmobiledev.fakeapi.UserDirectory
import pl.prodevcode.learnmobiledev.fakeapi.UserStorage
import pl.prodevcode.learnmobiledev.fakeapi.routes.REJECTED_FAVORITE_USER_ID

/**
 * The users path end to end: repository → Ktor client → fake backend → user table.
 *
 * What it proves is that the screen's data no longer lives in the app. Every assertion
 * here travels over HTTP, and a broken URL, a wrong status mapping or a missing body
 * would fail it — none of which the reducer tests can see.
 */
class ApiUserRepositoryTest {

    private val seed = """
        {
          "users": [
            {"id":"u-1","name":"Anna Nowak","email":"anna@example.com","role":"Android Developer"},
            {"id":"u-2","name":"Bob Smith","email":"bob@example.com","role":"iOS Developer"},
            {"id":"$REJECTED_FAVORITE_USER_ID","name":"Dana Fixed","email":"dana@example.com","role":"QA Engineer"}
          ]
        }
    """.trimIndent()

    private fun repository(): ApiUserRepository {
        val client = createContentHttpClient(
            engine = FakeBackend.createEngine(
                languages = LanguageCatalog(AppLanguage.SUPPORTED_TAGS, AppLanguage.DEFAULT.tag),
                // No artificial latency: this test asserts wiring, not loading states.
                config = FakeBackendConfig(latencyMillis = 0),
                users = UserDirectory(UserStorage { seed }),
            ),
            baseUrl = FakeBackend.BASE_URL,
        )
        return ApiUserRepository(UsersApi(client))
    }

    @Test
    fun `users are fetched over http`() = runTest {
        val users = repository().getUsers("")

        assertEquals(3, users.size)
        assertTrue(users.none { it.isFavorite })
    }

    @Test
    fun `the query is answered by the backend`() = runTest {
        val users = repository().getUsers("ios")

        assertEquals(listOf("u-2"), users.map { it.id })
    }

    @Test
    fun `a favorite survives a reload`() = runTest {
        val repository = repository()

        assertTrue(repository.setFavorite("u-1", favorite = true))

        assertTrue(repository.getUsers("").first { it.id == "u-1" }.isFavorite)
    }

    /** A `409` from the server is what triggers the optimistic-update rollback. */
    @Test
    fun `a refused favorite surfaces as a domain failure`() = runTest {
        assertFailsWith<UserSyncException.FavoriteRejected> {
            repository().setFavorite(REJECTED_FAVORITE_USER_ID, favorite = true)
        }
    }


    @Test
    fun `an edit is persisted and normalized by the server`() = runTest {
        val repository = repository()

        val saved = repository.updateUser("u-1", "  Anna Kowalska  ", "anna@example.com", "Tech Lead")

        assertEquals("Anna Kowalska", saved.name)
        assertEquals("Tech Lead", repository.getUsers("").first { it.id == "u-1" }.role)
    }

    /** A `422` is the server refusing the values, and must not read as an outage. */
    @Test
    fun `values the server refuses surface as an invalid user`() = runTest {
        assertFailsWith<UserSyncException.InvalidUser> {
            repository().updateUser("u-1", "Anna", "not-an-email", "Tech Lead")
        }
    }

    @Test
    fun `editing a user that is gone reports it as missing`() = runTest {
        assertFailsWith<UserSyncException.UserNotFound> {
            repository().updateUser("u-999", "Anna", "anna@example.com", "Tech Lead")
        }
    }

    @Test
    fun `the failure switch makes the endpoint answer 503`() = runTest {
        val repository = repository().apply { failNextLoad = true }

        assertFailsWith<UserSyncException.NetworkUnavailable> { repository.getUsers("") }

        repository.failNextLoad = false
        assertTrue(repository.getUsers("").isNotEmpty())
    }
}
