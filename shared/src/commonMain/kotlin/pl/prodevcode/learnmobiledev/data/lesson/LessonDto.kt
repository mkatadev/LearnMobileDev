package pl.prodevcode.learnmobiledev.data.lesson

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.prodevcode.learnmobiledev.domain.model.Block
import pl.prodevcode.learnmobiledev.domain.model.Lesson

/**
 * DTO — the **wire representation** of course content (JSON).
 *
 * Why separate classes instead of annotating the domain model?
 * - the domain stays free of a dependency on kotlinx.serialization,
 * - JSON field names can change independently of the names in code (`@SerialName`),
 * - the mapper is the only place to fix when the format changes.
 *
 * `@SerialName("type")` combined with `sealed` yields polymorphic JSON: every block
 * carries its own type.
 */
@Serializable
internal data class LessonsFileDto(
    val version: Int = 1,
    val lessons: List<LessonDto>,
)

@Serializable
internal data class LessonDto(
    val id: String,
    val title: String,
    val summary: String,
    val blocks: List<BlockDto>,
)

@Serializable
internal sealed interface BlockDto {

    @Serializable
    @SerialName("paragraph")
    data class Paragraph(val text: String) : BlockDto

    @Serializable
    @SerialName("subheading")
    data class Subheading(val text: String) : BlockDto

    @Serializable
    @SerialName("bullets")
    data class Bullets(val items: List<String>) : BlockDto

    @Serializable
    @SerialName("code")
    data class Code(val code: String, val caption: String? = null) : BlockDto

    @Serializable
    @SerialName("rule")
    data class Rule(val text: String) : BlockDto

    @Serializable
    @SerialName("table")
    data class Table(val headers: List<String>, val rows: List<List<String>>) : BlockDto

    @Serializable
    @SerialName("exercise")
    data class Exercise(val number: Int, val text: String) : BlockDto
}

/** Layer boundary: DTO to domain model. Outside this file the DTO does not exist. */
internal fun LessonDto.toDomain(): Lesson = Lesson(
    id = id,
    title = title,
    summary = summary,
    blocks = blocks.map { it.toDomain() },
)

internal fun BlockDto.toDomain(): Block = when (this) {
    is BlockDto.Paragraph -> Block.Paragraph(text)
    is BlockDto.Subheading -> Block.Subheading(text)
    is BlockDto.Bullets -> Block.Bullets(items)
    is BlockDto.Code -> Block.Code(code = code, caption = caption)
    is BlockDto.Rule -> Block.Rule(text)
    is BlockDto.Table -> Block.Table(headers = headers, rows = rows)
    is BlockDto.Exercise -> Block.Exercise(number = number, text = text)
}
