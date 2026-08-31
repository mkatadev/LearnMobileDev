package pl.prodevcode.learnmobiledev.domain.repository

/**
 * Port exposing the language the device is currently set to.
 *
 * Course content lives in JSON assets, and `Res.readBytes` takes a raw path with no
 * qualifier support, so selecting the right file needs an explicit language. Asking the
 * platform for it is a platform concern — hence a port, implemented per target.
 *
 * The returned value is a raw platform tag, which may well be a language this app does not
 * translate. Mapping it onto a supported language is
 * `AppLanguage.fromTag(...) ?: AppLanguage.DEFAULT`, and that mapping lives in exactly one
 * place: `LocalizedAsset`.
 */
fun interface LanguageProvider {

    /** ISO 639-1 code of the current language, for example `en` or `pl`. */
    fun language(): String
}
