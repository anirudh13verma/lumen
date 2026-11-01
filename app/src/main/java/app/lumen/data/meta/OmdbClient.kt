package app.lumen.data.meta

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Looks up real IMDb ratings through OMDb (free key at omdbapi.com), because TMDB only has its own
 * ratings. Optional: without a key the app shows TMDB's rating, clearly labelled as TMDB.
 */
class OmdbClient(
    private val http: OkHttpClient,
    private val apiKey: String,
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank()

    private val json = Json { ignoreUnknownKeys = true }

    /** The IMDb rating (0..10) for [imdbId], or null if IMDb has none. Throws [IOException] on trouble. */
    suspend fun imdbRating(imdbId: String): Double? = withContext(Dispatchers.IO) {
        val url = "https://www.omdbapi.com/".toHttpUrl().newBuilder()
            .addQueryParameter("apikey", apiKey)
            .addQueryParameter("i", imdbId)
            .build()
        val body = http.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (response.code == 401) throw IOException("OMDb rejected the API key")
            if (!response.isSuccessful) throw IOException("OMDb returned HTTP ${response.code}")
            response.body?.string() ?: throw IOException("OMDb returned an empty response")
        }
        val parsed = try {
            json.decodeFromString<OmdbResponse>(body)
        } catch (e: SerializationException) {
            throw IOException("Unreadable OMDb response", e)
        }
        if (parsed.response.equals("False", ignoreCase = true)) {
            val error = parsed.error.orEmpty()
            // A bad key or an exhausted daily limit is trouble to retry later, not "no rating".
            if (error.contains("limit", ignoreCase = true) || error.contains("key", ignoreCase = true)) {
                throw IOException("OMDb: $error")
            }
            return@withContext null
        }
        parsed.imdbRating?.toDoubleOrNull()
    }

    @Serializable
    private data class OmdbResponse(
        @SerialName("Response") val response: String? = null,
        @SerialName("Error") val error: String? = null,
        val imdbRating: String? = null,
    )
}
