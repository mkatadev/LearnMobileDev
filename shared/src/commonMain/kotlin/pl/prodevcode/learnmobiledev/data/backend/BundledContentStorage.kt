package pl.prodevcode.learnmobiledev.data.backend

import learnmobiledev.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import pl.prodevcode.learnmobiledev.fakeapi.ContentStorage

/**
 * The fake backend's storage, backed by the JSON documents bundled with the app.
 *
 * This is the one place where the illusion is wired up, and it is deliberately on the
 * *server* side of the boundary: the JSON is the backend's database, not the app's assets.
 * Nothing in the app reads these files any more — it goes through HTTP like it would
 * against a real service.
 *
 * A missing document returns `null` so the routes can answer `404`. Compose Resources
 * signals it by throwing, and the exception type differs per platform, hence the broad
 * catch: any failure to produce bytes is, from the storage's point of view, an absent
 * document.
 */
@OptIn(ExperimentalResourceApi::class)
class BundledContentStorage : ContentStorage {

    override suspend fun read(language: String, resource: String): String? = try {
        Res.readBytes("files/$language/$resource.json").decodeToString()
    } catch (_: Exception) {
        null
    }
}
