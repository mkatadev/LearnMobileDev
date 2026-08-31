package pl.prodevcode.learnmobiledev.presentation.users

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import pl.prodevcode.learnmobiledev.core.ui.UiText
import pl.prodevcode.learnmobiledev.domain.model.User

/**
 * Tests of the **pure reducer**: no coroutines, no mocks, no UI and no waiting.
 * This is exactly the property that makes teams pick MVI.
 */
class UsersReducerTest {

    private val anna = User("u-1", "Anna", "anna@x.pl", "Android")
    private val bartek = User("u-2", "Bartek", "bartek@x.pl", "iOS")

    private fun reduce(state: UsersState, intent: UsersIntent) =
        UsersReducer.reduce(state, intent)

    @Test
    fun `LoadStarted turns on the loader and clears the error`() {
        val result = reduce(
            UsersState(error = UiText.Raw("stale error")),
            UsersIntent.Internal.LoadStarted,
        )

        assertTrue(result.isLoading)
        assertNull(result.error)
    }

    @Test
    fun `LoadSucceeded fills the list and turns off the loaders`() {
        val result = reduce(
            UsersState(isLoading = true, isRefreshing = true),
            UsersIntent.Internal.LoadSucceeded(listOf(anna, bartek)),
        )

        assertEquals(listOf(anna, bartek), result.users)
        assertFalse(result.isLoading)
        assertFalse(result.isRefreshing)
        assertNull(result.error)
    }

    @Test
    fun `LoadFailed sets the error and ends loading`() {
        val result = reduce(
            UsersState(isLoading = true),
            UsersIntent.Internal.LoadFailed(UiText.Raw("HTTP 503")),
        )

        assertEquals(UiText.Raw("HTTP 503"), result.error)
        assertFalse(result.isLoading)
    }

    @Test
    fun `an empty result yields the empty state rather than an error`() {
        val result = reduce(
            UsersState(query = "zzz", isLoading = true),
            UsersIntent.Internal.LoadSucceeded(emptyList()),
        )

        assertTrue(result.showEmptyState)
        assertFalse(result.showContent)
    }

    @Test
    fun `FavoriteToggled applies the optimistic update immediately`() {
        val result = reduce(
            UsersState(users = listOf(anna, bartek)),
            UsersIntent.Ui.FavoriteToggled("u-1"),
        )

        assertTrue(result.users.first { it.id == "u-1" }.isFavorite)
        assertFalse(result.users.first { it.id == "u-2" }.isFavorite)
    }

    @Test
    fun `FavoriteSaveFailed rolls the optimistic update back`() {
        val optimistic = reduce(
            UsersState(users = listOf(anna)),
            UsersIntent.Ui.FavoriteToggled("u-1"),
        )
        val saving = reduce(optimistic, UsersIntent.Internal.FavoriteSaveStarted("u-1"))

        val rolledBack = reduce(
            saving,
            UsersIntent.Internal.FavoriteSaveFailed("u-1", UiText.Raw("offline")),
        )

        assertFalse(rolledBack.users.first().isFavorite)
        assertTrue(rolledBack.savingFavorites.isEmpty())
    }

    @Test
    fun `a double tap while saving leaves the state unchanged`() {
        val saving = UsersState(
            users = listOf(anna),
            savingFavorites = setOf("u-1"),
        )

        val result = reduce(saving, UsersIntent.Ui.FavoriteToggled("u-1"))

        assertEquals(saving, result)
    }

    @Test
    fun `a refresh does not drop a favorite that is still being saved`() {
        val pending = UsersState(
            users = listOf(anna.copy(isFavorite = true)),
            savingFavorites = setOf("u-1"),
        )

        val result = reduce(
            pending,
            UsersIntent.Internal.LoadSucceeded(listOf(anna.copy(isFavorite = false))),
        )

        assertTrue(result.users.first().isFavorite)
    }


    @Test
    fun `EditClicked copies the row into a draft`() {
        val result = reduce(UsersState(users = listOf(anna, bartek)), UsersIntent.Ui.EditClicked("u-2"))

        assertEquals(UserEditor.of(bartek), result.editor)
    }

    @Test
    fun `EditClicked on an unknown user changes nothing`() {
        val state = UsersState(users = listOf(anna))

        assertEquals(state, reduce(state, UsersIntent.Ui.EditClicked("u-9")))
    }

    /** Editing the draft must leave the list alone until the server has agreed. */
    @Test
    fun `typing changes the draft and not the list`() {
        val open = reduce(UsersState(users = listOf(anna)), UsersIntent.Ui.EditClicked("u-1"))

        val result = reduce(open, UsersIntent.Ui.EditNameChanged("Anna Nowak"))

        assertEquals("Anna Nowak", result.editor?.name)
        assertEquals(anna, result.users.first())
    }

    @Test
    fun `a cancelled edit is discarded`() {
        val open = reduce(UsersState(users = listOf(anna)), UsersIntent.Ui.EditClicked("u-1"))
        val typed = reduce(open, UsersIntent.Ui.EditRoleChanged("Tech Lead"))

        val result = reduce(typed, UsersIntent.Ui.EditCancelled)

        assertNull(result.editor)
        assertEquals(anna, result.users.first())
    }

    @Test
    fun `an empty field blocks the save`() {
        val open = reduce(UsersState(users = listOf(anna)), UsersIntent.Ui.EditClicked("u-1"))

        val result = reduce(open, UsersIntent.Ui.EditNameChanged("  "))

        assertFalse(result.editor!!.canSave)
    }

    @Test
    fun `a save in flight blocks another one`() {
        val open = reduce(UsersState(users = listOf(anna)), UsersIntent.Ui.EditClicked("u-1"))

        val result = reduce(open, UsersIntent.Internal.EditSaveStarted)

        assertTrue(result.editor!!.isSaving)
        assertFalse(result.editor!!.canSave)
    }

    /** The list adopts the stored row, which need not equal what was typed. */
    @Test
    fun `a saved edit closes the editor and replaces the row`() {
        val open = reduce(UsersState(users = listOf(anna, bartek)), UsersIntent.Ui.EditClicked("u-1"))
        val stored = anna.copy(name = "Anna Nowak", role = "Tech Lead")

        val result = reduce(open, UsersIntent.Internal.EditSaveSucceeded(stored))

        assertNull(result.editor)
        assertEquals(listOf(stored, bartek), result.users)
    }

    /** A form that empties itself on failure is a form people retype. */
    @Test
    fun `a rejected edit keeps the editor open with the typed values`() {
        val open = reduce(UsersState(users = listOf(anna)), UsersIntent.Ui.EditClicked("u-1"))
        val typed = reduce(open, UsersIntent.Ui.EditEmailChanged("nope"))
        val saving = reduce(typed, UsersIntent.Internal.EditSaveStarted)

        val result = reduce(saving, UsersIntent.Internal.EditSaveFailed(UiText.Raw("rejected")))

        assertEquals("nope", result.editor?.email)
        assertFalse(result.editor!!.isSaving)
    }

    @Test
    fun `the reducer is deterministic`() {
        val state = UsersState(users = listOf(anna, bartek))
        val intent = UsersIntent.Ui.QueryChanged("an")

        assertEquals(reduce(state, intent), reduce(state, intent))
    }
}
