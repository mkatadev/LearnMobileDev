package pl.prodevcode.learnmobiledev.fakeapi.http

/**
 * An incoming request, stripped down to what a route actually needs to decide.
 *
 * Deliberately *not* a Ktor type. The routing layer of this fake backend must be testable
 * without spinning up an engine, exactly like a real server's handlers are tested against
 * a request abstraction rather than a socket.
 */
data class ApiRequest(
    val method: String,
    val path: String,
    val query: Map<String, String> = emptyMap(),
)

/**
 * A route match: the request plus the values captured from the path template.
 *
 * `/api/v1/content/{resource}` matched against `/api/v1/content/lessons` yields
 * `pathParameters = mapOf("resource" to "lessons")`.
 */
data class ApiCall(
    val request: ApiRequest,
    val pathParameters: Map<String, String> = emptyMap(),
)

/**
 * A response as the backend produces it, before the transport turns it into bytes.
 *
 * The status is a plain [Int] rather than a Ktor `HttpStatusCode` so that the whole
 * routing layer stays free of client-library types.
 */
data class ApiResponse(
    val status: Int,
    val body: String,
    val contentType: String = JSON_CONTENT_TYPE,
    val headers: Map<String, String> = emptyMap(),
) {
    companion object {
        const val JSON_CONTENT_TYPE: String = "application/json"

        fun ok(body: String, headers: Map<String, String> = emptyMap()): ApiResponse =
            ApiResponse(status = 200, body = body, headers = headers)

        fun notFound(message: String): ApiResponse = error(404, message)

        fun serviceUnavailable(message: String): ApiResponse = error(503, message)

        /**
         * Errors carry a JSON body too. A backend that answers `404` with an empty body
         * teaches the client nothing, and the app would have no message to show.
         */
        fun error(status: Int, message: String): ApiResponse =
            ApiResponse(status = status, body = errorBody(status, message))

        private fun errorBody(status: Int, message: String): String =
            """{"error":{"status":$status,"message":"${message.escapeJson()}"}}"""

        private fun String.escapeJson(): String =
            replace("\\", "\\\\").replace("\"", "\\\"")
    }
}
