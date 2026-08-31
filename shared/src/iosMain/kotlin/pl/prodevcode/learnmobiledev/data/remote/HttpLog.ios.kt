package pl.prodevcode.learnmobiledev.data.remote

import platform.Foundation.NSLog

/**
 * NSLog rather than print: it carries a timestamp and the process identity, and it reaches
 * Console.app and device logs, not only Xcode's debug pane.
 */
internal actual fun logHttp(message: String) {
    NSLog("[%@] %@", HTTP_LOG_TAG, message)
}
