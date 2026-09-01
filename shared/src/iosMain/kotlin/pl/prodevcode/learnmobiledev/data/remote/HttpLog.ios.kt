package pl.prodevcode.learnmobiledev.data.remote

/**
 * `println` rather than `NSLog`.
 *
 * `NSLog` reads its first argument as a *format string*, and everything after it through a
 * variadic ObjC call that Kotlin/Native does not bridge — which crashed the app on launch
 * with `EXC_BAD_ACCESS` inside `objc_opt_respondsToSelector`. HTTP traffic is exactly the
 * wrong payload to hand a formatter: bodies carry `%` (the string catalogue uses `%1$s`,
 * an image is effectively random bytes), so even the single-argument form is one careless
 * edit away from reading arguments nobody passed.
 *
 * `println` has no format string and no bridging, so neither failure mode exists. On
 * Kotlin/Native it goes to stdout, which Xcode's console and `simctl launch --console` both
 * show. That costs the tag-based filtering `NSLog` offered, which is why the tag is written
 * into the line itself.
 */
internal actual fun logHttp(message: String) {
    println("[$HTTP_LOG_TAG] $message")
}
