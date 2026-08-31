package pl.prodevcode.learnmobiledev.data.preferences

import java.util.Locale
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider

/** Reads the language from the JVM default locale, which Android keeps in sync with settings. */
class AndroidLanguageProvider : LanguageProvider {
    override fun language(): String = Locale.getDefault().language
}
