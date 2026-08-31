package pl.prodevcode.learnmobiledev.di

import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import pl.prodevcode.learnmobiledev.data.concurrency.CoroutineConcurrencyLab
import pl.prodevcode.learnmobiledev.data.preferences.KeyValueLanguagePreferences
import pl.prodevcode.learnmobiledev.data.remote.ApiLessonsSource
import pl.prodevcode.learnmobiledev.data.remote.ApiQuestionsSource
import pl.prodevcode.learnmobiledev.data.remote.ApiScenariosSource
import pl.prodevcode.learnmobiledev.data.remote.ContentApi
import pl.prodevcode.learnmobiledev.data.preferences.KeyValueThemePreferences
import pl.prodevcode.learnmobiledev.data.repository.InMemoryUserRepository
import pl.prodevcode.learnmobiledev.data.repository.LessonJsonRepository
import pl.prodevcode.learnmobiledev.data.repository.QuestionJsonRepository
import pl.prodevcode.learnmobiledev.data.repository.ScenarioJsonRepository
import pl.prodevcode.learnmobiledev.domain.model.AppLanguage
import pl.prodevcode.learnmobiledev.domain.repository.ConcurrencyLab
import pl.prodevcode.learnmobiledev.domain.repository.LanguagePreferences
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider
import pl.prodevcode.learnmobiledev.domain.repository.PlatformLocale
import pl.prodevcode.learnmobiledev.domain.repository.LessonRepository
import pl.prodevcode.learnmobiledev.domain.repository.NetworkFailureSwitch
import pl.prodevcode.learnmobiledev.domain.repository.QuestionRepository
import pl.prodevcode.learnmobiledev.domain.repository.ScenarioRepository
import pl.prodevcode.learnmobiledev.domain.repository.ThemePreferences
import pl.prodevcode.learnmobiledev.domain.repository.UserRepository
import pl.prodevcode.learnmobiledev.fakeapi.FakeBackend
import pl.prodevcode.learnmobiledev.fakeapi.FakeBackendConfig
import pl.prodevcode.learnmobiledev.fakeapi.LanguageCatalog

/**
 * The data layer: the only place where concrete data sources live.
 *
 * `single { ... } bind ...` registers **one instance behind two interfaces** — the
 * failure switch and the repository are physically the same object, yet the rest of the
 * app only ever sees narrow, separate contracts.
 */
val dataModule: Module = module {
    single { InMemoryUserRepository() } bind UserRepository::class
    single<NetworkFailureSwitch> { get<InMemoryUserRepository>() }
    // The platform locale is the only source of language in the app. Resources already
    // resolved against it, so anything that loaded content from a different one would be
    // showing a language the surrounding UI is not using.
    single<LanguageProvider> { LanguageProvider { get<PlatformLocale>().current() } }
    single { KeyValueLanguagePreferences(get()) } bind LanguagePreferences::class
    // The content service. One client for the whole app: an HttpClient owns a connection
    // pool and a coroutine scope, and creating one per request would leak both.
    //
    // The catalogue of languages is handed to the backend rather than duplicated inside
    // it, so adding a value to AppLanguage cannot leave the service serving English.
    single {
        FakeBackend.createClient(
            languages = LanguageCatalog(
                supported = AppLanguage.SUPPORTED_TAGS,
                default = AppLanguage.DEFAULT.tag,
            ),
            // Every exchange with the content service shows up in Logcat and Console.app,
            // which is the whole point of a fake that speaks real HTTP.
            config = FakeBackendConfig(logTraffic = true),
        )
    }
    single { ContentApi(get(), get()) }
    single {
        LessonJsonRepository(ApiLessonsSource(get()), get())
    } bind LessonRepository::class
    single { CoroutineConcurrencyLab() } bind ConcurrencyLab::class
    single {
        ScenarioJsonRepository(
            workers = CoroutineConcurrencyLab.DEFAULT_WORKERS,
            source = ApiScenariosSource(get()),
            languageProvider = get(),
        )
    } bind ScenarioRepository::class
    single {
        QuestionJsonRepository(ApiQuestionsSource(get()), get())
    } bind QuestionRepository::class
    single { KeyValueThemePreferences(get()) } bind ThemePreferences::class
}
