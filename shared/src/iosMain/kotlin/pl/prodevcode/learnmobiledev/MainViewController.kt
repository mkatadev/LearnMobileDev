package pl.prodevcode.learnmobiledev

import androidx.compose.ui.window.ComposeUIViewController
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pl.prodevcode.learnmobiledev.di.DEVICE_LANGUAGE
import pl.prodevcode.learnmobiledev.data.preferences.IosLanguageProvider
import pl.prodevcode.learnmobiledev.data.preferences.NsUserDefaultsKeyValueStore
import pl.prodevcode.learnmobiledev.domain.repository.KeyValueStore
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider

/**
 * Entry point for iOS. The platform module provides persistent storage based on
 * NSUserDefaults — the rest of the dependency graph is shared with Android.
 */
fun MainViewController() = ComposeUIViewController {
    App(
        platformModule = module {
            single<KeyValueStore> { NsUserDefaultsKeyValueStore() }
            single<LanguageProvider>(named(DEVICE_LANGUAGE)) { IosLanguageProvider() }
        },
    )
}
