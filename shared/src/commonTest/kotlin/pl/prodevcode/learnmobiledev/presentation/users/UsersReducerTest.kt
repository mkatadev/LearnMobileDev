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
    fun `the reducer is deterministic`() {
        val state = UsersState(users = listOf(anna, bartek))
        val intent = UsersIntent.Ui.QueryChanged("an")

        assertEquals(reduce(state, intent), reduce(state, intent))
    }
}
