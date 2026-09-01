package pl.prodevcode.learnmobiledev.fakeapi.routes

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.prodevcode.learnmobiledev.fakeapi.InfographicCatalog
import pl.prodevcode.learnmobiledev.fakeapi.InfographicRecord
import pl.prodevcode.learnmobiledev.fakeapi.http.ApiResponse
import pl.prodevcode.learnmobiledev.fakeapi.http.RoutingBuilder

const val INFOGRAPHICS_PATH = "/api/v1/infographics"

const val INFOGRAPHIC_IMAGE_PATH = "$INFOGRAPHICS_PATH/{id}/image"

@Serializable
private data class InfographicsResponse(val infographics: List<InfographicRecord>)

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * The infographic service.
 *
 * - `GET /api/v1/infographics` — what is published, as JSON metadata.
 * - `GET /api/v1/infographics/{id}/image` — the picture itself, typed by its stored format.
 *
 * Two endpoints rather than one on purpose. Inlining the bytes into the listing would make
 * the app download every megabyte to render a list of titles, and base64 would add a third
 * again for the privilege. Metadata is small and always needed; a picture is large and
 * needed only when it is actually shown.
 *
 * The `path` in the metadata is the *storage's* path, not a URL the client may fetch: the
 * app addresses an image by id, and where the service keeps its files stays the service's
 * business.
 */
internal fun RoutingBuilder.infographicRoutes(catalog: InfographicCatalog) {
    get(INFOGRAPHICS_PATH) {
        ApiResponse.ok(json.encodeToString(InfographicsResponse(catalog.all())))
    }

    get(INFOGRAPHIC_IMAGE_PATH) { call ->
        val id = call.pathParameters.getValue("id")
        val record = catalog.find(id)
            ?: return@get ApiResponse.notFound("unknown infographic '$id'")

        // The catalogue listed it but the storage has not got it: that is a `404` for the
        // client either way, and the distinction only matters to whoever ships the bundle.
        val bytes = catalog.imageBytes(record)
            ?: return@get ApiResponse.notFound("no image stored for '$id'")

        ApiResponse.ok(bytes = bytes, contentType = record.path.imageContentType())
    }
}

/**
 * The content type follows the stored file, rather than being fixed at one format.
 *
 * The pictures are WebP today because a text-heavy infographic compresses to a fraction of
 * the PNG. Hardcoding `image/png` would have kept working — clients sniff the magic bytes —
 * right up until something downstream trusted the header, which is the kind of lie that
 * surfaces months later in a CDN or a proxy.
 */
private fun String.imageContentType(): String = when {
    endsWith(".webp", ignoreCase = true) -> ApiResponse.WEBP_CONTENT_TYPE
    else -> ApiResponse.PNG_CONTENT_TYPE
}
