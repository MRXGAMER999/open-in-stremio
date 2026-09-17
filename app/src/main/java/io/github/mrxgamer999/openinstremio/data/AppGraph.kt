package io.github.mrxgamer999.openinstremio.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import io.github.mrxgamer999.openinstremio.BuildConfig
import io.github.mrxgamer999.openinstremio.data.github.DefaultUpdateRepository
import io.github.mrxgamer999.openinstremio.data.github.GitHubService
import io.github.mrxgamer999.openinstremio.data.github.UpdateRepository
import io.github.mrxgamer999.openinstremio.data.tmdb.TmdbService
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

// Single process-wide DataStore, shared by the UI and the extension receiver.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("open_in_stremio")

/**
 * Manual dependency graph. The app is small enough that a DI framework would be overhead;
 * ViewModels receive their dependencies from here at the `viewModel { }` call site, which
 * keeps everything swappable with fakes in tests.
 */
object AppGraph {

    private val json = Json { ignoreUnknownKeys = true }

    // Baseline for user-initiated calls, i.e. the update check: a tap deserves a longer leash
    // than a background lookup.
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    // Tighter deadlines for the TMDb fallback: it runs detached, after the extension broadcast has
    // already been answered, and is only worth anything while SeriesGuide still remembers the
    // title — so failing fast beats waiting. Derived, so it shares the connection pool.
    private val tmdbHttpClient: OkHttpClient by lazy {
        okHttpClient
            .newBuilder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .callTimeout(4, TimeUnit.SECONDS)
            .build()
    }

    private val tmdbService: TmdbService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(tmdbHttpClient)
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

    private val gitHubService: GitHubService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GitHubService::class.java)
    }

    fun updateRepository(): UpdateRepository = DefaultUpdateRepository(gitHubService)

    fun dataStore(context: Context): DataStore<Preferences> = context.applicationContext.dataStore

    fun playerChoiceStore(context: Context): PlayerChoiceStore = PlayerChoiceStore(dataStore(context))

    fun extensionStatusRepository(context: Context): ExtensionStatusRepository =
        ExtensionStatusRepository(
            packageChecker = AndroidPackageChecker(context.applicationContext),
            extensionActive = ExtensionStateStore(dataStore(context)).active,
        )
}
