package pl.prodevcode.learnmobiledev.data.preferences

import android.content.Context
import java.util.Locale
import pl.prodevcode.learnmobiledev.domain.repository.PlatformLocale

/**
 * Android implementation of [PlatformLocale].
 *
 * The choice is written to its own SharedPreferences file and re-applied by [restore] at
 * startup, before anything reads a resource. `Locale.setDefault` alone would not survive,
 * because a fresh process starts from the system locale again.
 *
 * `AppCompatDelegate.setApplicationLocales` would do this without a restart, but it drags
 * in AppCompat, only exists on Android, and would leave the two platforms behaving
 * differently for the same action. A restart on both is the honest option, and it is what
 * the user is asked for.
 */
class AndroidPlatformLocale(context: Context) : PlatformLocale {

    private val preferences =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    override fun current(): String = Locale.getDefault().language

    override fun apply(tag: String) {
        preferences.edit().putString(KEY, tag).apply()
    }

    companion object {
        private const val NAME = "learn_mobile_dev_locale"
        private const val KEY = "app_locale"

        /**
         * Applies the stored choice to this process.
         *
         * Called from `Activity.onCreate` **before** the content is set: Compose Resources
         * reads `Locale.getDefault()` when it resolves a string, so setting it afterwards
         * would leave the first frames in the wrong language.
         *
         * Reading SharedPreferences on the main thread is normally worth avoiding, but this
         * is a single small file that must be read before the first frame; deferring it
         * would mean rendering in the wrong language and then flipping.
         */
        fun restore(context: Context) {
            val tag = context.applicationContext
                .getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .getString(KEY, null)
                ?: return

            Locale.setDefault(Locale.forLanguageTag(tag))
        }
    }
}
