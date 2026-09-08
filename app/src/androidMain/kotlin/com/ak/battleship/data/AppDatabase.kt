package com.ak.battleship.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * DATA LAYER: Room Database Infrastructure
 * * The core database holder. Manages the connection to the underlying SQLite database
 * and executes version migrations to prevent app crashes when updating the schema.
 */

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        var columnExists = false
        // Kotlin Idiom Upgrade: .use { } automatically closes the cursor when the block finishes
        database.query("PRAGMA table_info(games)").use { cursor ->
            while (cursor.moveToNext()) {
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex != -1 && cursor.getString(nameIndex) == "playerName") {
                    columnExists = true
                    break
                }
            }
        }
        if (!columnExists) {
            database.execSQL("ALTER TABLE games ADD COLUMN playerName TEXT NOT NULL DEFAULT 'Human'")
        }
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE moves ADD COLUMN shotNumber INTEGER NOT NULL DEFAULT 0")
        database.execSQL(
            """
            UPDATE moves 
            SET shotNumber = turnNumber 
            WHERE result NOT IN ('SHIP', 'Carrier', 'Battleship', 'Cruiser', 'Submarine', 'Destroyer')
        """
        )
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE games ADD COLUMN gameMode TEXT NOT NULL DEFAULT 'Unknown'")
        database.execSQL("UPDATE games SET gameMode = 'PassAndPlay' WHERE playerName = 'PassAndPlay'")
        database.execSQL("UPDATE games SET gameMode = 'Companion' WHERE playerName = 'Companion' OR playerName = 'Human'")
        database.execSQL("UPDATE games SET gameMode = 'Bot' WHERE opponentName LIKE '%Bot%'")
        database.execSQL("UPDATE games SET playerName = 'Player 1' WHERE playerName IN ('PassAndPlay', 'Companion', 'Human')")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE games ADD COLUMN botBrainMetadata TEXT")
    }
}

@Database(
    entities = [Game::class, Move::class, OpponentProfile::class],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun battleshipDao(): BattleshipDao
}