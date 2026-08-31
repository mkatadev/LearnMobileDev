package pl.prodevcode.learnmobiledev.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import java.io.File
import pl.prodevcode.learnmobiledev.data.remote.ApiLessonsSource
import pl.prodevcode.learnmobiledev.data.remote.ApiQuestionsSource
import pl.prodevcode.learnmobiledev.data.remote.ApiScenariosSource
import pl.prodevcode.learnmobiledev.data.remote.ContentApi
import pl.prodevcode.learnmobiledev.data.repository.LessonJsonRepository
import pl.prodevcode.learnmobiledev.domain.model.AppLanguage
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider
import pl.prodevcode.learnmobiledev.fakeapi.ContentStorage
import pl.prodevcode.learnmobiledev.fakeapi.FakeBackend
import pl.prodevcode.learnmobiledev.fakeapi.FakeBackendConfig
import pl.prodevcode.learnmobiledev.fakeapi.LanguageCatalog

/**
 * The one test that exercises the whole content path end to end: repository → source →
 * Ktor client → fake backend → bundled storage.
 *
 * The unit tests above stub the source, which is right for testing parsing and caching but
 * would happily keep passing if the wiring to the backend were broken. This one would not.
 *
 * The backend is given a filesystem-backed storage rather than the production
 * [pl.prodevcode.learnmobiledev.data.backend.BundledContentStorage], because Compose
 * Resources needs an Android runtime that a JVM test does not have. It reads the very same
 * files that ship with the app, so what is under test — routing, language negotiation,
 * status handling, parsing — is unaffected; only the disk access differs.
 */
class ContentApiIntegrationTest {

    /** The same documents the app bundles, read straight from the source tree. */
    private val storage = ContentStorage { language, resource ->
        File("src/commonMain/composeResources/files/$language/$resource.json")
            .takeIf { it.exists() }
            ?.readText()
    }

    private fun apiFor(language: String): ContentApi {
        val client = FakeBackend.createClient(
            storage = storage,
            languages = LanguageCatalog(AppLanguage.SUPPORTED_TAGS, AppLanguage.DEFAULT.tag),
            // No artificial latency: this test asserts wiring, not loading states.
            config = FakeBackendConfig(latencyMillis = 0),
        )
        return ContentApi(client, LanguageProvider { language })
    }

    @Test
    fun `lessons are served over http and parsed into the domain model`() = runTest {
        val repository = LessonJsonRepository(
            source = ApiLessonsSource(apiFor("en")),
            languageProvider = LanguageProvider { "en" },
        )

        val lessons = repository.getLessons()

        assertTrue(lessons.isNotEmpty(), "no lessons came back from the content service")
        assertTrue(lessons.all { it.title.isNotBlank() })
    }

    @Test
    fun `every shipped resource is reachable in every shipped language`() = runTest {
        AppLanguage.entries.forEach { language ->
            val api = apiFor(language.tag)
            val documents = mapOf(
                "lessons" to ApiLessonsSource(api)::readContent,
                "questions" to ApiQuestionsSource(api)::readContent,
                "scenarios" to ApiScenariosSource(api)::readContent,
            )
            documents.forEach { (name, read) ->
                assertTrue(
                    read().isNotBlank(),
                    "empty '$name' document for ${language.tag}",
                )
            }
        }
    }

    /**
     * A locale the backend has no translation for must still render the app, in the
     * default language. Serving a 404 here would turn an untranslated device into a broken
     * one.
     */
    @Test
    fun `an untranslated device language falls back to the default content`() = runTest {
        val fallback = ApiLessonsSource(apiFor("de")).readContent()
        val default = ApiLessonsSource(apiFor(AppLanguage.DEFAULT.tag)).readContent()

        assertEquals(default, fallback)
    }
}
