package pl.prodevcode.learnmobiledev.data.quiz

import kotlinx.serialization.Serializable
import pl.prodevcode.learnmobiledev.domain.model.Difficulty
import pl.prodevcode.learnmobiledev.domain.model.Question
import pl.prodevcode.learnmobiledev.domain.model.QuizCategory

/**
 * Wire format of the question bank. As with lessons, serialization annotations stay in
 * the data layer and the domain remains plain Kotlin.
 */
@Serializable
internal data class QuestionsFileDto(
    val version: Int = 1,
    val questions: List<QuestionDto>,
)

@Serializable
internal data class QuestionDto(
    val id: String,
    val category: String,
    val difficulty: String = "senior",
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val code: String? = null,
)

/**
 * Maps the DTO to the domain model **with validation**.
 *
 * An unknown category or an out-of-range `correctIndex` is a data defect, not an
 * application state: failing loudly while parsing beats showing the user a question
 * that has no correct answer.
 */
internal fun QuestionDto.toDomain(): Question {
    require(options.size >= 2) { "question $id has fewer than 2 options" }
    require(correctIndex in options.indices) {
        "question $id has correctIndex=$correctIndex outside ${options.indices}"
    }

    return Question(
        id = id,
        category = category.toCategory(id),
        difficulty = difficulty.toDifficulty(),
        text = text,
        options = options,
        correctIndex = correctIndex,
        explanation = explanation,
        code = code,
    )
}

private fun String.toCategory(questionId: String): QuizCategory = when (lowercase()) {
    "mvi" -> QuizCategory.Mvi
    "kmp" -> QuizCategory.Kmp
    "coroutines" -> QuizCategory.Coroutines
    "rx" -> QuizCategory.Rx
    "clean" -> QuizCategory.CleanArchitecture
    "solid" -> QuizCategory.Solid
    "compose" -> QuizCategory.Compose
    "testing" -> QuizCategory.Testing
    "kotlin" -> QuizCategory.Kotlin
    "android" -> QuizCategory.Android
    "performance" -> QuizCategory.Performance
    "datastructures" -> QuizCategory.DataStructures
    else -> throw IllegalArgumentException("unknown category '$this' in question $questionId")
}

private fun String.toDifficulty(): Difficulty =
    if (lowercase() == "mid") Difficulty.Mid else Difficulty.Senior
