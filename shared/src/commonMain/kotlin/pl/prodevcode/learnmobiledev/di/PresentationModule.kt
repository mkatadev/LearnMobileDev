package pl.prodevcode.learnmobiledev.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pl.prodevcode.learnmobiledev.presentation.app.AppShellViewModel
import pl.prodevcode.learnmobiledev.presentation.concurrency.ConcurrencyViewModel
import pl.prodevcode.learnmobiledev.presentation.infographics.InfographicsViewModel
import pl.prodevcode.learnmobiledev.presentation.learn.LearnViewModel
import pl.prodevcode.learnmobiledev.presentation.quiz.QuizViewModel
import pl.prodevcode.learnmobiledev.presentation.users.UsersViewModel

/**
 * The presentation layer.
 *
 * `viewModel { }` creates stores inside a `ViewModelStore`, so an instance survives
 * recomposition and configuration changes, and `viewModelScope` is cancelled
 * automatically. That matters in MVI: a store recreated on every recomposition would
 * lose both the state and the timeline.
 *
 * Explicit `get()` instead of the constructor DSL, because the last constructor
 * parameter (the middleware list) has a default value the DSL would not honour.
 */
val presentationModule: Module = module {
    viewModel {
        UsersViewModel(
            searchUsers = get(),
            setFavorite = get(),
            updateUser = get(),
            createUser = get(),
            deleteUser = get(),
            getRoles = get(),
            networkFailureSwitch = get(),
        )
    }
    viewModel { LearnViewModel(get()) }
    viewModel { InfographicsViewModel(get()) }
    viewModel { ConcurrencyViewModel(get(), get()) }
    viewModel { QuizViewModel(get()) }
    viewModel { AppShellViewModel(get(), get(), get(), get()) }
}
