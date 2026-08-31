package pl.prodevcode.learnmobiledev.domain.usecase

import pl.prodevcode.learnmobiledev.domain.repository.StringCatalogRepository

/** Loads the UI string catalogue for the active language. */
class GetStringCatalogUseCase(
    private val repository: StringCatalogRepository,
) {
    suspend operator fun invoke(): Map<String, String> = repository.getStrings()
}
