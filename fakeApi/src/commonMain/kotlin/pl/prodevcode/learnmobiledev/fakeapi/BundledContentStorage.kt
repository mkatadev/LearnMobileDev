package pl.prodevcode.learnmobiledev.fakeapi

import org.jetbrains.compose.resources.ExperimentalResourceApi
import pl.prodevcode.learnmobiledev.fakeapi.resources.Res

/**
 * The service's own storage: the JSON documents shipped inside this module.
 *
 * These files are the backend's database, which is why they live here rather than in the
 * app. Nothing above the HTTP boundary can reach them — the app has no path to
 * `fakeApi`'s resources and must go through the API like it would against a real server.
 *
 * A missing document returns `null` so the routes can answer `404`. Compose Resources
 * signals absence by throwing, and the exception type differs per platform, hence the
 * broad catch: any failure to produce bytes is, to the storage, an absent document.
 */
@OptIn(ExperimentalResourceApi::class)
class BundledContentStorage : ContentStorage {

    override suspend fun read(language: String, resource: String): String? =
        readFile("files/$language/$resource.json")

    internal suspend fun readFile(path: String): String? = try {
        Res.readBytes(path).decodeToString()
    } catch (_: Exception) {
        null
    }
}
