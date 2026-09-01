package pl.prodevcode.learnmobiledev.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import pl.prodevcode.learnmobiledev.data.infographic.toDomain
import pl.prodevcode.learnmobiledev.data.remote.InfographicsApi
import pl.prodevcode.learnmobiledev.domain.model.Infographic
import pl.prodevcode.learnmobiledev.domain.repository.InfographicRepository
import pl.prodevcode.learnmobiledev.domain.repository.InfographicsUnavailableException

/**
 * Adapter for [InfographicRepository]: fetches the catalogue, then the pictures it names.
 *
 * The two-call shape of the service stops here. A caller asks for infographics and receives
 * whole ones; that the bytes arrived separately is transport detail, and letting it leak
 * upward would make every screen orchestrate downloads.
 *
 * Images are fetched concurrently, because they are independent and each costs a round
 * trip — doing them in sequence would make the screen wait for the sum rather than the
 * slowest.
 *
 * Cached, and unlike the course content **not** keyed by language: the text is baked into
 * the pixels, so switching the app's language does not change which bytes are correct.
 */
class ApiInfographicRepository(
    private val api: InfographicsApi,
) : InfographicRepository {

    private var cached: List<Infographic>? = null

    override suspend fun getInfographics(): List<Infographic> {
        cached?.let { return it }

        val infographics = try {
            coroutineScope {
                api.getInfographics()
                    .map { dto -> dto to async { api.getImage(dto.id) } }
                    .map { (dto, bytes) -> dto.toDomain(bytes.await()) }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation // a control signal, not a domain failure
        } catch (error: Exception) {
            throw InfographicsUnavailableException("Failed to load infographics", error)
        }

        cached = infographics
        return infographics
    }
}
