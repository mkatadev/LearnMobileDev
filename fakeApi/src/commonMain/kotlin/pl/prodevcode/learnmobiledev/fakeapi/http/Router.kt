package pl.prodevcode.learnmobiledev.fakeapi.http

/**
 * A single route: a method, a path template and the code that answers it.
 *
 * The template may contain placeholders in braces, e.g. `/api/v1/content/{resource}`.
 * Each placeholder matches exactly one path segment.
 */
class Route internal constructor(
    private val method: String,
    template: String,
    private val handler: suspend (ApiCall) -> ApiResponse,
) {
    private val segments: List<String> = template.split("/").filter { it.isNotEmpty() }

    /**
     * Returns the captured path parameters when the request matches, or `null` when it
     * does not. `null` rather than an empty map, because "matched with no parameters" and
     * "did not match" are different answers and must not collapse into one.
     */
    internal fun match(request: ApiRequest): Map<String, String>? {
        if (!request.method.equals(method, ignoreCase = true)) return null

        val requestSegments = request.path.split("/").filter { it.isNotEmpty() }
        if (requestSegments.size != segments.size) return null

        val parameters = mutableMapOf<String, String>()
        segments.forEachIndexed { index, segment ->
            val actual = requestSegments[index]
            when {
                segment.startsWith("{") && segment.endsWith("}") ->
                    parameters[segment.substring(1, segment.length - 1)] = actual

                segment != actual -> return null
            }
        }
        return parameters
    }

    internal suspend fun handle(call: ApiCall): ApiResponse = handler(call)
}

/** Collects routes while [routing] runs the configuration block. */
class RoutingBuilder internal constructor() {
    internal val routes = mutableListOf<Route>()

    fun get(template: String, handler: suspend (ApiCall) -> ApiResponse) {
        routes += Route("GET", template, handler)
    }

    /**
     * `PUT` rather than `POST` for the favorite flag: the client states the value it wants
     * and repeating the call changes nothing, which is what makes a retry after a timeout
     * safe.
     */
    fun put(template: String, handler: suspend (ApiCall) -> ApiResponse) {
        routes += Route("PUT", template, handler)
    }

    /**
     * `POST` for creation, because the server assigns the id: the client cannot name the
     * resource it is asking for, and sending the same call twice creates two rows. That is
     * precisely the difference from [put], and the reason a create must not be retried
     * blindly.
     */
    fun post(template: String, handler: suspend (ApiCall) -> ApiResponse) {
        routes += Route("POST", template, handler)
    }

    fun delete(template: String, handler: suspend (ApiCall) -> ApiResponse) {
        routes += Route("DELETE", template, handler)
    }
}

/**
 * Dispatches requests to the first matching route.
 *
 * The distinction between "no route at this path" (404) and "path exists but not for this
 * method" is intentionally *not* modelled: a fake backend that mimics every nuance of HTTP
 * becomes a second product to maintain. It answers 404, which is what the app must handle
 * anyway.
 */
class Router internal constructor(private val routes: List<Route>) {

    suspend fun handle(request: ApiRequest): ApiResponse {
        routes.forEach { route ->
            val parameters = route.match(request)
            if (parameters != null) return route.handle(ApiCall(request, parameters))
        }
        return ApiResponse.notFound("no route for ${request.method} ${request.path}")
    }
}

fun routing(block: RoutingBuilder.() -> Unit): Router =
    Router(RoutingBuilder().apply(block).routes)
