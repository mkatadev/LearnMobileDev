package pl.prodevcode.learnmobiledev.data.preferences

import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages
import pl.prodevcode.learnmobiledev.domain.model.AppLanguage
import pl.prodevcode.learnmobiledev.domain.repository.PlatformLocale

/**
 * iOS implementation of [PlatformLocale].
 *
 * `AppleLanguages` is the list UIKit itself consults when it decides which `.lproj` — and,
 * through `NSLocale.preferredLanguages`, which Compose resource qualifier — applies.
 * Writing it puts the app's choice in front of the system's, and iOS reads it at launch,
 * which is why the change needs a restart.
 *
 * No API restarts an iOS app: `exit(0)` is indistinguishable from a crash and is grounds
 * for rejection on the App Store. The user is asked to do it instead.
 */
class IosPlatformLocale : PlatformLocale {

    override fun current(): String {
        val tag = NSLocale.preferredLanguages.firstOrNull() as? String
            ?: return AppLanguage.DEFAULT.tag
        return tag.substringBefore('-')
    }

    override fun apply(tag: String) {
        NSUserDefaults.standardUserDefaults.setObject(listOf(tag), forKey = KEY)
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    private companion object {
        const val KEY = "AppleLanguages"
    }
}
