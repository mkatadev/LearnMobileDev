package pl.prodevcode.learnmobiledev.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import pl.prodevcode.learnmobiledev.data.preferences.KeyValueLanguagePreferences
import pl.prodevcode.learnmobiledev.domain.model.AppLanguage
import pl.prodevcode.learnmobiledev.domain.repository.InMemoryKeyValueStore
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider

/**
 * Language resolution: the device decides the initial value, an explicit choice wins
 * afterwards, and anything unsupported falls back to English.
 */
class EffectiveLanguageTest {

    private fun device(tag: String) = LanguageProvider { tag }

    @Test
    fun `a Polish device starts in Polish`() {
        assertEquals(AppLanguage.Polish, EffectiveLanguage(device("pl")).current())
    }

    @Test
    fun `an English device starts in English`() {
        assertEquals(AppLanguage.English, EffectiveLanguage(device("en")).current())
    }

    @Test
    fun `an unsupported device language falls back to English`() {
        assertEquals(AppLanguage.English, EffectiveLanguage(device("de")).current())
    }

    @Test
    fun `region tags and casing are tolerated`() {
        assertEquals(AppLanguage.Polish, EffectiveLanguage(device("PL")).current())
    }

    @Test
    fun `applying a different language reports a change`() {
        val effective = EffectiveLanguage(device("en"))

        assertTrue(effective.apply(AppLanguage.Polish))
        assertEquals("pl", effective.language())
    }

    @Test
    fun `applying the same language reports no change`() {
        val effective = EffectiveLanguage(device("en"))

        // This is what stops screens from reloading their content for nothing.
        assertFalse(effective.apply(AppLanguage.English))
    }

    @Test
    fun `no stored preference means no choice was made`() = runTest {
        val preferences = KeyValueLanguagePreferences(InMemoryKeyValueStore())

        assertEquals(null, preferences.getLanguage())
    }

    @Test
    fun `a stored language is read back`() = runTest {
        val preferences = KeyValueLanguagePreferences(InMemoryKeyValueStore())

        preferences.setLanguage(AppLanguage.Polish)

        assertEquals(AppLanguage.Polish, preferences.getLanguage())
    }

    @Test
    fun `an unrecognised stored value is treated as no choice`() = runTest {
        val store = InMemoryKeyValueStore()
        store.putString("app_language", "Klingon")

        assertEquals(null, KeyValueLanguagePreferences(store).getLanguage())
    }
}
