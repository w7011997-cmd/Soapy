package com.soapy.app.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** A word the user wants blocked. Stored on the device only, never uploaded. */
@Entity(
    tableName = "blocked_keywords",
    indices = [Index(value = ["term"], unique = true)]
)
data class BlockedKeyword(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val term: String,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis()
)

@Dao
interface BlockedKeywordDao {
    @Query("SELECT * FROM blocked_keywords ORDER BY added_at DESC")
    fun observeAll(): Flow<List<BlockedKeyword>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: BlockedKeyword): Long

    @Delete
    suspend fun delete(item: BlockedKeyword)
}

@Database(entities = [BlockedKeyword::class], version = 1, exportSchema = false)
abstract class SoapyDatabase : RoomDatabase() {
    abstract fun keywordDao(): BlockedKeywordDao

    companion object {
        @Volatile
        private var instance: SoapyDatabase? = null

        fun get(context: Context): SoapyDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SoapyDatabase::class.java,
                    "soapy.db"
                ).build().also { instance = it }
            }
    }
}
