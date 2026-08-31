package pl.prodevcode.learnmobiledev.domain.model

/**
 * Course content model — **plain Kotlin, free of framework annotations**.
 *
 * Note the absence of `@Serializable`: the wire format belongs to the data layer
 * (see `data/lesson/LessonDto.kt`). Swapping JSON for a database or an API would not
 * change a single line here.
 */
sealed interface Block {

    /** Paragraph. Fragments wrapped in `**asterisks**` are rendered in bold. */
    data class Paragraph(val text: String) : Block

    /** Section heading inside a lesson. */
    data class Subheading(val text: String) : Block

    /** Bulleted list. */
    data class Bullets(val items: List<String>) : Block

    /** Code snippet with an optional caption. */
    data class Code(val code: String, val caption: String? = null) : Block

    /** Highlighted rule — the takeaway worth remembering. */
    data class Rule(val text: String) : Block

    /** Comparison table. */
    data class Table(val headers: List<String>, val rows: List<List<String>>) : Block

    /** Hands-on exercise. */
    data class Exercise(val number: Int, val text: String) : Block
}

/** A single course lesson. */
data class Lesson(
    val id: String,
    val title: String,
    val summary: String,
    val blocks: List<Block>,
)
