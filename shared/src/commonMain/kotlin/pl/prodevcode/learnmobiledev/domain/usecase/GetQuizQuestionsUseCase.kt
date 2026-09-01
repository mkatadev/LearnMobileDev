package pl.prodevcode.learnmobiledev.domain.usecase

import kotlin.random.Random
import pl.prodevcode.learnmobiledev.domain.model.Question
import pl.prodevcode.learnmobiledev.domain.model.QuizCategory
import pl.prodevcode.learnmobiledev.domain.repository.QuestionRepository

/**
 * Builds the set of questions for a quiz session.
 *
 * The rules live here rather than in the store:
 * - an empty category filter means "all categories",
 * - the order is randomised so that revision cannot rely on memorised positions,
 * - the answer options of every question are randomised too, so the correct one does not
 *   sit at the position the author happened to favour,
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
        val random = if (seed == null) Random.Default else Random(seed)

        return filtered.shuffled(random)
            .take(limit)
            .map { it.withShuffledOptions(random) }
    }

    companion object {
        const val DEFAULT_LIMIT = 15
    }
}
