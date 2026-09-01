package pl.prodevcode.learnmobiledev.domain.model

import kotlin.random.Random

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

    /**
     * Returns the same question with its options permuted and [correctIndex] moved along
     * with the answer it points at.
     *
     * The bank is authored by hand, so the correct option tends to drift towards one
     * position; without this, a session could be passed by always picking the same slot.
     * Everything downstream addresses options by index, so permuting here is invisible to
     * the presentation layer.
     */
    fun withShuffledOptions(random: Random): Question {
        val permutation = options.indices.shuffled(random)
        return copy(
            options = permutation.map { options[it] },
            correctIndex = permutation.indexOf(correctIndex),
        )
    }
}
