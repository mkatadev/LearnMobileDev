package pl.prodevcode.learnmobiledev.data.quiz

/**
 * Raw source of the question bank. Extracted behind an interface so that *where* the bytes come
 * from stays replaceable: it used to be a bundled asset, it is now an HTTP call to the
 * content service, and tests substitute a literal string without touching a network or an
 * emulator.
 *
 * @see pl.prodevcode.learnmobiledev.data.remote.ApiQuestionsSource
 */
fun interface QuestionsSource {
    suspend fun readContent(): String
}
