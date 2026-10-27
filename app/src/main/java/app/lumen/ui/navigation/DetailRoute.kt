package app.lumen.ui.navigation

/** Route of the title detail screen. */
object DetailRoute {
    const val Pattern = "detail/{id}"
    const val ArgId = "id"

    fun create(itemId: String): String = "detail/" + encodeRouteArg(itemId)

    fun decode(arg: String?): String? = decodeRouteArg(arg)
}

/** Route of the fullscreen player. [ArgStart] is the position to start from, in milliseconds. */
object PlayerRoute {
    const val Pattern = "player/{id}?start={start}"
    const val ArgId = "id"
    const val ArgStart = "start"

    fun create(documentUri: String, startMs: Long = 0): String =
        "player/" + encodeRouteArg(documentUri) + "?start=" + startMs

    fun decode(arg: String?): String? = decodeRouteArg(arg)
}
