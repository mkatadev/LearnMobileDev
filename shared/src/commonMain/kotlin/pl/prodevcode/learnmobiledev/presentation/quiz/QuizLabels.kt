package pl.prodevcode.learnmobiledev.presentation.quiz

import pl.prodevcode.learnmobiledev.core.ui.AppString
import learnmobiledev.shared.generated.resources.Res
import pl.prodevcode.learnmobiledev.domain.model.Difficulty
import pl.prodevcode.learnmobiledev.domain.model.QuizCategory

/**
 * Maps domain enums to display labels.
 *
 * This mapping lives in the presentation layer on purpose: how a category is spelled is
 * a UI concern, and hard-coding it in the domain enum would make it impossible to
 * translate. The compiler enforces completeness through the exhaustive `when`.
 */
val QuizCategory.labelRes: AppString
    get() = when (this) {
        QuizCategory.Mvi -> AppString.CategoryMvi
        QuizCategory.Kmp -> AppString.CategoryKmp
        QuizCategory.Coroutines -> AppString.CategoryCoroutines
        QuizCategory.Rx -> AppString.CategoryRx
        QuizCategory.CleanArchitecture -> AppString.CategoryClean
        QuizCategory.Solid -> AppString.CategorySolid
        QuizCategory.Compose -> AppString.CategoryCompose
        QuizCategory.Testing -> AppString.CategoryTesting
        QuizCategory.Kotlin -> AppString.CategoryKotlin
        QuizCategory.Android -> AppString.CategoryAndroid
        QuizCategory.Performance -> AppString.CategoryPerformance
        QuizCategory.DataStructures -> AppString.CategoryDataStructures
    }

val Difficulty.labelRes: AppString
    get() = when (this) {
        Difficulty.Mid -> AppString.DifficultyMid
        Difficulty.Senior -> AppString.DifficultySenior
    }
