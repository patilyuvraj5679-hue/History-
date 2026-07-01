package com.example.data

import kotlinx.coroutines.flow.Flow

class WorkRepository(private val workDao: WorkDao) {
    val allWorkRecords: Flow<List<WorkRecord>> = workDao.getAllWorkRecords()
    val uniqueClients: Flow<List<String>> = workDao.getUniqueClients()

    suspend fun insert(record: WorkRecord) {
        workDao.insertWorkRecord(record)
    }

    suspend fun delete(record: WorkRecord) {
        workDao.deleteWorkRecord(record)
    }

    suspend fun getWorkRecordById(id: Int): WorkRecord? {
        return workDao.getWorkRecordById(id)
    }

    fun getWorkRecordsForClient(clientName: String): Flow<List<WorkRecord>> {
        return workDao.getWorkRecordsForClient(clientName)
    }
}
