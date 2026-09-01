package pl.prodevcode.learnmobiledev.data

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import pl.prodevcode.learnmobiledev.data.remote.InfographicsApi
import pl.prodevcode.learnmobiledev.data.remote.createContentHttpClient
import pl.prodevcode.learnmobiledev.data.repository.ApiInfographicRepository
import pl.prodevcode.learnmobiledev.domain.model.AppLanguage
import pl.prodevcode.learnmobiledev.domain.repository.InfographicsUnavailableException
import pl.prodevcode.learnmobiledev.fakeapi.FakeBackend
import pl.prodevcode.learnmobiledev.fakeapi.FakeBackendConfig
import pl.prodevcode.learnmobiledev.fakeapi.ImageStorage
import pl.prodevcode.learnmobiledev.fakeapi.InfographicCatalog
import pl.prodevcode.learnmobiledev.fakeapi.InfographicIndexStorage
import pl.prodevcode.learnmobiledev.fakeapi.LanguageCatalog

/**
 * The infographics path end to end: repository → Ktor client → fake backend → storage.
 *
 * What it proves is that the picture survives the round trip *as bytes*. A PNG that went
 * through a `String` anywhere on the way would arrive corrupted and fail to decode on a
 * device, which no reducer test could see.
 */
class ApiInfographicRepositoryTest {

    private val index = """
        {
          "infographics": [
            {
              "id": "mvi-in-kmp",
              "title": "MVI w KMP",
              "summary": "Jednokierunkowy przeplyw danych",
              "language": "pl",
              "width": 1024,
              "height": 1536,
              "path": "files/images/mvi-in-kmp.webp"
            }
          ]
        }
    """.trimIndent()

    /** A byte sequence that is not valid UTF-8, which is exactly the point. */
    private val imageBytes = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        0xFF.toByte(), 0xD8.toByte(), 0x00, 0x01,
    )

    private fun repository(
        indexDocument: String? = index,
        images: ImageStorage = ImageStorage { imageBytes },
    ): ApiInfographicRepository {
        val client = createContentHttpClient(
            engine = FakeBackend.createEngine(
                languages = LanguageCatalog(AppLanguage.SUPPORTED_TAGS, AppLanguage.DEFAULT.tag),
                config = FakeBackendConfig(latencyMillis = 0),
                infographics = InfographicCatalog(
                    storage = InfographicIndexStorage { indexDocument },
                    images = images,
                ),
            ),
            baseUrl = FakeBackend.BASE_URL,
        )
        return ApiInfographicRepository(InfographicsApi(client))
    }

    @Test
    fun `the catalogue and its picture travel over http`() = runTest {
        val infographics = repository().getInfographics()

        assertEquals(1, infographics.size)
        val infographic = infographics.single()
        assertEquals("mvi-in-kmp", infographic.id)
        assertEquals(1024, infographic.width)
        assertEquals(1536, infographic.height)
    }

    /** The bytes must arrive unchanged; a string round trip would mangle the image header. */
    @Test
    fun `the image bytes survive the round trip intact`() = runTest {
        val infographic = repository().getInfographics().single()

        assertContentEquals(imageBytes, infographic.bytes)
    }

    @Test
    fun `a catalogue entry with no stored image is a domain failure`() = runTest {
        val repository = repository(images = ImageStorage { null })

        assertFailsWith<InfographicsUnavailableException> { repository.getInfographics() }
    }

    /** Fetched once: each picture is about a megabyte, so a second trip is wasted bandwidth. */
    @Test
    fun `the result is cached`() = runTest {
        val repository = repository()

        val first = repository.getInfographics()
        val second = repository.getInfographics()

        assertEquals(first, second)
    }
}
