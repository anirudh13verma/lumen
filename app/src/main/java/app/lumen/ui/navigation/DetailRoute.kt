package app.lumen.ui.navigation

import java.util.Base64

/**
 * Route of the detail screen. Item ids can contain slashes and colons ("file:content://..."), so
 * the id is URL-safe Base64 encoded to travel safely inside a route.
 */
object DetailRoute {
    const val Pattern = "detail/{id}"
    const val ArgId = "id"

    fun create(itemId: String): String = "detail/" + Base64.getUrlEncoder().withoutPadding()
        .encodeToString(itemId.toByteArray(Charsets.UTF_8))

    fun decode(arg: String?): String? = try {
        arg?.let { String(Base64.getUrlDecoder().decode(it), Charsets.UTF_8) }
    } catch (e: IllegalArgumentException) {
        null
    }
}
