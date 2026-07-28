package bs.wahgwaan.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    // ── The two query shapes LocationFilter resolves to ────────────────────

    @Query(
        """SELECT * FROM events
           WHERE islandTag IN (:islandTags)
             AND epochDay BETWEEN :fromDay AND :toDay
           ORDER BY epochDay, timeStart""")
    fun byIslands(islandTags: List<String>, fromDay: Long, toDay: Long): Flow<List<EventEntity>>

    @Query(
        """SELECT * FROM events
           WHERE lat BETWEEN :minLat AND :maxLat
             AND lng BETWEEN :minLng AND :maxLng
             AND epochDay BETWEEN :fromDay AND :toDay
           ORDER BY epochDay, timeStart""")
    fun byBounds(
        minLat: Double, maxLat: Double, minLng: Double, maxLng: Double,
        fromDay: Long, toDay: Long,
    ): Flow<List<EventEntity>>

    @Query(
        """SELECT * FROM events
           WHERE epochDay BETWEEN :fromDay AND :toDay
           ORDER BY epochDay, timeStart""")
    fun allBetween(fromDay: Long, toDay: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    fun byId(id: String): Flow<EventEntity?>

    // ── Favorites ───────────────────────────────────────────────────────────

    @Query(
        """SELECT e.* FROM events e
           INNER JOIN favorites f ON e.id = f.eventId
           ORDER BY e.epochDay, e.timeStart""")
    fun favorites(): Flow<List<EventEntity>>

    @Query("SELECT eventId FROM favorites")
    fun favoriteIds(): Flow<List<String>>

    @Query(
        """SELECT e.* FROM events e
           INNER JOIN favorites f ON e.id = f.eventId
           WHERE e.epochDay = :day""")
    suspend fun favoritesOnDay(day: Long): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE eventId = :eventId")
    suspend fun removeFavorite(eventId: String)

    // ── Sync: full-feed replace that never clobbers favorites ───────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<EventEntity>)

    @Query("DELETE FROM events WHERE id NOT IN (:keepIds)")
    suspend fun deleteAllExcept(keepIds: List<String>)

    @Transaction
    suspend fun replaceFeed(events: List<EventEntity>) {
        upsertAll(events)
        deleteAllExcept(events.map { it.id })
    }
}
