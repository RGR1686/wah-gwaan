package bs.wahgwaan.di

import android.content.Context
import androidx.room.Room
import bs.wahgwaan.BuildConfig
import bs.wahgwaan.data.db.EventDao
import bs.wahgwaan.data.db.WahGwaanDatabase
import bs.wahgwaan.data.location.GeocodingService
import bs.wahgwaan.data.location.NoOpGeocoder
import bs.wahgwaan.data.network.EventsApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): WahGwaanDatabase =
        Room.databaseBuilder(context, WahGwaanDatabase::class.java, "wahgwaan.db")
            .fallbackToDestructiveMigration()   // cache DB: feed re-syncs it
            .build()

    @Provides
    fun eventDao(db: WahGwaanDatabase): EventDao = db.eventDao()

    @Provides
    @Singleton
    fun prefs(@ApplicationContext context: Context): android.content.SharedPreferences =
        context.getSharedPreferences("wahgwaan_meta", Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    @Provides
    @Singleton
    fun eventsApi(client: OkHttpClient): EventsApi {
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
        return Retrofit.Builder()
            .baseUrl(BuildConfig.FEED_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(EventsApi::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {
    /** v1: offline island gazetteer only. Swap this binding for a real
     *  geocoder implementation to light up Type B/C globally. */
    @Binds
    abstract fun geocoder(impl: NoOpGeocoder): GeocodingService
}
