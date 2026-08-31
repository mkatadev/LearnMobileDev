package pl.prodevcode.learnmobiledev.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The resolved string catalogue for the active language.
 *
 * A [staticCompositionLocalOf] rather than a dynamic one: the catalogue changes rarely
 * (only on a language switch) and reading it must not subscribe every text to
 * recomposition. When it does change, the whole subtree recomposes, which is exactly what
 * a language switch should do.
 */
val LocalStrings = staticCompositionLocalOf { StringCatalog.EMPTY }

/**
 * Key-to-translation lookup with argument formatting.
 *
 * A missing key falls back to the key itself instead of throwing: a defect in the
 * catalogue should be visible on screen during development, never a crash in the user's
 * hands. `StringCatalogTest` makes sure it does not reach production in the first place.
 */
class StringCatalog(private val values: Map<String, String>) {

    operator fun get(id: AppString, args: List<Any> = emptyList()): String {
        val template = values[id.key] ?: return id.key
        return if (args.isEmpty()) template else template.format(args)
    }

    /**
     * Substitutes positional placeholders in the Android format style: `%1$s`, `%2$d`.
     *
     * Written by hand because Kotlin common code has no `String.format`. Only the subset
     * the catalogue actually uses is supported, and `%%` escapes a literal percent sign.
     */
    private fun String.format(args: List<Any>): String {
        val out = StringBuilder(length)
        var index = 0

        while (index < length) {
            val char = this[index]
            if (char != '%') {
                out.append(char)
                index++
                continue
            }

            if (index + 1 < length && this[index + 1] == '%') {
                out.append('%')
                index += 2
                continue
            }

            val digitsEnd = (index + 1 until length).firstOrNull { !this[it].isDigit() }
            val position = substring(index + 1, digitsEnd ?: length).toIntOrNull()
            val hasSpecifier = digitsEnd != null && this[digitsEnd] == '$' && digitsEnd + 1 < length

            if (position == null || !hasSpecifier) {
                out.append(char)
                index++
                continue
            }

            out.append(args.getOrNull(position - 1)?.toString().orEmpty())
            index = digitsEnd + 2
        }

        return out.toString()
    }

    companion object {
        val EMPTY = StringCatalog(emptyMap())
    }
}

/** Resolves a string in the active language. The counterpart of `stringResource`. */
@Composable
fun localized(id: AppString): String = LocalStrings.current[id]

/** Resolves a string with positional arguments. */
@Composable
fun localized(id: AppString, vararg args: Any): String = LocalStrings.current[id, args.toList()]
