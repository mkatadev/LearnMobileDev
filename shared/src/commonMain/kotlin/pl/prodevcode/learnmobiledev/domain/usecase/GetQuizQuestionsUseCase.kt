package pl.prodevcode.learnmobiledev.domain.usecase

import pl.prodevcode.learnmobiledev.domain.model.Question
import pl.prodevcode.learnmobiledev.domain.model.QuizCategory
import pl.prodevcode.learnmobiledev.domain.repository.QuestionRepository

/**
 * Builds the set of questions for a quiz session.
 *
 * The rules live here rather than in the store:
 * - an empty category filter means "all categories",
 * - the order is randomised so that revision cannot rely on memorised positions,
 * - [limit] caps the length of a session.
 */
class GetQuizQuestionsUseCase(
    private val repository: QuestionRepository,
) {
    suspend operator fun invoke(
        categories: Set<QuizCategory> = emptySet(),
        limit: Int = DEFAULT_LIMIT,
        seed: Int? = null,
    ): List<Question> {
        val filtered = repository.getQuestions()
            .filter { categories.isEmpty() || it.category in categories }

        // Deterministic shuffle when a seed is supplied, so that tests stay repeatable.
        val ordered = if (seed == null) {
            filtered.shuffled()
        } else {
            filtered.shuffled(kotlin.random.Random(seed))
        }

        return ordered.take(limit)
    }

    companion object {
        const val DEFAULT_LIMIT = 15
    }
}
