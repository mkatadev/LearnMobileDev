package pl.prodevcode.learnmobiledev.data

import pl.prodevcode.learnmobiledev.domain.model.AppLanguage
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider

/**
 * The language actually used to load content.
 *
 * A mutable holder rather than a plain value, because the stored preference is read from
 * disk asynchronously while [LanguageProvider.language] is synchronous — data sources need
 * an answer the moment they open a file.
 *
 * The initial value comes from the device, so content is already correct on the very first
 * render even before the preference has been restored.
 */
class EffectiveLanguage(
    private val deviceLanguage: LanguageProvider,
) : LanguageProvider {

    private var current: AppLanguage =
        AppLanguage.fromTag(deviceLanguage.language()) ?: AppLanguage.DEFAULT

    /** The language currently in effect. */
    fun current(): AppLanguage = current

    /** Applies a choice. Returns true when the effective language actually changed. */
    fun apply(language: AppLanguage): Boolean {
        val changed = current != language
        current = language
        return changed
    }

    override fun language(): String = current.tag
}
