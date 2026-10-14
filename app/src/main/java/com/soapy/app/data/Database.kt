package com.soapy.app.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/**
 * A word or website the user wants blocked. Stored on the device only, never uploaded.
 * Entries are permanent: the DAO has no delete on purpose.
 */
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
}

/** The time a block happened. Nothing else: no word, no site, no search text. */
@Entity(tableName = "block_events")
data class BlockEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "happened_at") val happenedAt: Long
)

/** The time the user pressed "I slipped". History is permanent, like the blocklist. */
@Entity(tableName = "slips")
data class Slip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "happened_at") val happenedAt: Long
)

@Dao
interface ProgressDao {
    @Insert
    suspend fun insertBlock(event: BlockEvent)

    @Insert
    suspend fun insertSlip(slip: Slip)

    @Query("SELECT COUNT(*) FROM block_events")
    fun observeBlockCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM block_events WHERE happened_at >= :since")
    fun observeBlocksSince(since: Long): Flow<Int>

    @Query("SELECT happened_at FROM slips ORDER BY happened_at ASC")
    fun observeSlips(): Flow<List<Long>>
}

@Database(
    entities = [BlockedKeyword::class, BlockEvent::class, Slip::class],
    version = 2,
    exportSchema = false
)
abstract class SoapyDatabase : RoomDatabase() {
    abstract fun keywordDao(): BlockedKeywordDao
    abstract fun progressDao(): ProgressDao

    companion object {
        @Volatile
        private var instance: SoapyDatabase? = null

        fun get(context: Context): SoapyDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SoapyDatabase::class.java,
                    "soapy.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }

        // Existing installs keep their blocklist; only the two new tables are added.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `block_events` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`happened_at` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `slips` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`happened_at` INTEGER NOT NULL)"
                )
            }
        }
    }
}
