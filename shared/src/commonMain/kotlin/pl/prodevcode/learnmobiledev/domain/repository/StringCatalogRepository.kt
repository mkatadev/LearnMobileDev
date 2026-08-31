package pl.prodevcode.learnmobiledev.domain.repository

/** Port supplying the UI string catalogue for the active language. */
interface StringCatalogRepository {

    /** Key to translation, for the language currently in effect. */
    suspend fun getStrings(): Map<String, String>
}

/** Signals that the string catalogue could not be loaded. Message is technical, for logs. */
class StringsUnavailableException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
