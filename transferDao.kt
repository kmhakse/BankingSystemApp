package com.example.bankingsystem

import androidx.room.*

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers")
    suspend fun getAllTransfers(): List<Transfer>
    @Query("DELETE FROM transfers")
    suspend fun deleteAllTransfers()

    @Insert
    suspend fun insertTransfer(transfer: Transfer): Long
}
