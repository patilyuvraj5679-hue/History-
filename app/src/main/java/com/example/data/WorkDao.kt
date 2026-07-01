package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDao {
    @Query("SELECT * FROM work_records ORDER BY date DESC, timestamp DESC")
    fun getAllWorkRecords(): Flow<List<WorkRecord>>

    @Query("SELECT * FROM work_records WHERE clientName = :clientName ORDER BY date DESC")
    fun getWorkRecordsForClient(clientName: String): Flow<List<WorkRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkRecord(record: WorkRecord)

    @Delete
    suspend fun deleteWorkRecord(record: WorkRecord)

    @Query("SELECT DISTINCT clientName FROM work_records ORDER BY clientName ASC")
    fun getUniqueClients(): Flow<List<String>>

    @Query("SELECT * FROM work_records WHERE id = :id LIMIT 1")
    suspend fun getWorkRecordById(id: Int): WorkRecord?
}
