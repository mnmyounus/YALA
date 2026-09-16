package com.mnmyounus.yala.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "intruder_shots")
data class IntruderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val packageName: String,
    val timestampMillis: Long,
    val outcome: String
)

@Dao
interface IntruderDao {
    @Insert
    suspend fun insert(entity: IntruderEntity): Long

    @Query("SELECT * FROM intruder_shots WHERE outcome = :outcome ORDER BY timestampMillis DESC")
    fun observeByOutcome(outcome: String): Flow<List<IntruderEntity>>

    @Query("SELECT * FROM intruder_shots WHERE outcome = :outcome")
    suspend fun allByOutcome(outcome: String): List<IntruderEntity>

    @Query("SELECT * FROM intruder_shots WHERE id = :id")
    suspend fun byId(id: Long): IntruderEntity?

    @Query("DELETE FROM intruder_shots WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM intruder_shots WHERE outcome = :outcome")
    suspend fun deleteByOutcome(outcome: String)
}

@Database(entities = [IntruderEntity::class], version = 1, exportSchema = false)
abstract class YalaDatabase : RoomDatabase() {
    abstract fun intruderDao(): IntruderDao
}
