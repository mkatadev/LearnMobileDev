package pl.prodevcode.learnmobiledev.data.remote

import android.util.Log

/**
 * DEBUG rather than INFO: request and response bodies are developer-facing detail, and
 * DEBUG is the level Android strips from release builds by default.
 */
internal actual fun logHttp(message: String) {
    Log.d(HTTP_LOG_TAG, message)
}
