package pl.prodevcode.learnmobiledev.domain.model

/**
 * Content language.
 *
 * There is deliberately no "system" variant: the device language only decides the
 * **initial** value, and from then on the choice is explicit. A third option would add a
 * state that looks different from the other two but renders identically to one of them.
 */
enum class AppLanguage(val tag: String) {
    English("en"),
    Polish("pl"),
    ;

    companion object {
        /** Used when the device language has no translation. */
        val DEFAULT = English

        val SUPPORTED_TAGS: Set<String> = entries.map { it.tag }.toSet()

        fun fromTag(tag: String?): AppLanguage? =
            entries.firstOrNull { it.tag == tag?.lowercase() }
    }
}
