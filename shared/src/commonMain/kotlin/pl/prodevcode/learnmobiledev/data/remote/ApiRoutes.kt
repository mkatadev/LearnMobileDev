package pl.prodevcode.learnmobiledev.data.remote

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

/**
 * Single source of truth for the shared module's API endpoints.
 *
 * Each endpoint is a `@Resource`-annotated `@Serializable` class or object: path segments
 * come from the annotation, path parameters are constructor properties, and so are query
 * parameters. Nesting expresses the URL hierarchy, with `parent` linking a segment to the
 * one above it.
 *
 * The point is that a URL stops being a string. `"$USERS_PATH/$userId/favorite"` compiles
 * whatever is interpolated into it — a null id, an unencoded name, a path that no longer
 * exists — and fails at runtime, on a device. A resource is checked by the compiler, and
 * the client encodes and escapes the values, so a user whose id contains a slash or an
 * accent cannot break the request.
 */
object ApiRoutes {

    @Resource("api/v1")
    @Serializable
    class V1 {

        @Resource("users")
        @Serializable
        class Users(val parent: V1 = V1(), val q: String? = null) {

            @Resource("{id}")
            @Serializable
            class Detail(val parent: Users = Users(), val id: String) {

                @Resource("favorite")
                @Serializable
                class Favorite(val parent: Detail)
            }
        }

        @Resource("roles")
        @Serializable
        class Roles(val parent: V1 = V1())

        @Resource("infographics")
        @Serializable
        class Infographics(val parent: V1 = V1()) {

            @Resource("{id}/image")
            @Serializable
            class Image(val parent: Infographics = Infographics(), val id: String)
        }

        @Resource("content/{resource}")
        @Serializable
        class Content(val parent: V1 = V1(), val resource: String, val lang: String? = null)
    }
}
