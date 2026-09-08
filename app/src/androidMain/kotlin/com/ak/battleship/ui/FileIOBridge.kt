package com.ak.battleship.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.ak.battleship.viewmodel.BattleshipViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
actual fun rememberCsvExporter(viewModel: BattleshipViewModel, context: Any?): () -> Unit {
    val ctx = context as? Context
    val scope = rememberCoroutineScope()
    val exportDateFormatter = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val csvData = viewModel.generateAllGamesCsv()
                    ctx?.contentResolver?.openOutputStream(it)?.use { stream -> stream.write(csvData.toByteArray()) }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }
    return { saveLauncher.launch("Battleship_Export_${exportDateFormatter.format(Date())}.csv") }
}

@Composable
actual fun rememberCsvImporter(viewModel: BattleshipViewModel, context: Any?): () -> Unit {
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.handleSmartImport(context, it) }
    }
    return { importLauncher.launch("*/*") }
}
