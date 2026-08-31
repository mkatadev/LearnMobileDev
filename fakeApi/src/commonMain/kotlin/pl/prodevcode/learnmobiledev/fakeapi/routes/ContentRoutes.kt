package pl.prodevcode.learnmobiledev.fakeapi.routes

import pl.prodevcode.learnmobiledev.fakeapi.ContentStorage
import pl.prodevcode.learnmobiledev.fakeapi.LanguageCatalog
import pl.prodevcode.learnmobiledev.fakeapi.http.ApiResponse
import pl.prodevcode.learnmobiledev.fakeapi.http.RoutingBuilder

/** The documents this backend publishes. Anything else is a `404`, not a crash. */
internal val CONTENT_RESOURCES = setOf("lessons", "questions", "scenarios")

internal const val CONTENT_PATH = "/api/v1/content/{resource}"

/**
 * `GET /api/v1/content/{resource}?lang=xx`
 *
 * Returns the stored document verbatim. The backend does **not** re-parse or reshape it:
 * its contract is the JSON payload, and the app's parsers own its interpretation. That
 * keeps the fake honest — swapping it for a real HTTP service later changes nothing above
 * the transport.
 */
internal fun RoutingBuilder.contentRoutes(
    storage: ContentStorage,
    languages: LanguageCatalog,
) {
    get(CONTENT_PATH) { call ->
        val resource = call.pathParameters.getValue("resource")
        if (resource !in CONTENT_RESOURCES) {
            return@get ApiResponse.notFound("unknown resource '$resource'")
        }

        val language = languages.resolve(call.request.query["lang"])
        val document = storage.read(language, resource)
            ?: return@get ApiResponse.notFound("'$resource' is not available in '$language'")

        ApiResponse.ok(document, headers = mapOf("Content-Language" to language))
    }
}
