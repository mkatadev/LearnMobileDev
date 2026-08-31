package pl.prodevcode.learnmobiledev.data.lesson

/**
 * Raw source of course content. Extracted behind an interface so that *where* the bytes come
 * from stays replaceable: it used to be a bundled asset, it is now an HTTP call to the
 * content service, and tests substitute a literal string without touching a network or an
 * emulator.
 *
 * @see pl.prodevcode.learnmobiledev.data.remote.ApiLessonsSource
 */
fun interface LessonsSource {
    suspend fun readContent(): String
}
