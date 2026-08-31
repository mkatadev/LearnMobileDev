package pl.prodevcode.learnmobiledev.di

import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pl.prodevcode.learnmobiledev.domain.model.AppLanguage
import pl.prodevcode.learnmobiledev.domain.repository.InMemoryKeyValueStore
import pl.prodevcode.learnmobiledev.domain.repository.KeyValueStore
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider

/**
 * The application composition root — the single place where the layers meet.
 *
 * The platform module is a **parameter** rather than an `expect/actual` declaration.
 * That keeps the platform dependency visible in the signature, and lets tests and
 * previews supply their own implementation without any magic.
 */
fun appModules(platformModule: Module = previewPlatformModule()): List<Module> =
    listOf(platformModule, dataModule, domainModule, presentationModule)

/** In-memory store for Compose previews and tests, where persistence does not matter. */
fun previewPlatformModule(): Module = module {
    single<KeyValueStore> { InMemoryKeyValueStore() }
    single<LanguageProvider>(named(DEVICE_LANGUAGE)) {
        LanguageProvider { AppLanguage.DEFAULT.tag }
    }
}
