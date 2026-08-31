package pl.prodevcode.learnmobiledev.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.url

/**
 * The app's HTTP client, configured by the app rather than by whatever it happens to be
 * talking to. The engine is the only part that differs between the in-process fake and a
 * deployed service; everything above it — the base URL, logging, and any future timeout,
 * retry or serialization policy — is a client-side decision and stays here.
 *
 * @param logTraffic prints every request and response. Off by default because [logHttp]
 *   is a platform API that does not exist on the JVM unit tests run on; the app switches
 *   it on.
 */
fun createContentHttpClient(
    engine: HttpClientEngine,
    baseUrl: String,
    logTraffic: Boolean = false,
): HttpClient = HttpClient(engine) {
    install(DefaultRequest) { url(baseUrl) }
    install(Logging) {
        logger = PlatformLogger
        level = if (logTraffic) LogLevel.ALL else LogLevel.NONE
    }
}

private object PlatformLogger : Logger {
    override fun log(message: String) {
        logHttp(message)
    }
}
