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
    val headers: Map<String, String> = emptyMap(),
    val body: String = "",
) {
    /** Header lookup is case-insensitive on the wire, so it must be here too. */
    fun header(name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
}

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
 *
 * The payload is [bytes], not a string: an image is not text, and decoding a PNG into a
 * `String` and back mangles it. JSON routes keep passing strings and are encoded here.
 */
data class ApiResponse(
    val status: Int,
    val bytes: ByteArray,
    val contentType: String = JSON_CONTENT_TYPE,
    val headers: Map<String, String> = emptyMap(),
) {
    /** Convenience for the JSON routes, which reason in text. */
    val body: String get() = bytes.decodeToString()

    // A ByteArray field means the generated equals/hashCode compare by identity, which
    // would quietly break any test asserting on two equal responses.
    override fun equals(other: Any?): Boolean = this === other ||
        (
            other is ApiResponse &&
                status == other.status &&
                bytes.contentEquals(other.bytes) &&
                contentType == other.contentType &&
                headers == other.headers
            )

    override fun hashCode(): Int {
        var result = status
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + headers.hashCode()
        return result
    }

    companion object {
        const val JSON_CONTENT_TYPE: String = "application/json"

        const val PNG_CONTENT_TYPE: String = "image/png"

        const val WEBP_CONTENT_TYPE: String = "image/webp"

        fun ok(body: String, headers: Map<String, String> = emptyMap()): ApiResponse =
            ApiResponse(status = 200, bytes = body.encodeToByteArray(), headers = headers)

        /** Binary payloads travel as bytes and declare their own type. */
        fun ok(
            bytes: ByteArray,
            contentType: String,
            headers: Map<String, String> = emptyMap(),
        ): ApiResponse = ApiResponse(
            status = 200,
            bytes = bytes,
            contentType = contentType,
            headers = headers,
        )

        /** A row was created. The body is the stored resource, including its assigned id. */
        fun created(body: String): ApiResponse =
            ApiResponse(status = 201, bytes = body.encodeToByteArray())

        /**
         * Applied, and there is nothing to say about it. A delete has no resource left to
         * return, and answering `200` with an invented body would only invite the client to
         * parse something the server does not really have.
         */
        fun noContent(): ApiResponse = ApiResponse(status = 204, bytes = ByteArray(0))

        fun badRequest(message: String): ApiResponse = error(400, message)

        fun notFound(message: String): ApiResponse = error(404, message)

        /** The request was understood, but the server refuses to apply it. */
        fun conflict(message: String): ApiResponse = error(409, message)

        /** Well-formed, but the values are not ones the server will store. */
        fun unprocessable(message: String): ApiResponse = error(422, message)

        fun serviceUnavailable(message: String): ApiResponse = error(503, message)

        /**
         * Errors carry a JSON body too. A backend that answers `404` with an empty body
         * teaches the client nothing, and the app would have no message to show.
         */
        fun error(status: Int, message: String): ApiResponse =
            ApiResponse(status = status, bytes = errorBody(status, message).encodeToByteArray())

        private fun errorBody(status: Int, message: String): String =
            """{"error":{"status":$status,"message":"${message.escapeJson()}"}}"""

        private fun String.escapeJson(): String =
            replace("\\", "\\\\").replace("\"", "\\\"")
    }
}
