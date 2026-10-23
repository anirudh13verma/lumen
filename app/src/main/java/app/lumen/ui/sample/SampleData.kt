package app.lumen.ui.sample

/**
 * Throwaway placeholder content so the UI has something to show before the
 * scanner and TMDB matching exist. Delete once real library data is wired in.
 */
data class SampleItem(
    val title: String,
    val subtitle: String,
    /** 0..1 watch progress, or null if unwatched. */
    val progress: Float? = null,
)

val SampleContinueWatching = listOf(
    SampleItem("Sample Movie One", "1h 12m left", 0.55f),
    SampleItem("Sample Show", "S02 · E03", 0.30f),
    SampleItem("Sample Movie Two", "24m left", 0.85f),
    SampleItem("Another Show", "S01 · E07", 0.10f),
)

val SampleRecentlyAdded = List(10) { index ->
    SampleItem("Sample Title ${index + 1}", if (index % 2 == 0) "2024" else "2023")
}
