package pl.prodevcode.learnmobiledev.data.scenario

import learnmobiledev.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import pl.prodevcode.learnmobiledev.data.LocalizedAsset
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider

/**
 * Raw source of scenario descriptions. Extracted behind an interface because `Res.readBytes` requires a
 * Context on Android, and unit tests must run without an emulator.
 */
fun interface ScenariosSource {
    suspend fun readContent(): String
}

/**
 * Production source reading `composeResources/files/<lang>/scenarios.json`, falling back to
 * the default language when the device language has no translation.
 */
@OptIn(ExperimentalResourceApi::class)
class ComposeResourceScenariosSource(
    private val languageProvider: LanguageProvider,
) : ScenariosSource {
    override suspend fun readContent(): String =
        Res.readBytes(LocalizedAsset.path("scenarios", languageProvider)).decodeToString()
}
