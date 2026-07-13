package io.github.mrxgamer999.openinstremio.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import io.github.mrxgamer999.openinstremio.BuildConfig
import io.github.mrxgamer999.openinstremio.data.tmdb.TmdbService
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

// Single process-wide DataStore, shared by the UI and the extension service.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("open_in_stremio")

/**
 * Manual dependency graph. The app is small enough that a DI framework would be overhead;
 * ViewModels receive their dependencies from here at the `viewModel { }` call site, which
 * keeps everything swappable with fakes in tests.
 */
object AppGraph {

    private val json = Json { ignoreUnknownKeys = true }

    // Tight timeouts: the TMDb fallback runs on the extension's worker thread while a user
    // is looking at a SeriesGuide screen, so failing fast beats waiting.
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    private val tmdbService: TmdbService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TmdbService::class.java)
    }

    @Volatile private var imdbResolver: ImdbResolver? = null

    fun imdbResolver(context: Context): ImdbResolver =
        imdbResolver
            ?: synchronized(this) {
                imdbResolver
                    ?: DefaultImdbResolver(
                            tmdb = tmdbService,
                            cache = DataStoreImdbIdCache(context.applicationContext.dataStore),
                            apiKey = BuildConfig.TMDB_API_KEY,
                        )
                        .also { imdbResolver = it }
            }

    fun dataStore(context: Context): DataStore<Preferences> = context.applicationContext.dataStore
}
