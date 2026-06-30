package com.ak.battleship.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Creates a single, generic NoSQL file to hold all AI memory matrices
private val Context.aiDataStore by preferencesDataStore(name = "ai_memory_store")

object AiMemoryDataStore {

    // --- CUSTOM LIGHTWEIGHT SERIALIZATION ---
    private fun serializeMatrix(matrix: Array<FloatArray>): String {
        return matrix.joinToString(";") { row -> row.joinToString(",") }
    }

    private fun deserializeMatrix(data: String): Array<FloatArray> {
        val rows = data.split(";")
        return Array(10) { i ->
            if (i < rows.size && rows[i].isNotBlank()) {
                val floats = rows[i].split(",")
                FloatArray(10) { j -> if (j < floats.size) floats[j].toFloatOrNull() ?: 1.0f else 1.0f }
            } else {
                FloatArray(10) { 1.0f } // Default Laplace weight
            }
        }
    }

    // --- GENERIC DATASTORE OPERATIONS ---

    // Save any AI's matrix using a unique string key (e.g., "Moriarty_Offensive", "MonteCarlo_Tree")
    suspend fun saveMatrix(context: Context, keyName: String, matrix: Array<FloatArray>) {
        val prefsKey = stringPreferencesKey(keyName)
        context.aiDataStore.edit { preferences ->
            preferences[prefsKey] = serializeMatrix(matrix)
        }
    }

    // Load any AI's matrix. Returns a Flow that the ViewModel can collect.
    fun getMatrix(context: Context, keyName: String): Flow<Array<FloatArray>?> {
        val prefsKey = stringPreferencesKey(keyName)
        return context.aiDataStore.data.map { preferences ->
            preferences[prefsKey]?.let { deserializeMatrix(it) }
        }
    }
}