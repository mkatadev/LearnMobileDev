package pl.prodevcode.learnmobiledev.presentation.infographics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import pl.prodevcode.learnmobiledev.core.ui.UiText
import pl.prodevcode.learnmobiledev.domain.model.Infographic

/** Tests of the pure reducer: no coroutines, no mocks, no UI and no waiting. */
class InfographicsReducerTest {

    private val mvi = infographic("mvi-in-kmp")
    private val kmp = infographic("kmp-targets")

    private fun infographic(id: String) = Infographic(
        id = id,
        title = "title-$id",
        summary = "summary-$id",
        language = "pl",
        width = 1024,
        height = 1536,
        bytes = byteArrayOf(1, 2, 3),
    )

    private fun reduce(state: InfographicsState, intent: InfographicsIntent) =
        InfographicsReducer.reduce(state, intent)

    @Test
    fun `LoadStarted turns on the loader and clears the error`() {
        val result = reduce(
            InfographicsState(error = UiText.Raw("stale error")),
            InfographicsIntent.Internal.LoadStarted,
        )

        assertTrue(result.isLoading)
        assertNull(result.error)
    }

    @Test
    fun `LoadSucceeded fills the list and ends loading`() {
        val result = reduce(
            InfographicsState(isLoading = true),
            InfographicsIntent.Internal.LoadSucceeded(listOf(mvi, kmp)),
        )

        assertEquals(listOf(mvi, kmp), result.infographics)
        assertFalse(result.isLoading)
        assertNull(result.error)
    }

    @Test
    fun `LoadFailed sets the error and ends loading`() {
        val result = reduce(
            InfographicsState(isLoading = true),
            InfographicsIntent.Internal.LoadFailed(UiText.Raw("boom")),
        )

        assertEquals(UiText.Raw("boom"), result.error)
        assertFalse(result.isLoading)
    }

    @Test
    fun `an empty result yields the empty state rather than an error`() {
        val result = reduce(
            InfographicsState(isLoading = true),
            InfographicsIntent.Internal.LoadSucceeded(emptyList()),
        )

        assertTrue(result.showEmptyState)
        assertNull(result.error)
    }

    @Test
    fun `opening an infographic records which one is showing`() {
        val loaded = InfographicsState(infographics = listOf(mvi, kmp))

        val result = reduce(loaded, InfographicsIntent.Ui.InfographicOpened("kmp-targets"))

        assertEquals(kmp, result.opened)
    }

    /** A programmatic dispatch could reach this even where the UI offers no tap target. */
    @Test
    fun `opening an unknown infographic changes nothing`() {
        val loaded = InfographicsState(infographics = listOf(mvi))

        val result = reduce(loaded, InfographicsIntent.Ui.InfographicOpened("nope"))

        assertNull(result.opened)
    }

    @Test
    fun `dismissing the viewer closes it without touching the list`() {
        val open = reduce(
            InfographicsState(infographics = listOf(mvi)),
            InfographicsIntent.Ui.InfographicOpened("mvi-in-kmp"),
        )

        val result = reduce(open, InfographicsIntent.Ui.ViewerDismissed)

        assertNull(result.opened)
        assertEquals(listOf(mvi), result.infographics)
    }

    /** Leaving the viewer open on a picture the service no longer publishes shows nothing. */
    @Test
    fun `a reload that drops the open infographic closes the viewer`() {
        val open = reduce(
            InfographicsState(infographics = listOf(mvi, kmp)),
            InfographicsIntent.Ui.InfographicOpened("kmp-targets"),
        )

        val result = reduce(open, InfographicsIntent.Internal.LoadSucceeded(listOf(mvi)))

        assertNull(result.opened)
    }

    @Test
    fun `a reload that keeps it leaves the viewer open`() {
        val open = reduce(
            InfographicsState(infographics = listOf(mvi, kmp)),
            InfographicsIntent.Ui.InfographicOpened("kmp-targets"),
        )

        val result = reduce(open, InfographicsIntent.Internal.LoadSucceeded(listOf(kmp, mvi)))

        assertEquals(kmp, result.opened)
    }

    @Test
    fun `the reducer is deterministic`() {
        val state = InfographicsState(infographics = listOf(mvi))
        val intent = InfographicsIntent.Ui.InfographicOpened("mvi-in-kmp")

        assertEquals(reduce(state, intent), reduce(state, intent))
    }
}
