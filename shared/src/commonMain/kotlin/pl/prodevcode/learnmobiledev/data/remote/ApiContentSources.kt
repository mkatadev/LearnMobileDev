package pl.prodevcode.learnmobiledev.data.remote

import pl.prodevcode.learnmobiledev.data.lesson.LessonsSource
import pl.prodevcode.learnmobiledev.data.quiz.QuestionsSource
import pl.prodevcode.learnmobiledev.data.scenario.ScenariosSource

/**
 * Network-backed implementations of the content sources.
 *
 * The `*Source` interfaces were always defined as "where the bytes come from", so moving
 * from a bundled asset to an HTTP call is a change of implementation only: the parsers and
 * repositories above them are untouched, and their tests keep passing unchanged. That is
 * the payoff of having split *where* from *how*.
 */
class ApiLessonsSource(private val api: ContentApi) : LessonsSource {
    override suspend fun readContent(): String = api.fetchDocument("lessons")
}

class ApiQuestionsSource(private val api: ContentApi) : QuestionsSource {
    override suspend fun readContent(): String = api.fetchDocument("questions")
}

class ApiScenariosSource(private val api: ContentApi) : ScenariosSource {
    override suspend fun readContent(): String = api.fetchDocument("scenarios")
}
