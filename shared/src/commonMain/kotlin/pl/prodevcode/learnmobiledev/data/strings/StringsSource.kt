package pl.prodevcode.learnmobiledev.data.strings

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import learnmobiledev.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import pl.prodevcode.learnmobiledev.data.LocalizedAsset
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider

/** Raw source of the string catalogue. Extracted because `Res.readBytes` needs a Context. */
fun interface StringsSource {
    suspend fun readContent(): String
}

/** Production source reading `composeResources/files/<lang>/strings.json`. */
@OptIn(ExperimentalResourceApi::class)
class ComposeResourceStringsSource(
    private val languageProvider: LanguageProvider,
) : StringsSource {
    override suspend fun readContent(): String =
        Res.readBytes(LocalizedAsset.path("strings", languageProvider)).decodeToString()
}

/** Wire format of the catalogue. */
@Serializable
internal data class StringsFileDto(
    val version: Int = 1,
    val strings: Map<String, String>,
)

/** Pure parser, testable without Android. */
class StringsJsonParser(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun parse(content: String): Map<String, String> =
        json.decodeFromString<StringsFileDto>(content).strings
}
