package pl.prodevcode.learnmobiledev.domain.repository

/**
 * Port for the locale the **platform** reports, which is the one Compose Resources reads
 * when it picks between `values/` and `values-pl/`.
 *
 * The app cannot simply translate itself: `Res.string` resolves against the system locale,
 * and Compose Multiplatform 1.11 seals that off — `ResourceEnvironment` has an internal
 * constructor and `LocalComposeEnvironment` is internal, so there is no supported way to
 * override it in-process. Changing the language therefore means changing it *on the
 * platform* and starting over, which is why [apply] promises nothing until the next launch.
 *
 * Both implementations write a value the platform itself reads back at startup:
 * `Locale.getDefault()` on Android, `NSLocale.preferredLanguages` on iOS.
 */
interface PlatformLocale {

    /** The language tag currently in effect for this process, e.g. `en`. */
    fun current(): String

    /**
     * Records [tag] as the app's language. Takes effect on the next launch — the caller is
     * expected to tell the user that a restart is needed.
     */
    fun apply(tag: String)
}
