package pl.prodevcode.learnmobiledev.data.infographic

import kotlinx.serialization.Serializable
import pl.prodevcode.learnmobiledev.domain.model.Infographic

/**
 * DTO — the wire representation of an infographic's metadata.
 *
 * The service also publishes a `path`, which is deliberately absent here: it is where the
 * service keeps its file, and the app addresses an image by id. Carrying it upward would
 * let a screen build a request out of somebody else's storage layout.
 */
@Serializable
internal data class InfographicsResponseDto(val infographics: List<InfographicDto>)

@Serializable
internal data class InfographicDto(
    val id: String,
    val title: String,
    val summary: String,
    val language: String,
    val width: Int,
    val height: Int,
)

internal fun InfographicDto.toDomain(bytes: ByteArray): Infographic = Infographic(
    id = id,
    title = title,
    summary = summary,
    language = language,
    width = width,
    height = height,
    bytes = bytes,
)
