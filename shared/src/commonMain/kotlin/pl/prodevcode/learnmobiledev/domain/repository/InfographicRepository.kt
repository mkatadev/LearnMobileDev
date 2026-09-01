package pl.prodevcode.learnmobiledev.domain.repository

import pl.prodevcode.learnmobiledev.domain.model.Infographic

/** Port for the published infographics. */
interface InfographicRepository {

    /**
     * The infographics, pictures included.
     *
     * The metadata and the bytes arrive over separate calls, but that is the adapter's
     * business: a caller asks for infographics and gets whole ones. Splitting the two here
     * would push the service's transport shape into the domain.
     */
    suspend fun getInfographics(): List<Infographic>
}

/** Signals that the infographics could not be loaded. Message is technical, for logs only. */
class InfographicsUnavailableException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
