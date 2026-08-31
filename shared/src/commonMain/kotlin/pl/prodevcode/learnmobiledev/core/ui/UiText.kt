package pl.prodevcode.learnmobiledev.core.ui

import androidx.compose.runtime.Composable

/**
 * Text intended for the user, described **without** resolving it to a String.
 *
 * The problem this solves: a store cannot resolve text, because resolution needs the
 * composition (see [LocalStrings]). Baking a translated string into the presentation layer
 * would also make messages impossible to assert on independently of the current language.
 *
 * With [UiText] the store says *which* message to show, and the Compose layer decides
 * *how it reads* in the active language.
 */
sealed interface UiText {

    /** Text coming from outside (for example a server error). Not translatable. */
    data class Raw(val value: String) : UiText

    /** A reference to a catalogue entry with optional formatting arguments. */
    data class Resource(
        val id: AppString,
        val args: List<Any> = emptyList(),
    ) : UiText
}

/** Resolves a [UiText] into a string in the active language. */
@Composable
fun UiText.resolve(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Resource -> LocalStrings.current[id, args]
}

/** Shorthand for building messages inside stores. */
fun AppString.asUiText(vararg args: Any): UiText = UiText.Resource(this, args.toList())
