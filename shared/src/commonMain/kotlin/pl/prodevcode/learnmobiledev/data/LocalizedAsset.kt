package pl.prodevcode.learnmobiledev.data

import pl.prodevcode.learnmobiledev.domain.model.AppLanguage
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider

/**
 * Resolves the path of a localized JSON asset.
 *
 * Compose Resources applies qualifiers to `values/` and `drawable/`, but **not** to
 * `files/`, which is a raw directory. Localized content therefore lives in one
 * subdirectory per language:
 *
 * ```
 * files/en/lessons.json   <- default
 * files/pl/lessons.json   <- Polish
 * ```
 *
 * A directory per language rather than a `lessons-pl.json` suffix, so that the complete
 * set of files for a locale is visible at a glance.
 *
 * The set of supported languages is derived from [AppLanguage] rather than repeated here.
 * A second list would be a silent trap: adding a language to the enum but forgetting this
 * file would show the new language in the switcher while still serving English content.
 */
internal object LocalizedAsset {

    fun path(baseName: String, languageProvider: LanguageProvider): String {
        val tag = languageProvider.language().lowercase()
        val language = AppLanguage.fromTag(tag) ?: AppLanguage.DEFAULT
        return "files/${language.tag}/$baseName.json"
    }
}
