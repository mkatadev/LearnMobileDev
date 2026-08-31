package pl.prodevcode.learnmobiledev.presentation.users

import pl.prodevcode.learnmobiledev.core.ui.AppString
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
import pl.prodevcode.learnmobiledev.domain.usecase.SearchUsersUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.SetFavoriteUseCase

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
        var loadDelayMs: Long = 0
        val loadedQueries = mutableListOf<String>()
        val savedFavorites = mutableListOf<Pair<String, Boolean>>()

        var users: List<User> = listOf(
            User("u-1", "Anna", "anna@x.pl", "Android"),
            User("u-2", "Bartek", "bartek@x.pl", "iOS"),
        )

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
    }

    private fun viewModel(
        repository: TestRepository,
        middlewares: List<Middleware<UsersState, UsersIntent>> = emptyList(),
    ) = UsersViewModel(
        searchUsers = SearchUsersUseCase(repository),
        setFavorite = SetFavoriteUseCase(repository),
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
    fun `middleware observes every reduction`() = runTest(dispatcher) {
        val seen = mutableListOf<String>()
        val vm = viewModel(
            repository = TestRepository(),
            middlewares = listOf(Middleware { intent, _, _ -> seen += intent::class.simpleName!! }),
        )

        vm.dispatch(UsersIntent.Ui.ScreenOpened)
        advanceUntilIdle()

        assertEquals(listOf("ScreenOpened", "LoadStarted", "LoadSucceeded"), seen)
    }
}
