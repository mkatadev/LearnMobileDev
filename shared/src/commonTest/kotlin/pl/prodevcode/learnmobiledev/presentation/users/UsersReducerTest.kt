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
    fun `a created user appears at the top of the list`() {
        val state = UsersState(users = listOf(anna, bartek), creator = UserEditor.empty("Tech Lead"))
        val created = User("u-9", "Nina", "nina@x.pl", "Tech Lead")

        val result = reduce(state, UsersIntent.Internal.CreateSaveSucceeded(created))

        assertNull(result.creator)
        assertEquals(listOf(created, anna, bartek), result.users)
    }

    /** A rejected create keeps the typed values, for the same reason an edit does. */
    @Test
    fun `a rejected create keeps the form open`() {
        val state = UsersState(roles = listOf("Tech Lead"))
        val open = reduce(state, UsersIntent.Ui.AddClicked)
        val typed = reduce(open, UsersIntent.Ui.CreateNameChanged("Nina"))
        val saving = reduce(typed, UsersIntent.Internal.CreateSaveStarted)

        val result = reduce(saving, UsersIntent.Internal.CreateSaveFailed(UiText.Raw("nope")))

        assertEquals("Nina", result.creator?.name)
        assertFalse(result.creator!!.isSaving)
    }

    /** The picker offers a closed list, so a blank role is never a valid starting point. */
    @Test
    fun `a new form starts on a real role`() {
        val state = UsersState(roles = listOf("Android Developer", "Tech Lead"))

        val result = reduce(state, UsersIntent.Ui.AddClicked)

        assertEquals("Android Developer", result.creator?.role)
    }

    /** Asking is not doing: a swipe must not remove anybody on its own. */
    @Test
    fun `DeleteClicked only asks for confirmation`() {
        val state = UsersState(users = listOf(anna, bartek))

        val result = reduce(state, UsersIntent.Ui.DeleteClicked("u-1"))

        assertEquals(anna, result.pendingDeletion)
        assertEquals(listOf(anna, bartek), result.users)
    }

    /** The row leaves only once the server agrees — there is no optimistic delete. */
    @Test
    fun `a confirmed delete closes the dialog but keeps the row`() {
        val asked = reduce(
            UsersState(users = listOf(anna, bartek)),
            UsersIntent.Ui.DeleteClicked("u-1"),
        )

        val result = reduce(asked, UsersIntent.Ui.DeleteConfirmed)

        assertNull(result.pendingDeletion)
        assertEquals(listOf(anna, bartek), result.users)
    }

    @Test
    fun `a deleted user is removed once the server confirms`() {
        val deleting = reduce(
            UsersState(users = listOf(anna, bartek)),
            UsersIntent.Internal.DeleteStarted("u-1"),
        )

        val result = reduce(deleting, UsersIntent.Internal.DeleteSucceeded("u-1"))

        assertEquals(listOf(bartek), result.users)
        assertTrue(result.deleting.isEmpty())
    }

    /** A failed delete puts nothing back, because nothing was taken away. */
    @Test
    fun `a failed delete leaves the list intact`() {
        val deleting = reduce(
            UsersState(users = listOf(anna, bartek)),
            UsersIntent.Internal.DeleteStarted("u-1"),
        )

        val result = reduce(deleting, UsersIntent.Internal.DeleteFailed("u-1", UiText.Raw("nope")))

        assertEquals(listOf(anna, bartek), result.users)
        assertTrue(result.deleting.isEmpty())
    }

    /** A catalogue arriving late must not silently change what the user is about to submit. */
    @Test
    fun `late roles do not overwrite a role the user picked`() {
        val open = reduce(UsersState(roles = listOf("Tech Lead")), UsersIntent.Ui.AddClicked)
        val picked = reduce(open, UsersIntent.Ui.CreateRoleChanged("QA Engineer"))

        val result = reduce(picked, UsersIntent.Internal.RolesLoaded(listOf("Android Developer")))

        assertEquals("QA Engineer", result.creator?.role)
    }

    @Test
    fun `the reducer is deterministic`() {        val state = UsersState(users = listOf(anna, bartek))
        val intent = UsersIntent.Ui.QueryChanged("an")

        assertEquals(reduce(state, intent), reduce(state, intent))
    }
}
