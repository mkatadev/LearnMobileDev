package pl.prodevcode.learnmobiledev.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider

/** Raised for any answer that is not a `2xx`, so callers never parse an error page as content. */
class ContentApiException(
    val status: Int,
    message: String,
) : Exception(message)

/**
 * The app's client for the content service.
 *
 * This is an ordinary Ktor client against an ordinary HTTP API; that the service currently
 * runs in-process (see the `:fakeApi` module) is invisible here and must stay that way.
 * Pointing the app at a deployed server is a change of engine and base URL, nothing more.
 *
 * The language is sent as a *preference*, not a decision: the server answers with whatever
 * translation it actually has, and reports it back in `Content-Language`. Keeping the
 * fallback on the server side means the app cannot serve a locale the backend never had.
 */
class ContentApi(
    private val client: HttpClient,
    private val languageProvider: LanguageProvider,
) {

    suspend fun fetchDocument(resource: String): String {
        val response = client.get("$CONTENT_PATH/$resource") {
            parameter("lang", languageProvider.language())
        }

        if (!response.status.isSuccess()) {
            throw ContentApiException(
                status = response.status.value,
                message = "GET $CONTENT_PATH/$resource failed with ${response.status}",
            )
        }
        return response.bodyAsText()
    }

    private companion object {
        const val CONTENT_PATH = "/api/v1/content"
    }
}
