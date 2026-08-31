package pl.prodevcode.learnmobiledev.data.repository

import kotlinx.coroutines.delay
import pl.prodevcode.learnmobiledev.domain.model.User
import pl.prodevcode.learnmobiledev.domain.repository.NetworkFailureSwitch
import pl.prodevcode.learnmobiledev.domain.repository.UserRepository
import pl.prodevcode.learnmobiledev.domain.repository.UserSyncException

/**
 * The only class in the project that pretends to be a network.
 *
 * Delays and failures are **controllable** rather than random: the demo is repeatable and
 * the tests never flake. Replacing this with a Ktor-backed implementation would leave both
 * the domain and the presentation layer untouched.
 */
class InMemoryUserRepository(
    private val networkDelayMs: Long = 700,
    private val favoriteDelayMs: Long = 400,
) : UserRepository, NetworkFailureSwitch {

    override var failNextLoad: Boolean = false

    /** This user always rejects the favorite update, to demonstrate rollback. */
    private val brokenFavoriteUserId = "u-4"

    private val all = listOf(
        User("u-1", "Anna Kowalska", "anna@prodevcode.pl", "Android Developer"),
        User("u-2", "Bartek Nowak", "bartek@prodevcode.pl", "iOS Developer"),
        User("u-3", "Celina Wójcik", "celina@prodevcode.pl", "Tech Lead"),
        User("u-4", "Damian Zieliński", "damian@prodevcode.pl", "QA Engineer"),
        User("u-5", "Ewa Lewandowska", "ewa@prodevcode.pl", "Product Owner"),
        User("u-6", "Filip Mazur", "filip@prodevcode.pl", "Backend Developer"),
        User("u-7", "Gosia Krawczyk", "gosia@prodevcode.pl", "UX Designer"),
    )

    /** Favorites survive a reload, exactly as a real backend would make them. */
    private val favorites = mutableSetOf<String>()

    override suspend fun getUsers(query: String): List<User> {
        delay(networkDelayMs)
        if (failNextLoad) {
            throw UserSyncException.NetworkUnavailable()
        }
        return all
            .filter { it.matches(query) }
            .map { it.copy(isFavorite = it.id in favorites) }
    }

    override suspend fun setFavorite(userId: String, favorite: Boolean): Boolean {
        delay(favoriteDelayMs)
        if (userId == brokenFavoriteUserId) {
            throw UserSyncException.FavoriteRejected(userId)
        }
        if (favorite) favorites += userId else favorites -= userId
        return favorite
    }

    private fun User.matches(query: String): Boolean =
        query.isBlank() ||
            name.contains(query, ignoreCase = true) ||
            role.contains(query, ignoreCase = true) ||
            email.contains(query, ignoreCase = true)
}
