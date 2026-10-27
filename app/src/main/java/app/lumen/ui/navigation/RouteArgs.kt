package app.lumen.ui.navigation

import java.util.Base64

/**
 * Ids and URIs contain slashes and colons, so they are URL-safe Base64 encoded to travel inside a
 * navigation route.
 */
internal fun encodeRouteArg(value: String): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

internal fun decodeRouteArg(arg: String?): String? = try {
    arg?.let { String(Base64.getUrlDecoder().decode(it), Charsets.UTF_8) }
} catch (e: IllegalArgumentException) {
    null
}
