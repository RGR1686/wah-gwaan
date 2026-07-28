package bs.wahgwaan.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [EventEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class WahGwaanDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}
