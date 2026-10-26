package app.lumen.data.meta

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.coroutines.coroutineContext

/** TMDB rejected the API key (HTTP 401), so further requests are pointless. */
class TmdbAuthException : IOException("TMDB rejected the API key")

/**
 * Minimal TMDB v3 client: title search for films and shows.
 * All failures surface as [IOException] so callers can treat "offline" and "server trouble" alike.
 */
class TmdbClient(
    private val http: OkHttpClient,
    private val apiKey: String,
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Searches TMDB. [year] narrows the search to that release year (first air year for shows);
     * pass null to search by title alone.
     */
    suspend fun search(kind: MediaKind, query: String, year: Int?): List<TmdbMatch> =
        withContext(Dispatchers.IO) {
            val url = "https://api.themoviedb.org/3/search/${kind.key}".toHttpUrl().newBuilder()
                .addQueryParameter("api_key", apiKey)
                .addQueryParameter("query", query)
                .addQueryParameter("language", "en-US")
                .addQueryParameter("include_adult", "false")
                .apply {
                    if (year != null) {
                        addQueryParameter(if (kind == MediaKind.Movie) "year" else "first_air_date_year", year.toString())
                    }
                }
                .build()

            coroutineContext.ensureActive()
            val body = http.newCall(Request.Builder().url(url).build()).execute().use { response ->
                when {
                    response.code == 401 -> throw TmdbAuthException()
                    !response.isSuccessful -> throw IOException("TMDB returned HTTP ${response.code}")
                    else -> response.body?.string() ?: throw IOException("TMDB returned an empty response")
                }
            }

            val parsed = try {
                json.decodeFromString<SearchResponse>(body)
            } catch (e: kotlinx.serialization.SerializationException) {
                throw IOException("Unreadable TMDB response", e)
            }
            parsed.results.map { it.toMatch(kind) }
        }

    @Serializable
    private data class SearchResponse(val results: List<SearchResult> = emptyList())

    @Serializable
    private data class SearchResult(
        val id: Int,
        val title: String? = null,
        val name: String? = null,
        @SerialName("original_title") val originalTitle: String? = null,
        @SerialName("original_name") val originalName: String? = null,
        val overview: String? = null,
        @SerialName("poster_path") val posterPath: String? = null,
        @SerialName("backdrop_path") val backdropPath: String? = null,
        @SerialName("release_date") val releaseDate: String? = null,
        @SerialName("first_air_date") val firstAirDate: String? = null,
        @SerialName("vote_average") val voteAverage: Double? = null,
    ) {
        fun toMatch(kind: MediaKind): TmdbMatch {
            val date = if (kind == MediaKind.Movie) releaseDate else firstAirDate
            return TmdbMatch(
                kind = kind,
                id = id,
                title = (if (kind == MediaKind.Movie) title else name).orEmpty(),
                originalTitle = if (kind == MediaKind.Movie) originalTitle else originalName,
                year = date?.take(4)?.toIntOrNull(),
                overview = overview?.takeIf { it.isNotBlank() },
                posterPath = posterPath,
                backdropPath = backdropPath,
                rating = voteAverage?.takeIf { it > 0.0 },
            )
        }
    }
}
