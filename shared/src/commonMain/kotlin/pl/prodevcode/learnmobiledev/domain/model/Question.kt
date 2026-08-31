package pl.prodevcode.learnmobiledev.domain.model

/**
 * Knowledge area of a quiz question.
 *
 * Deliberately carries **no display label**: a domain enum must not decide how it is
 * spelled in the user interface, otherwise the wording could never be translated.
 * The presentation layer maps each entry to a string resource.
 */
enum class QuizCategory {
    Mvi,
    Kmp,
    Coroutines,
    Rx,
    CleanArchitecture,
    Solid,
    Compose,
    Testing,
    Kotlin,
    Android,
    Performance,
    DataStructures,
}

/** Difficulty level. The course targets [Senior]; [Mid] serves as a warm-up. */
enum class Difficulty {
    Mid,
    Senior,
}

/**
 * Single-choice question.
 *
 * [explanation] is **mandatory**: it is shown after every answer, because the quiz is
 * meant to teach rather than merely grade. A bare "wrong" carries no knowledge.
 */
data class Question(
    val id: String,
    val category: QuizCategory,
    val difficulty: Difficulty,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    /** Optional code snippet rendered above the question. */
    val code: String? = null,
) {
    val correctAnswer: String
        get() = options[correctIndex]
}
