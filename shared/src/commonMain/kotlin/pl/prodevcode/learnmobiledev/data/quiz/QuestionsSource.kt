package pl.prodevcode.learnmobiledev.data.quiz

import learnmobiledev.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import pl.prodevcode.learnmobiledev.data.LocalizedAsset
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider

/**
 * Raw source of the question bank. Extracted behind an interface because `Res.readBytes` requires a
 * Context on Android, and unit tests must run without an emulator.
 */
fun interface QuestionsSource {
    suspend fun readContent(): String
}

/**
 * Production source reading `composeResources/files/<lang>/questions.json`, falling back to
 * the default language when the device language has no translation.
 */
@OptIn(ExperimentalResourceApi::class)
class ComposeResourceQuestionsSource(
    private val languageProvider: LanguageProvider,
) : QuestionsSource {
    override suspend fun readContent(): String =
        Res.readBytes(LocalizedAsset.path("questions", languageProvider)).decodeToString()
}
