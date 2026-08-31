package pl.prodevcode.learnmobiledev.fakeapi

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import pl.prodevcode.learnmobiledev.fakeapi.http.ApiRequest
import pl.prodevcode.learnmobiledev.fakeapi.http.ApiResponse
import pl.prodevcode.learnmobiledev.fakeapi.http.routing
import pl.prodevcode.learnmobiledev.fakeapi.routes.contentRoutes
import pl.prodevcode.learnmobiledev.fakeapi.routes.userRoutes

/**
 * Knobs that make the fake behave like a real network rather than a function call.
 *
 * @param latencyMillis artificial round-trip time. Without it every response arrives in
 *   the same frame, loading states never render, and races stay hidden until a real device
 *   on a real connection finds them.
 * @param unavailable consulted per request. A lambda rather than a flag, so the app can
 *   flip the backend into a failing state at runtime and exercise its error paths.
 */
data class FakeBackendConfig(
    val latencyMillis: Long = 150,
    val unavailable: () -> Boolean = { false },
)

/**
 * An in-process stand-in for the content service.
 *
 * The point is that only the *transport* is fake. Above it there is a genuine HTTP surface
 * — paths, query parameters, status codes, headers — and below it genuine routing and
 * storage. The app talks to it with an ordinary Ktor client and cannot tell the
 * difference; replacing this with a deployed server means changing a base URL and an
 * engine, nothing else.
 *
 * It runs on every target, including iOS, which a real embedded server would not.
 */
object FakeBackend {

    /** Any absolute host works, since no packet ever leaves the process. */
    const val BASE_URL: String = "https://fake.learnmobiledev.local"

    /**
     * Fault injection, requested per call.
     *
     * A client that sends `X-Fake-Fault: unavailable` gets a `503` instead of an answer.
     * The demo needs a *failing endpoint*, not a failing app, and a header keeps that
     * decision on the wire: the app asks for an outage, the server produces it, and the
     * error path under test is the genuine one. Scoping it to the request also means
     * flipping the switch on the users screen does not knock the content service over.
     */
    const val FAULT_HEADER: String = "X-Fake-Fault"

    const val FAULT_UNAVAILABLE: String = "unavailable"

    /**
     * The service's transport, and nothing else. A server has no business configuring its
     * callers' clients — timeouts, retries, logging and content negotiation belong to
     * whoever does the calling, and are the same decisions against a deployed backend.
     *
     * @param storage defaults to the documents bundled with this module. Callers are not
     *   expected to pass one — the service owns its data. Tests override it to stand the
     *   backend up on fixtures.
     * @param users the user table, likewise owned by the service. It holds the favorites
     *   accepted so far, which is why a single instance serves the whole app.
     */
    fun createEngine(
        languages: LanguageCatalog,
        config: FakeBackendConfig = FakeBackendConfig(),
        storage: ContentStorage = BundledContentStorage(),
        users: UserDirectory = UserDirectory(),
    ): HttpClientEngine {
        val router = routing {
            contentRoutes(storage, languages)
            userRoutes(users)
        }
        return MockEngine { request ->
            if (config.latencyMillis > 0) delay(config.latencyMillis)

            val apiRequest = request.toApiRequest()
            val response = when {
                config.unavailable() || apiRequest.requestsOutage() ->
                    ApiResponse.serviceUnavailable("the service is temporarily unavailable")

                else -> router.handle(apiRequest)
            }

            respondWith(response)
        }
    }
}

private fun ApiRequest.requestsOutage(): Boolean =
    header(FakeBackend.FAULT_HEADER) == FakeBackend.FAULT_UNAVAILABLE

private suspend fun HttpRequestData.toApiRequest(): ApiRequest = ApiRequest(
    method = method.value,
    path = url.encodedPath,
    query = url.parameters.entries().associate { (name, values) -> name to values.first() },
    headers = headers.entries().associate { (name, values) -> name to values.first() },
    body = body.toByteArray().decodeToString(),
)

private fun MockRequestHandleScope.respondWith(response: ApiResponse): HttpResponseData = respond(
    content = response.body,
    status = HttpStatusCode.fromValue(response.status),
    headers = Headers.build {
        append(HttpHeaders.ContentType, response.contentType)
        response.headers.forEach { (name, value) -> append(name, value) }
    },
)
