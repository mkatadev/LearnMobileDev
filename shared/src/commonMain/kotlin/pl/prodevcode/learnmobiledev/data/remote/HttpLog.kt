package pl.prodevcode.learnmobiledev.data.remote

/** Prefix on every logged line, so the console can be filtered down to HTTP traffic. */
internal const val HTTP_LOG_TAG: String = "ContentHttp"

/**
 * Writes one line of HTTP traffic to the platform's logging facility.
 *
 * Ktor's default logger delegates to SLF4J, which has no provider on Android and none at
 * all on Kotlin/Native, so its output never reaches a console. `println` would, but only
 * as unstructured stdout: no tag to filter on, no severity, and no way for the platform to
 * drop it in a release build. Each target's own logger gives all three.
 */
internal expect fun logHttp(message: String)
