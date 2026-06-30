package com.ak.battleship

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.ak.battleship.data.AppDatabase
import com.ak.battleship.data.MIGRATION_7_8
import com.ak.battleship.data.MIGRATION_8_9
import com.ak.battleship.data.MIGRATION_9_10
import com.ak.battleship.data.MIGRATION_10_11
import com.ak.battleship.ui.AppNavigation
import com.ak.battleship.viewmodel.BattleshipViewModel
import com.ak.battleship.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize the Room Database
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "battleship_db"
        )
            // THE FIX: Chain ALL migrations so legacy users don't lose their data!
            .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
            .fallbackToDestructiveMigration()
            .build()

        // 2. Inject the DAO into the ViewModel Factory
        val factory = ViewModelFactory(database.battleshipDao(), applicationContext)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 3. Instantiate the ViewModel and pass it to the Router
                    val viewModel: BattleshipViewModel = viewModel(factory = factory)
                    AppNavigation(viewModel)
                }
            }
        }
    }
}