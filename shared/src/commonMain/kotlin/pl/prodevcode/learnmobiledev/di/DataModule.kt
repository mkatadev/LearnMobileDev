package pl.prodevcode.learnmobiledev.di

import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import pl.prodevcode.learnmobiledev.data.EffectiveLanguage
import pl.prodevcode.learnmobiledev.data.concurrency.CoroutineConcurrencyLab
import pl.prodevcode.learnmobiledev.data.preferences.KeyValueLanguagePreferences
import pl.prodevcode.learnmobiledev.data.lesson.ComposeResourceLessonsSource
import pl.prodevcode.learnmobiledev.data.quiz.ComposeResourceQuestionsSource
import pl.prodevcode.learnmobiledev.data.scenario.ComposeResourceScenariosSource
import pl.prodevcode.learnmobiledev.data.preferences.KeyValueThemePreferences
import pl.prodevcode.learnmobiledev.data.repository.InMemoryUserRepository
import pl.prodevcode.learnmobiledev.data.repository.LessonJsonRepository
import pl.prodevcode.learnmobiledev.data.repository.QuestionJsonRepository
import pl.prodevcode.learnmobiledev.data.repository.ScenarioJsonRepository
import pl.prodevcode.learnmobiledev.data.repository.StringCatalogJsonRepository
import pl.prodevcode.learnmobiledev.data.strings.ComposeResourceStringsSource
import pl.prodevcode.learnmobiledev.domain.repository.ConcurrencyLab
import pl.prodevcode.learnmobiledev.domain.repository.LanguagePreferences
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider
import pl.prodevcode.learnmobiledev.domain.repository.LessonRepository
import pl.prodevcode.learnmobiledev.domain.repository.NetworkFailureSwitch
import pl.prodevcode.learnmobiledev.domain.repository.QuestionRepository
import pl.prodevcode.learnmobiledev.domain.repository.ScenarioRepository
import pl.prodevcode.learnmobiledev.domain.repository.StringCatalogRepository
import pl.prodevcode.learnmobiledev.domain.repository.ThemePreferences
import pl.prodevcode.learnmobiledev.domain.repository.UserRepository

/**
 * The data layer: the only place where concrete data sources live.
 *
 * `single { ... } bind ...` registers **one instance behind two interfaces** — the
 * failure switch and the repository are physically the same object, yet the rest of the
 * app only ever sees narrow, separate contracts.
 */
/** Qualifier for the raw platform language, before the user's preference is applied. */
const val DEVICE_LANGUAGE = "deviceLanguage"

val dataModule: Module = module {
    single { InMemoryUserRepository() } bind UserRepository::class
    single<NetworkFailureSwitch> { get<InMemoryUserRepository>() }
    // EffectiveLanguage decorates the platform provider with the user's choice; it is a
    // singleton because the choice must be visible to every data source at once.
    single {
        EffectiveLanguage(deviceLanguage = get(named(DEVICE_LANGUAGE)))
    } bind LanguageProvider::class
    single { KeyValueLanguagePreferences(get()) } bind LanguagePreferences::class
    single {
        StringCatalogJsonRepository(ComposeResourceStringsSource(get()), get())
    } bind StringCatalogRepository::class
    single {
        LessonJsonRepository(ComposeResourceLessonsSource(get()), get())
    } bind LessonRepository::class
    single { CoroutineConcurrencyLab() } bind ConcurrencyLab::class
    single {
        ScenarioJsonRepository(
            workers = CoroutineConcurrencyLab.DEFAULT_WORKERS,
            source = ComposeResourceScenariosSource(get()),
            languageProvider = get(),
        )
    } bind ScenarioRepository::class
    single {
        QuestionJsonRepository(ComposeResourceQuestionsSource(get()), get())
    } bind QuestionRepository::class
    single { KeyValueThemePreferences(get()) } bind ThemePreferences::class
}
