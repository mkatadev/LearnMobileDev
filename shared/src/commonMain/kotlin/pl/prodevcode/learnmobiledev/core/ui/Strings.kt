package pl.prodevcode.learnmobiledev.core.ui

import androidx.compose.runtime.Composable
import learnmobiledev.shared.generated.resources.Res
import learnmobiledev.shared.generated.resources.allStringResources
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Resolution of [AppString] against the Compose Resources catalogue.
 *
 * The translations live in the `composeResources/values` directories as `strings.xml`,
 * so they are ordinary
 * Android string resources: the standard tooling reads them, translators recognise the
 * format, and `%1$s` formatting is the platform's rather than something written by hand.
 *
 * The lookup goes through the generated key map instead of a 128-branch `when` over
 * `Res.string.*`. A mapping that large is a place for a copy-paste mistake to hide, and
 * `AppStringResourcesTest` proves every key resolves — which the `when` could not.
 *
 * Because resources follow the **platform** locale, changing the language means changing
 * it on the platform and restarting; see `PlatformLocale`.
 */
private fun AppString.resource(): StringResource =
    Res.allStringResources[key] ?: error("missing string resource: $key")

/** Resolves a string in the active language. */
@Composable
fun localized(id: AppString): String = stringResource(id.resource())

/** Resolves a string with positional arguments. */
@Composable
fun localized(id: AppString, vararg args: Any): String =
    stringResource(id.resource(), *args)
