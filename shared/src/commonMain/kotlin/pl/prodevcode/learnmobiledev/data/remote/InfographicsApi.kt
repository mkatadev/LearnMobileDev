package pl.prodevcode.learnmobiledev.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.resources.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import pl.prodevcode.learnmobiledev.data.infographic.InfographicDto
import pl.prodevcode.learnmobiledev.data.infographic.InfographicsResponseDto

/** Any answer that is not a `2xx`. The status is kept, because the caller maps it. */
class InfographicsApiException(
    val status: Int,
    message: String,
) : Exception(message)

/**
 * The app's client for the infographic service.
 *
 * Metadata and pictures are separate calls, mirroring the service: the listing is small and
 * always needed, a picture is a megabyte and needed only when shown. Fetching them together
 * would mean downloading every image to draw a list of titles.
 */
class InfographicsApi(
    private val client: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {

    // `internal`, because a DTO must not leave the data layer: the repository above maps
    // it to the domain, and a public signature here would let it escape the module.
    internal suspend fun getInfographics(): List<InfographicDto> {
        val response = client.get(ApiRoutes.V1.Infographics())
        if (!response.status.isSuccess()) response.fail("GET infographics")
        return json.decodeFromString<InfographicsResponseDto>(response.bodyAsText()).infographics
    }

    /** Bytes, not text: reading a PNG as a string mangles it beyond recovery. */
    suspend fun getImage(id: String): ByteArray {
        val response = client.get(ApiRoutes.V1.Infographics.Image(id = id))
        if (!response.status.isSuccess()) response.fail("GET infographic image")
        return response.bodyAsBytes()
    }

    private fun HttpResponse.fail(call: String): Nothing =
        throw InfographicsApiException(status.value, "$call failed with $status")
}
