package pl.prodevcode.learnmobiledev.data.preferences

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages
import pl.prodevcode.learnmobiledev.domain.model.AppLanguage
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider

/**
 * Reads the first preferred language of the system.
 *
 * `preferredLanguages` returns tags such as `pl-PL` or `en-US`, so only the part before
 * the dash is taken to obtain a plain ISO 639-1 code.
 */
class IosLanguageProvider : LanguageProvider {
    override fun language(): String {
        val tag = NSLocale.preferredLanguages.firstOrNull() as? String
            ?: return AppLanguage.DEFAULT.tag
        return tag.substringBefore('-')
    }
}
