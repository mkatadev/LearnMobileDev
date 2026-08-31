package pl.prodevcode.learnmobiledev.domain.repository

/**
 * Port exposing the language the device is currently set to.
 *
 * Content is requested per language, so every call to the content service carries one.
 * Asking the platform for the current language is a platform concern — hence a port,
 * implemented per target.
 *
 * The returned value is a raw platform tag, which may well be a language this app does not
 * translate. Narrowing it to a supported one is not done here: for content it is the
 * backend's job (`LanguageCatalog`), and for the bundled UI strings it is `LocalizedAsset`.
 * Either way the tag travels unmodified, so the decision stays in one place per consumer.
 */
fun interface LanguageProvider {

    /** ISO 639-1 code of the current language, for example `en` or `pl`. */
    fun language(): String
}
