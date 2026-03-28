package com.turbofan3360.openeq.appdata

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update

// Defining the actual database
@Database(entities = [Preset::class], version = 1, exportSchema = true)
abstract class EqPresetDatabase : RoomDatabase() {
    abstract fun userDao(): PresetDao
}

// Defining what a row in the preset database looks like
@Entity
data class Preset(
    @PrimaryKey val presetId: String,
    // Stores the JSON serialized EQ levels list
    @ColumnInfo(name = "eq_levels") val eqLevels: String
)

// Defining the functions to interact with the database
@Dao
interface PresetDao {
    // Lets you select a certain preset from the database
    @Query("SELECT * FROM preset WHERE presetId = :wantedPresetId")
    suspend fun getPreset(wantedPresetId: String): Preset?

    // Lets you grab all the preset ID strings
    @Query("SELECT presetId FROM Preset")
    suspend fun getPresetIds(): List<String>

    // Lets you add a preset to the database
    @Insert
    suspend fun addPreset(preset: Preset)

    // Lets you update a preset in the database
    @Update
    suspend fun updatePreset(preset: Preset)

    // Lets you remove preset from the database
    @Query("DELETE FROM Preset WHERE presetId = :wantedPresetId")
    suspend fun deletePreset(wantedPresetId: String)
}
