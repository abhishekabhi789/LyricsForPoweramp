package io.github.abhishekabhi789.lyricsforpoweramp.helpers

import android.util.Log
import androidx.annotation.StringRes
import com.google.gson.Gson
import io.github.abhishekabhi789.lyricsforpoweramp.BuildConfig
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.Track
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URLEncoder
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

/**Helper to interacts with LRCLIB*/
class LrclibApiHelper(private val client: OkHttpClient) {

    sealed class ApiResult {
        data class Success(val data: String) : ApiResult()
        data class Failure(val error: Error) : ApiResult()
    }

    private suspend fun makeApiRequest(
        params: String,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        onComplete: (ApiResult) -> Unit
    ) {
        Log.d(TAG, "makeApiRequest: $params")
        try {
            val httpUrl = (API_BASE_URL + params).toHttpUrlOrNull()
            if (httpUrl != null) {
                val request = Request.Builder().run {
                    url(httpUrl)
                    header(
                        "User-Agent",
                        buildString {
                            append(BuildConfig.APPLICATION_ID)
                            append("-${BuildConfig.BUILD_TYPE}")
                            append(" ${BuildConfig.VERSION_NAME}")
                            append(" ${BuildConfig.GITHUB_REPO_URL}")
                        })
                    header("Content-Type", "application/json")
                    get()
                    build()
                }
                val result = withContext(dispatcher) {
                    suspendCancellableCoroutine { continuation ->
                        val call = client.newCall(request)
                        continuation.invokeOnCancellation {
                            Log.i(TAG, "makeApiRequest: continuation cancel invoked")
                            call.cancel()
                        }
                        call.enqueue(object : okhttp3.Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                if (call.isCanceled()) {
                                    Log.i(TAG, "onFailure: call cancelled")
                                    continuation.resume(ApiResult.Failure(Error.CANCELLED))
                                } else {
                                    Log.e(TAG, "onFailure: failed, ${e.message}")
                                    continuation.resume(ApiResult.Failure(Error.NETWORK_ERROR.apply {
                                        moreInfo = e.localizedMessage
                                    }))
                                }
                            }

                            override fun onResponse(call: Call, response: Response) {
                                try {
                                    when (response.code) {
                                        HttpURLConnection.HTTP_OK -> {
                                            response.body?.let { responseBody ->
                                                val result = responseBody.string()
                                                continuation.resume(ApiResult.Success(result))
                                            }
                                                ?: continuation.resume(ApiResult.Failure(Error.EMPTY_RESPONSE))
                                        }

                                        HttpURLConnection.HTTP_NOT_FOUND -> {
                                            val errorMsg = response.message
                                            Log.i(TAG, "makeApiRequest: no result $errorMsg")
                                            continuation.resume(ApiResult.Failure(Error.NO_RESULTS))
                                        }

                                        else -> {
                                            val errorMsg =
                                                "Request Failed, HTTP ${response.code}: ${response.message}"
                                            Log.e(TAG, "makeApiRequest: $errorMsg")
                                            continuation.resume(ApiResult.Failure(Error.NETWORK_ERROR))
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "makeApiRequest: Error processing response", e)
                                    continuation.resume(ApiResult.Failure(Error.PROCESSING_ERROR))
                                } finally {
                                    response.close()
                                }
                            }
                        })
                    }
                }
                onComplete(result)
            } else {
                Log.e(TAG, "makeApiRequest: failed to prepare url, prams: $params")
                onComplete(ApiResult.Failure(Error.URL_ERROR))
            }
        } catch (e: CancellationException) {
            // when job is cancelled
            Log.e(TAG, "CancellationException", e)
            onComplete(ApiResult.Failure(Error.CANCELLED))
        } catch (e: IllegalStateException) {
            //when the call has already been executed.
            Log.e(TAG, "IllegalArgumentException, may be the call has already been executed", e)
            onComplete(ApiResult.Failure(
                Error.NETWORK_ERROR.apply { moreInfo = e.localizedMessage }
            ))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "makeApiRequest: timeout exception", e)
            onComplete(ApiResult.Failure(Error.TIMEOUT))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected Exception during network request: ${e.message}", e)
            onComplete(ApiResult.Failure(Error.EXCEPTION.apply { moreInfo = e.localizedMessage }))
        }
    }

    /**Performs a search to get the best matching lyrics for the track.
     * @see <a href="https://lrclib.net/docs#:~:text=tranxuanthang/lrcget).-,Get%20lyrics%20with%20a%20track%27s%20signature,-GET">
     *     LRCLIB#Get lyrics with a track's signature</a>*/
    suspend fun getLyricsForTracks(
        track: Track,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        onResult: (Lyrics) -> Unit,
        onError: (Error) -> Unit
    ) {
        if (track.trackName.isNullOrEmpty()) {
            onError(Error.NO_TRACK_NAME)
            return
        }
        val requestParams = listOfNotNull(
            "track_name=${encode(track.trackName!!)}",
            track.artistName?.let { "artist_name=${encode(it)}" },
            track.albumName?.let { "album_name=${encode(it)}" },
            track.duration?.takeIf { it > 0 }?.let { "duration=$it" }
        ).joinToString(separator = "&", prefix = "get?")
        makeApiRequest(requestParams, dispatcher) { output ->
            when (output) {
                is ApiResult.Failure -> {
                    Log.e(TAG, "getLyricsForTracks: error ${output.error}")
                    onError(output.error)
                }

                is ApiResult.Success -> {
                    val result = Gson().fromJson(output.data, Lyrics::class.java)
                    Log.d(TAG, "getLyricsForTracks: search result ${result.trackName}")
                    onResult(result)
                }
            }
        }
    }

    /** Performs search for the given input.
     * @param query either the query string or a valid [Track]
     * @see <a href="https://lrclib.net/docs#:~:text=s%20example%20response.-,Search%20for%20lyrics%20records,-GET">
     *     LRCLIB#Search for lyrics records</a>*/
    suspend fun searchLyricsForTrack(
        query: Any,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        onResult: (List<Lyrics>) -> Unit,
        onError: (Error) -> Unit
    ) {
        val requestParams: String = when (query) {
            is String -> "search?q=${encode(query)}"

            is Track -> {
                if (query.trackName.isNullOrEmpty()) {
                    onError(Error.NO_TRACK_NAME)
                    return
                }
                listOfNotNull(
                    "track_name=${encode(query.trackName!!)}",
                    query.artistName?.let { "artist_name=${encode(it)}" },
                    query.albumName?.let { "album_name=${encode(it)}" }
                ).joinToString(separator = "&", prefix = "search?")
            }

            else -> {
                val error = "Invalid query type"
                Log.e(TAG, "$error $query")
                onError(Error.EXCEPTION.apply { moreInfo = error })
                return
            }
        }
        Log.d(TAG, "searchLyricsForTrack: $requestParams")
        makeApiRequest(requestParams, dispatcher) { results ->
            when (results) {
                is ApiResult.Success -> {
                    val parsedResponse = parseSearchResponse(results.data)
                    if (!parsedResponse.isNullOrEmpty()) {
                        Log.d(TAG, "searchLyricsForTrack: found ${parsedResponse.size} results")
                        onResult(parsedResponse)
                    } else {
                        Log.e(TAG, "searchLyricsForTrack: no result found $results")
                        onError(Error.NO_RESULTS)
                    }
                }

                is ApiResult.Failure -> onError(results.error)
            }
        }
    }

    /**Converts JSON response into List of [Lyrics].
     * Ensures either plain or synced lyrics present in each list items.*/
    private fun parseSearchResponse(searchResponse: String?): List<Lyrics>? {
        val results: Array<Lyrics>? = Gson().fromJson(searchResponse, Array<Lyrics>::class.java)
        return results?.toList()
//        return results?.filter { it.plainLyrics != null || it.syncedLyrics != null }?.toList()
    }

    private fun encode(text: String) = URLEncoder.encode(text, "UTF-8")

    enum class Error(@StringRes val errMsg: Int, var moreInfo: String? = null) {
        CANCELLED(R.string.error_cancelled),
        EMPTY_RESPONSE(R.string.error_empty_response),
        NETWORK_ERROR(R.string.error_network),
        TIMEOUT(R.string.error_timeout),
        EXCEPTION(R.string.error_exception),
        NO_TRACK_NAME(R.string.error_no_track_name),
        URL_ERROR(R.string.error_preparing_url),
        NO_RESULTS(R.string.error_no_results),
        PROCESSING_ERROR(R.string.error_processing_error)
    }

    companion object {
        private const val TAG = "LrclibApiHelper"
        private const val API_BASE_URL = "https://lrclib.net/api/"
    }
}
