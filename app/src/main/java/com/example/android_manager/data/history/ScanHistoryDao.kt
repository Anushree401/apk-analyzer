package com.example.android_manager.data.history

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for scan history database operations.
 */
@Dao
interface ScanHistoryDao {

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    suspend fun getAllSnapshot(): List<ScanHistoryEntity>

    @Query("SELECT * FROM scan_history WHERE id = :id")
    suspend fun getById(id: Long): ScanHistoryEntity?

    @Query("SELECT * FROM scan_history WHERE riskLevel = :riskLevel ORDER BY timestamp DESC")
    fun getByRiskLevel(riskLevel: String): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE targetName LIKE '%' || :query || '%' OR packageName LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ScanHistoryEntity): Long

    @Delete
    suspend fun delete(entry: ScanHistoryEntity): Int

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM scan_history")
    suspend fun clearAll(): Int

    @Query("SELECT COUNT(*) FROM scan_history")
    fun getCount(): Flow<Int>
}
