package pl.prodevcode.learnmobiledev.di

import org.koin.core.module.Module
import org.koin.dsl.module
import pl.prodevcode.learnmobiledev.domain.usecase.CreateUserUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.DeleteUserUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.GetAppLanguageUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.GetConcurrencyScenariosUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.GetInfographicsUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.GetLessonsUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.GetQuizQuestionsUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.GetRolesUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.GetThemeModeUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.RunConcurrencyScenarioUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.SearchUsersUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.SetFavoriteUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.SetAppLanguageUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.SetThemeModeUseCase
import pl.prodevcode.learnmobiledev.domain.usecase.UpdateUserUseCase

/**
 * The domain layer: use cases are stateless, so they are registered as `factory`.
 *
 * Note that this module has no idea **where** data comes from — it only declares a
 * dependency on repository interfaces. That is Dependency Inversion expressed in the
 * DI configuration.
 */
val domainModule: Module = module {
    factory { SearchUsersUseCase(get()) }
    factory { SetFavoriteUseCase(get()) }
    factory { UpdateUserUseCase(get()) }
    factory { CreateUserUseCase(get()) }
    factory { DeleteUserUseCase(get()) }
    factory { GetRolesUseCase(get()) }
    factory { GetLessonsUseCase(get()) }
    factory { GetInfographicsUseCase(get()) }
    factory { GetConcurrencyScenariosUseCase(get()) }
    factory { RunConcurrencyScenarioUseCase(get()) }
    factory { GetQuizQuestionsUseCase(get()) }
    factory { GetThemeModeUseCase(get()) }
    factory { SetThemeModeUseCase(get()) }
    factory { GetAppLanguageUseCase(get()) }
    factory { SetAppLanguageUseCase(get()) }
}
