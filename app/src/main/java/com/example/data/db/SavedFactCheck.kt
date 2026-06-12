package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_fact_checks")
data class SavedFactCheck(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val claim: String,
    val verdict: String,         // True, False, Misleading, Partially True, Unverified
    val confidence: Int,         // 0 to 100
    val analysis: String,        // In-depth detail
    val context: String,         // Extra context or background
    val references: String,      // Text/JSON of verified sources
    val timestamp: Long = System.currentTimeMillis()
)
