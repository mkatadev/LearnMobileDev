package pl.prodevcode.learnmobiledev.domain.repository

import pl.prodevcode.learnmobiledev.domain.model.Lesson

/**
 * Port for accessing course content.
 *
 * The learn screen has no idea whether lessons come from a JSON asset, a database
 * or a CMS. Knowing that they can be fetched is enough to build the screen and its tests.
 */
interface LessonRepository {
    suspend fun getLessons(): List<Lesson>
}

/** Signals that course content could not be loaded. Message is technical, for logs only. */
class LessonsUnavailableException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
