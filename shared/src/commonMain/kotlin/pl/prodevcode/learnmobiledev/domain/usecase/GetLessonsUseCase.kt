package pl.prodevcode.learnmobiledev.domain.usecase

import pl.prodevcode.learnmobiledev.domain.model.Lesson
import pl.prodevcode.learnmobiledev.domain.repository.LessonRepository

/** Loads the course material. One business rule, one public method. */
class GetLessonsUseCase(
    private val repository: LessonRepository,
) {
    suspend operator fun invoke(): List<Lesson> = repository.getLessons()
}
