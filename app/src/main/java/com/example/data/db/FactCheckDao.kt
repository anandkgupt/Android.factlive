package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FactCheckDao {
    @Query("SELECT * FROM saved_fact_checks ORDER BY timestamp DESC")
    fun getAllSavedChecks(): Flow<List<SavedFactCheck>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheck(check: SavedFactCheck)

    @Query("DELETE FROM saved_fact_checks WHERE id = :id")
    suspend fun deleteCheckById(id: Int)

    @Query("SELECT * FROM saved_fact_checks WHERE id = :id LIMIT 1")
    suspend fun getCheckById(id: Int): SavedFactCheck?
    
    @Query("SELECT EXISTS(SELECT 1 FROM saved_fact_checks WHERE claim = :claim LIMIT 1)")
    suspend fun isClaimSaved(claim: String): Boolean

    @Query("DELETE FROM saved_fact_checks WHERE claim = :claim")
    suspend fun deleteCheckByClaim(claim: String)
}
