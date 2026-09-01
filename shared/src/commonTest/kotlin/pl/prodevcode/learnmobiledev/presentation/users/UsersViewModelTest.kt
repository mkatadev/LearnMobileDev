package pl.prodevcode.learnmobiledev.presentation.users

import pl.prodevcode.learnmobiledev.core.ui.AppString
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pl.prodevcode.learnmobiledev.core.mvi.Middleware
import learnmobiledev.shared.generated.resources.Res
import pl.prodevcode.learnmobiledev.core.ui.asUiText
import pl.prodevcode.learnmobiledev.domain.model.User
import pl.prodevcode.learnmobiledev.domain.repository.NetworkFailureSwitch
import pl.prodevcode.learnmobiledev.domain.repository.UserRepository
import pl.prodevcode.learnmobiledev.domain.repository.UserSyncException
import pl.prodevcode.learnmobiledev.domain.usecase.CreateUserUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.DeleteUserUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.GetRolesUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.SearchUsersUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.SetFavoriteUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.UpdateUserUseCase

/**
 * Store tests: they verify **asynchronous behaviour** (debounce, cancellation, rollback)
 * on virtual time, so the whole suite takes milliseconds instead of seconds.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UsersViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class TestRepository : UserRepository, NetworkFailureSwitch {
        override var failNextLoad: Boolean = false
        var favoriteShouldFail: Boolean = false
        var editShouldFail: Boolean = false
        var createShouldFail: Boolean = false
        var deleteShouldFail: Boolean = false
        var loadDelayMs: Long = 0
        val loadedQueries = mutableListOf<String>()
        val savedFavorites = mutableListOf<Pair<String, Boolean>>()
        val savedEdits = mutableListOf<Edit>()
        val createdUsers = mutableListOf<Edit>()
        val deletedUsers = mutableListOf<String>()
        var roles: List<String> = listOf("Tech Lead", "Android Developer")
        private var nextId = 100

        var users: List<User> = listOf(
            User("u-1", "Anna", "anna@x.pl", "Android"),
            User("u-2", "Bartek", "bartek@x.pl", "iOS"),
        )

        override suspend fun getRoles(): List<String> = roles

        override suspend fun createUser(name: String, email: String, role: String): User {
            createdUsers += Edit("", name, email, role)
            if (createShouldFail) throw UserSyncException.InvalidUser("new")
            val created = User("u-${nextId++}", name, email.lowercase(), role)
            users = users + created
            return created
        }

        override suspend fun deleteUser(userId: String) {
            deletedUsers += userId
            if (deleteShouldFail) throw UserSyncException.UserNotFound(userId)
            users = users.filterNot { it.id == userId }
        }

        override suspend fun getUsers(query: String): List<User> {
            loadedQueries += query
            if (loadDelayMs > 0) delay(loadDelayMs)
            if (failNextLoad) throw UserSyncException.NetworkUnavailable()
            return if (query.isBlank()) {
                users
            } else {
                users.filter { it.name.contains(query, ignoreCase = true) }
            }
        }

        override suspend fun setFavorite(userId: String, favorite: Boolean): Boolean {
            savedFavorites += userId to favorite
            if (favoriteShouldFail) throw UserSyncException.FavoriteRejected(userId)
            return favorite
        }

        override suspend fun updateUser(
            userId: String,
            name: String,
            email: String,
            role: String,
        ): User {
            savedEdits += Edit(userId, name, email, role)
            if (editShouldFail) throw UserSyncException.InvalidUser(userId)
            // Answering with a normalized value proves the store adopts what the server
            // stored rather than what the form held.
            val stored = User(userId, name, email.lowercase(), role)
            users = users.map { if (it.id == userId) stored else it }
            return stored
        }
    }

    private data class Edit(val userId: String, val name: String, val email: String, val role: String)

    private fun viewModel(
        repository: TestRepository,
        middlewares: List<Middleware<UsersState, UsersIntent>> = emptyList(),
    ) = UsersViewModel(
        searchUsers = SearchUsersUseCase(repository),
        setFavorite = SetFavoriteUseCase(repository),
        updateUser = UpdateUserUseCase(repository),
        createUser = CreateUserUseCase(repository),
        deleteUser = DeleteUserUseCase(repository),
        getRoles = GetRolesUseCase(repository),
        networkFailureSwitch = repository,
        middlewares = middlewares,
    )

    /**
     * Effects are collected on an [UnconfinedTestDispatcher]: the collector starts at once
     * and receives an event the moment it is emitted. With `StandardTestDispatcher` delivery
     * would depend on task ordering, which would make the test non-deterministic.
     */
    private fun TestScope.collectEffects(
        viewModel: UsersViewModel,
        into: MutableList<UsersEffect>,
    ) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { into += it }
        }
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `ScreenOpened loads users`() = runTest(dispatcher) {
        val repository = TestRepository()
        val vm = viewModel(repository)

        vm.dispatch(UsersIntent.Ui.ScreenOpened)
        advanceUntilIdle()

        assertEquals(2, vm.state.value.users.size)
        assertFalse(vm.state.value.isLoading)
        assertEquals(listOf(""), repository.loadedQueries)
    }

    @Test
    fun `a network failure produces an error state and a message effect`() = runTest(dispatcher) {
        val repository = TestRepository().apply { failNextLoad = true }
        val vm = viewModel(repository)
        val effects = mutableListOf<UsersEffect>()
        collectEffects(vm, effects)

        vm.dispatch(UsersIntent.Ui.ScreenOpened)
        advanceUntilIdle()

        // The assertion targets the message *identifier*, not its wording, so it survives
        // copy changes and the addition of translations.
        val expected = AppString.ErrorNetworkUnavailable.asUiText()
        assertEquals(expected, vm.state.value.error)
        assertEquals(listOf<UsersEffect>(UsersEffect.ShowMessage(expected)), effects)

        repository.failNextLoad = false
        vm.dispatch(UsersIntent.Ui.RetryClicked)
        advanceUntilIdle()

        assertEquals(null, vm.state.value.error)
        assertEquals(2, vm.state.value.users.size)
    }

    @Test
    fun `fast typing triggers only one request thanks to debounce`() = runTest(dispatcher) {
        val repository = TestRepository()
        val vm = viewModel(repository)

        vm.dispatch(UsersIntent.Ui.QueryChanged("A"))
        advanceTimeBy(100)
        vm.dispatch(UsersIntent.Ui.QueryChanged("An"))
        advanceTimeBy(100)
        vm.dispatch(UsersIntent.Ui.QueryChanged("Ann"))
        advanceUntilIdle()

        assertEquals(listOf("Ann"), repository.loadedQueries)
        assertEquals(listOf("Anna"), vm.state.value.users.map { it.name })
    }

    @Test
    fun `a new query cancels the previous load`() = runTest(dispatcher) {
        val repository = TestRepository().apply { loadDelayMs = 1_000 }
        val vm = viewModel(repository)

        vm.dispatch(UsersIntent.Ui.ScreenOpened)
        advanceTimeBy(100)
        vm.dispatch(UsersIntent.Ui.QueryChanged("Bartek"))
        advanceUntilIdle()

        // Both requests started, but only the latest result reached the state.
        assertEquals(listOf("", "Bartek"), repository.loadedQueries)
        assertEquals(listOf("Bartek"), vm.state.value.users.map { it.name })
    }

    @Test
    fun `a rejected favorite save is rolled back and reported`() =
        runTest(dispatcher) {
            val repository = TestRepository().apply { favoriteShouldFail = true }
            val vm = viewModel(repository)
            val effects = mutableListOf<UsersEffect>()
            collectEffects(vm, effects)

            vm.dispatch(UsersIntent.Ui.ScreenOpened)
            advanceUntilIdle()
            vm.dispatch(UsersIntent.Ui.FavoriteToggled("u-1"))
            advanceUntilIdle()

            assertFalse(vm.state.value.users.first { it.id == "u-1" }.isFavorite)
            assertTrue(vm.state.value.savingFavorites.isEmpty())
            assertTrue(effects.any { it is UsersEffect.ShowMessage })
            assertEquals(listOf("u-1" to true), repository.savedFavorites)
        }

    @Test
    fun `a successful favorite save keeps the change in state`() = runTest(dispatcher) {
        val repository = TestRepository()
        val vm = viewModel(repository)

        vm.dispatch(UsersIntent.Ui.ScreenOpened)
        advanceUntilIdle()
        vm.dispatch(UsersIntent.Ui.FavoriteToggled("u-1"))
        advanceUntilIdle()

        assertTrue(vm.state.value.users.first { it.id == "u-1" }.isFavorite)
        assertTrue(vm.state.value.savingFavorites.isEmpty())
    }

    @Test
    fun `clicking a user emits a navigation effect`() = runTest(dispatcher) {
        val vm = viewModel(TestRepository())
        val effects = mutableListOf<UsersEffect>()
        collectEffects(vm, effects)

        vm.dispatch(UsersIntent.Ui.UserClicked("u-2"))
        advanceUntilIdle()

        assertEquals(listOf<UsersEffect>(UsersEffect.OpenUserDetails("u-2")), effects)
    }

    @Test
    fun `the timeline allows jumping back to an earlier state`() = runTest(dispatcher) {
        val vm = viewModel(TestRepository())

        vm.dispatch(UsersIntent.Ui.ScreenOpened)
        advanceUntilIdle()
        vm.dispatch(UsersIntent.Ui.FavoriteToggled("u-1"))
        advanceUntilIdle()

        val beforeFavorite = vm.timeline.value.first { entry ->
            entry.intent is UsersIntent.Internal.LoadSucceeded
        }
        vm.jumpTo(beforeFavorite.index)

        assertFalse(vm.state.value.users.first { it.id == "u-1" }.isFavorite)
    }


    @Test
    fun `an edit is saved and the list shows what the server stored`() = runTest(dispatcher) {
        val repository = TestRepository()
        val vm = viewModel(repository)
        vm.dispatch(UsersIntent.Ui.ScreenOpened)
        advanceUntilIdle()

        vm.dispatch(UsersIntent.Ui.EditClicked("u-1"))
        vm.dispatch(UsersIntent.Ui.EditNameChanged("Anna Nowak"))
        vm.dispatch(UsersIntent.Ui.EditEmailChanged("ANNA@X.PL"))
        vm.dispatch(UsersIntent.Ui.EditSubmitted)
        advanceUntilIdle()

        assertEquals(1, repository.savedEdits.size)
        assertNull(vm.state.value.editor)
        val saved = vm.state.value.users.first { it.id == "u-1" }
        assertEquals("Anna Nowak", saved.name)
        // Lower-cased by the fake server: the store adopts the answer, not the form.
        assertEquals("anna@x.pl", saved.email)
    }

    /** Nothing typed, nothing to save: the store must not spend a round trip on it. */
    @Test
    fun `submitting an unchanged form performs no request`() = runTest(dispatcher) {
        val repository = TestRepository()
        val vm = viewModel(repository)
        vm.dispatch(UsersIntent.Ui.ScreenOpened)
        advanceUntilIdle()

        vm.dispatch(UsersIntent.Ui.EditClicked("u-1"))
        vm.dispatch(UsersIntent.Ui.EditSubmitted)
        advanceUntilIdle()

        assertTrue(repository.savedEdits.isEmpty())
        assertNull(vm.state.value.editor)
    }

    @Test
    fun `an incomplete form is not sent`() = runTest(dispatcher) {
        val repository = TestRepository()
        val vm = viewModel(repository)
        vm.dispatch(UsersIntent.Ui.ScreenOpened)
        advanceUntilIdle()

        vm.dispatch(UsersIntent.Ui.EditClicked("u-1"))
        vm.dispatch(UsersIntent.Ui.EditNameChanged(""))
        vm.dispatch(UsersIntent.Ui.EditSubmitted)
        advanceUntilIdle()

        assertTrue(repository.savedEdits.isEmpty())
        assertEquals("", vm.state.value.editor?.name)
    }

    @Test
    fun `a rejected edit reports a message and keeps the form open`() = runTest(dispatcher) {
        val repository = TestRepository().apply { editShouldFail = true }
        val vm = viewModel(repository)
        val effects = mutableListOf<UsersEffect>()
        collectEffects(vm, effects)
        vm.dispatch(UsersIntent.Ui.ScreenOpened)
        advanceUntilIdle()

        vm.dispatch(UsersIntent.Ui.EditClicked("u-1"))
        vm.dispatch(UsersIntent.Ui.EditEmailChanged("not-an-email"))
        vm.dispatch(UsersIntent.Ui.EditSubmitted)
        advanceUntilIdle()

        assertEquals(
            listOf<UsersEffect>(UsersEffect.ShowMessage(AppString.ErrorUserInvalid.asUiText())),
            effects,
        )
        assertEquals("not-an-email", vm.state.value.editor?.email)
        assertEquals("Anna", vm.state.value.users.first { it.id == "u-1" }.name)
    }

    @Test
    fun `creating a user sends it and shows the stored row`() = runTest(dispatcher) {
        val repository = TestRepository()
        val vm = viewModel(repository)
        vm.dispatch(UsersIntent.Ui.ScreenOpened)
        advanceUntilIdle()

        vm.dispatch(UsersIntent.Ui.AddClicked)
        vm.dispatch(UsersIntent.Ui.CreateNameChanged("  Nina Fresh  "))
        vm.dispatch(UsersIntent.Ui.CreateEmailChanged("NINA@X.PL"))
        vm.dispatch(UsersIntent.Ui.CreateRoleChanged("Tech Lead"))
        vm.dispatch(UsersIntent.Ui.CreateSubmitted)
        advanceUntilIdle()

        // Trimmed by the use case before it ever reaches the wire.
        assertEquals("Nina Fresh", repository.createdUsers.single().name)
        assertNull(vm.state.value.creator)
        // Lower-cased by the fake server: the list adopts the answer, not the form.
        assertEquals("nina@x.pl", vm.state.value.users.first().email)
    }

    @Test
    fun `an incomplete create is not sent`() = runTest(dispatcher) {
        val repository = TestRepository()
        val vm = viewModel(repository)
        vm.dispatch(UsersIntent.Ui.ScreenOpened)
        advanceUntilIdle()

        vm.dispatch(UsersIntent.Ui.AddClicked)
        vm.dispatch(UsersIntent.Ui.CreateNameChanged("Nina"))
        vm.dispatch(UsersIntent.Ui.CreateSubmitted)
        advanceUntilIdle()

        assertTrue(repository.createdUsers.isEmpty())
    }

    /** Swiping asks; only the confirmed dialog deletes. */
    @Test
    fun `a delete is only performed after confirmation`() = runTest(dispatcher) {
        val repository = TestRepository()
        val vm = viewModel(repository)
        vm.dispatch(UsersIntent.Ui.ScreenOpened)
        advanceUntilIdle()

        vm.dispatch(UsersIntent.Ui.DeleteClicked("u-1"))
        advanceUntilIdle()
        assertTrue(repository.deletedUsers.isEmpty(), "asking must not delete")

        vm.dispatch(UsersIntent.Ui.DeleteConfirmed)
        advanceUntilIdle()

        assertEquals(listOf("u-1"), repository.deletedUsers)
        assertTrue(vm.state.value.users.none { it.id == "u-1" })
    }

    @Test
    fun `a dismissed dialog deletes nobody`() = runTest(dispatcher) {
        val repository = TestRepository()
        val vm = viewModel(repository)
        vm.dispatch(UsersIntent.Ui.ScreenOpened)
        advanceUntilIdle()

        vm.dispatch(UsersIntent.Ui.DeleteClicked("u-1"))
        vm.dispatch(UsersIntent.Ui.DeleteDismissed)
        advanceUntilIdle()

        assertTrue(repository.deletedUsers.isEmpty())
        assertEquals(2, vm.state.value.users.size)
    }

    /** A failed delete keeps the row and says so, rather than losing somebody silently. */
    @Test
    fun `a failed delete keeps the row and reports it`() = runTest(dispatcher) {
        val repository = TestRepository().apply { deleteShouldFail = true }
        val effects = mutableListOf<UsersEffect>()
        val vm = viewModel(repository)
        collectEffects(vm, effects)
        vm.dispatch(UsersIntent.Ui.ScreenOpened)
        advanceUntilIdle()

        vm.dispatch(UsersIntent.Ui.DeleteClicked("u-1"))
        vm.dispatch(UsersIntent.Ui.DeleteConfirmed)
        advanceUntilIdle()

        assertEquals(2, vm.state.value.users.size)
        assertTrue(vm.state.value.deleting.isEmpty())
        assertTrue(
            effects.filterIsInstance<UsersEffect.ShowMessage>()
                .any { it.text == AppString.ErrorUserNotFound.asUiText() },
            "the failure was never reported",
        )
    }

    /** The roles are fetched on open, so the picker has something to offer. */
    @Test
    fun `the role catalogue is loaded with the screen`() = runTest(dispatcher) {
        val repository = TestRepository()
        val vm = viewModel(repository)

        vm.dispatch(UsersIntent.Ui.ScreenOpened)
        advanceUntilIdle()

        assertEquals(listOf("Android Developer", "Tech Lead"), vm.state.value.roles)
    }

    @Test
    fun `middleware observes every reduction`() = runTest(dispatcher) {
        val seen = mutableListOf<String>()
        val vm = viewModel(
            repository = TestRepository(),
            middlewares = listOf(Middleware { intent, _, _ -> seen += intent::class.simpleName!! }),
        )

        vm.dispatch(UsersIntent.Ui.ScreenOpened)
        advanceUntilIdle()

        // Opening the screen also fetches the role catalogue, on its own coroutine. Its
        // result can land on either side of the load, so the load sequence is asserted in
        // order and the catalogue separately — pinning one interleaving would be asserting
        // on coroutine scheduling rather than on behaviour.
        assertEquals(
            listOf("ScreenOpened", "LoadStarted", "LoadSucceeded"),
            seen.filterNot { it == "RolesLoaded" },
        )
        assertTrue("RolesLoaded" in seen, "the role catalogue was never loaded")
    }
}
