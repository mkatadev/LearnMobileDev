package pl.prodevcode.learnmobiledev.fakeapi

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The catalogue document: which infographics the service publishes.
 *
 * A `fun interface` like [UserStorage], so routing can be tested on a fixture rather than
 * on the shipped file.
 */
fun interface InfographicIndexStorage {
    suspend fun read(): String?
}

/** Raw access to an infographic's bytes, by the path the catalogue gives. */
fun interface ImageStorage {
    suspend fun read(path: String): ByteArray?
}

/** Reads the catalogue bundled with this module. */
class BundledInfographicIndexStorage : InfographicIndexStorage {

    private val content = BundledContentStorage()

    override suspend fun read(): String? = content.readFile("files/infographics.json")
}

/** Reads the images bundled with this module. */
class BundledImageStorage : ImageStorage {

    private val content = BundledContentStorage()

    override suspend fun read(path: String): ByteArray? = content.readImageBytes(path)
}

/**
 * An infographic as the service publishes it.
 *
 * The metadata travels as JSON and the picture as bytes, over two calls. Inlining a
 * megabyte of base64 into the listing would make the app download every image to show a
 * single title, and base64 costs a third more bytes on the wire for the privilege.
 *
 * [width] and [height] are published so the app can reserve the right space before the
 * bytes arrive, instead of reflowing the screen once they land.
 *
 * The title is *not* localized. The picture has text baked into its pixels, so a
 * translated caption over an untranslated image would promise a language the content does
 * not deliver; the image's own language is stated in [language] instead.
 */
@Serializable
data class InfographicRecord(
    val id: String,
    val title: String,
    val summary: String,
    val language: String,
    val width: Int,
    val height: Int,
    val path: String,
)

@Serializable
private data class InfographicTable(val version: Int = 1, val infographics: List<InfographicRecord>)

/**
 * The service's infographic catalogue: what it publishes, and where each picture lives.
 *
 * The index is a document like any other, so adding an infographic is a JSON entry plus a
 * file — no code change. `BundledContentTest` checks that every entry actually resolves to
 * an image, because a listing that promises a picture the storage does not have is a `404`
 * the user meets instead of the content.
 */
class InfographicCatalog(
    private val storage: InfographicIndexStorage = BundledInfographicIndexStorage(),
    private val images: ImageStorage = BundledImageStorage(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private var cached: List<InfographicRecord>? = null

    suspend fun all(): List<InfographicRecord> = cached ?: load().also { cached = it }

    suspend fun find(id: String): InfographicRecord? = all().firstOrNull { it.id == id }

    /** The picture itself, or `null` when the catalogue points at something that is gone. */
    suspend fun imageBytes(record: InfographicRecord): ByteArray? = images.read(record.path)

    private suspend fun load(): List<InfographicRecord> {
        // The catalogue sits beside the user table rather than inside a language folder:
        // it describes pictures, and which language a picture is drawn in is a property of
        // that entry, not of the request.
        val document = storage.read() ?: return emptyList()
        return json.decodeFromString<InfographicTable>(document).infographics
    }
}
