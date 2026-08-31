package pl.prodevcode.learnmobiledev.fakeapi

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
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
     * The service's transport, and nothing else. A server has no business configuring its
     * callers' clients — timeouts, retries, logging and content negotiation belong to
     * whoever does the calling, and are the same decisions against a deployed backend.
     *
     * @param storage defaults to the documents bundled with this module. Callers are not
     *   expected to pass one — the service owns its data. Tests override it to stand the
     *   backend up on fixtures.
     */
    fun createEngine(
        languages: LanguageCatalog,
        config: FakeBackendConfig = FakeBackendConfig(),
        storage: ContentStorage = BundledContentStorage(),
    ): HttpClientEngine {
        val router = routing { contentRoutes(storage, languages) }
        return MockEngine { request ->
            if (config.latencyMillis > 0) delay(config.latencyMillis)

            val response = if (config.unavailable()) {
                ApiResponse.serviceUnavailable("the content service is temporarily unavailable")
            } else {
                router.handle(request.toApiRequest())
            }

            respondWith(response)
        }
    }
}

private fun HttpRequestData.toApiRequest(): ApiRequest = ApiRequest(
    method = method.value,
    path = url.encodedPath,
    query = url.parameters.entries().associate { (name, values) -> name to values.first() },
)

private fun MockRequestHandleScope.respondWith(response: ApiResponse): HttpResponseData = respond(
    content = response.body,
    status = HttpStatusCode.fromValue(response.status),
    headers = Headers.build {
        append(HttpHeaders.ContentType, response.contentType)
        response.headers.forEach { (name, value) -> append(name, value) }
    },
)
