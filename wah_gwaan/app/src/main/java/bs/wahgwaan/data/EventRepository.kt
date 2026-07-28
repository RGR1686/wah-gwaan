package bs.wahgwaan.data

import bs.wahgwaan.data.db.EventDao
import bs.wahgwaan.data.db.FavoriteEntity
import bs.wahgwaan.data.db.toDomain
import bs.wahgwaan.data.network.EventsApi
import bs.wahgwaan.data.network.toEntity
import bs.wahgwaan.model.BahamianIsland
import bs.wahgwaan.model.DateRangeFilter
import bs.wahgwaan.model.Event
import bs.wahgwaan.model.LocationFilter
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first orchestration. Room is the single source of truth: the UI
 * only ever observes the database, and network syncs write into it.
 */
@Singleton
class EventRepository @Inject constructor(
    private val dao: EventDao,
    private val api: EventsApi,
    private val prefs: SharedPreferences,
) {
    private val _lastSyncEpochMs =
        MutableStateFlow(prefs.getLong(KEY_LAST_SYNC, 0L))

    /** Epoch millis of the last successful feed sync; 0 = never. Surfaced
     *  in the UI so feed freshness is honest, not implied. */
    val lastSyncEpochMs: StateFlow<Long> = _lastSyncEpochMs.asStateFlow()

    fun observeEvents(location: LocationFilter, dateRange: DateRangeFilter): Flow<List<Event>> {
        val range = dateRange.resolve()
        val from = range.start.toEpochDay()
        val to = range.endInclusive.toEpochDay()

        val entities = when (location) {
            is LocationFilter.IslandTag ->
                dao.byIslands(listOf(location.island.name), from, to)

            is LocationFilter.Bounds -> {
                // v1 dataset is island-tagged but rarely has raw coordinates,
                // so a bounding box matches any island whose box intersects
                // it — the island centroid stands in for missing lat/lng.
                val islands = BahamianIsland.entries
                    .filter { it.bounds.intersects(location.box) }
                    .map { it.name }
                if (islands.isNotEmpty()) dao.byIslands(islands, from, to)
                else dao.byBounds(
                    location.box.minLat, location.box.maxLat,
                    location.box.minLng, location.box.maxLng, from, to)
            }

            LocationFilter.Everywhere -> dao.allBetween(from, to)
        }

        return combine(entities, dao.favoriteIds()) { list, favIds ->
            val favs = favIds.toSet()
            list.map { it.toDomain(isSaved = it.id in favs) }
        }
    }

    fun observeEvent(id: String): Flow<Event?> =
        combine(dao.byId(id), dao.favoriteIds()) { entity, favIds ->
            entity?.toDomain(isSaved = entity.id in favIds.toSet())
        }

    fun observeFavorites(): Flow<List<Event>> =
        dao.favorites().map { list -> list.map { it.toDomain(isSaved = true) } }

    suspend fun toggleFavorite(event: Event) {
        if (event.isSaved) dao.removeFavorite(event.id)
        else dao.addFavorite(FavoriteEntity(event.id))
    }

    /** Pull the feed and atomically replace the cache. Throws on network
     *  failure — callers decide whether that is a toast or a silent retry. */
    suspend fun refresh() {
        val feed = api.fetchFeed()
        val entities = feed.events.mapNotNull { it.toEntity() }
        if (entities.isNotEmpty()) {
            dao.replaceFeed(entities)
            val now = System.currentTimeMillis()
            prefs.edit { putLong(KEY_LAST_SYNC, now) }
            _lastSyncEpochMs.value = now
        }
    }

    private companion object {
        const val KEY_LAST_SYNC = "last_sync_epoch_ms"
    }
}
