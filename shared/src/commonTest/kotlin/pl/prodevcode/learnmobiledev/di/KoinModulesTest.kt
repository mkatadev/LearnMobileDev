package pl.prodevcode.learnmobiledev.di

import kotlin.test.Test
import kotlin.test.assertSame
import org.koin.core.context.stopKoin
import org.koin.dsl.koinApplication
import pl.prodevcode.learnmobiledev.data.repository.ApiUserRepository
import pl.prodevcode.learnmobiledev.domain.repository.NetworkFailureSwitch
import pl.prodevcode.learnmobiledev.domain.repository.UserRepository
import pl.prodevcode.learnmobiledev.domain.usecase.SearchUsersUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.SetFavoriteUseCase

/**
 * A dependency graph is code too, and it can break like any other code. This test catches
 * a missing definition before a user does in production.
 */
class KoinModulesTest {

    @Test
    fun `the dependency graph can be resolved`() {
        val koin = koinApplication { modules(dataModule, domainModule) }.koin

        koin.get<UserRepository>()
        koin.get<SearchUsersUseCase>()
        koin.get<SetFavoriteUseCase>()

        stopKoin()
    }

    @Test
    fun `repository and failure switch are the same instance`() {
        val koin = koinApplication { modules(dataModule) }.koin

        val repository = koin.get<UserRepository>()
        val failureSwitch = koin.get<NetworkFailureSwitch>()

        assertSame(repository as ApiUserRepository, failureSwitch)

        stopKoin()
    }
}
