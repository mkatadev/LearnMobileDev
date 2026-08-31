package pl.prodevcode.learnmobiledev.domain.repository

import pl.prodevcode.learnmobiledev.domain.model.Question

/** Port for accessing the question bank. */
interface QuestionRepository {
    suspend fun getQuestions(): List<Question>
}

/** Signals that the question bank could not be loaded. Message is technical, for logs only. */
class QuestionsUnavailableException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
