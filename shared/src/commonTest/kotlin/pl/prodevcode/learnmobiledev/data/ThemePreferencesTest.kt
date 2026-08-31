package pl.prodevcode.learnmobiledev.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import pl.prodevcode.learnmobiledev.data.preferences.KeyValueThemePreferences
import pl.prodevcode.learnmobiledev.domain.model.ThemeMode
import pl.prodevcode.learnmobiledev.domain.repository.InMemoryKeyValueStore
import pl.prodevcode.learnmobiledev.domain.repository.KeyValueStore

/** Persistence is tested without Android, because the store is a port. */
class ThemePreferencesTest {

    @Test
    fun `no stored preference means no choice was made`() = runTest {
        // Null rather than a default, so the caller can follow the system dark-mode
        // setting on first launch instead of guessing.
        val preferences = KeyValueThemePreferences(InMemoryKeyValueStore())

        assertEquals(null, preferences.getThemeMode())
    }

    @Test
    fun `a stored theme is read back`() = runTest {
        val preferences = KeyValueThemePreferences(InMemoryKeyValueStore())

        preferences.setThemeMode(ThemeMode.Dark)

        assertEquals(ThemeMode.Dark, preferences.getThemeMode())
    }

    @Test
    fun `an unrecognised stored value is treated as no choice`() = runTest {
        val store = InMemoryKeyValueStore()
        store.putString("theme_mode", "Neonowy")

        val preferences = KeyValueThemePreferences(store)

        assertEquals(null, preferences.getThemeMode())
    }

    @Test
    fun `writing uses a stable key independent of enum ordering`() = runTest {
        var writtenKey: String? = null
        val store = object : KeyValueStore {
            private val delegate = InMemoryKeyValueStore()
            override suspend fun getString(key: String) = delegate.getString(key)
            override suspend fun putString(key: String, value: String) {
                writtenKey = key
                delegate.putString(key, value)
            }
        }

        KeyValueThemePreferences(store).setThemeMode(ThemeMode.Light)

        assertEquals("theme_mode", writtenKey)
    }
}
