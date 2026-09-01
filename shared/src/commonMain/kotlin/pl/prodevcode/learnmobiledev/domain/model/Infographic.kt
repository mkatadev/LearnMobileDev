package pl.prodevcode.learnmobiledev.domain.model

/**
 * A published infographic.
 *
 * Plain Kotlin, like the rest of the domain: no `@Serializable`, no `ImageBitmap`. The
 * picture is [bytes] because decoding is a platform concern the domain has no business
 * knowing about — Android and iOS decode differently, and a domain model that held a
 * decoded bitmap could not cross to iOS at all.
 *
 * [width] and [height] come from the service, so the screen can reserve the right space
 * before the bytes arrive instead of reflowing once they land.
 *
 * The title is not translated. The text is baked into the picture's pixels, so a caption in
 * one language over an image drawn in another would promise something the content cannot
 * deliver; [language] states what the picture itself is written in.
 */
data class Infographic(
    val id: String,
    val title: String,
    val summary: String,
    val language: String,
    val width: Int,
    val height: Int,
    val bytes: ByteArray,
) {
    /** A ByteArray field would otherwise make the generated equals compare by identity. */
    override fun equals(other: Any?): Boolean = this === other ||
        (
            other is Infographic &&
                id == other.id &&
                title == other.title &&
                summary == other.summary &&
                language == other.language &&
                width == other.width &&
                height == other.height &&
                bytes.contentEquals(other.bytes)
            )

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + summary.hashCode()
        result = 31 * result + language.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
