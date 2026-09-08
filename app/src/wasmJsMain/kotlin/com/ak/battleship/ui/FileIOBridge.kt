package com.ak.battleship.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.ak.battleship.viewmodel.BattleshipViewModel
import kotlinx.browser.window
import kotlinx.coroutines.launch
import com.ak.battleship.PlatformServices

internal fun jsDownloadCsv(content: String, filename: String) {
    js("""
        var blob = new window.Blob([content], { type: 'text/csv' });
        var url = window.URL.createObjectURL(blob);
        var a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
    """)
}

internal fun jsImportCsv(callback: (String) -> Unit) {
    js("""
        var input = document.createElement('input');
        input.type = 'file';
        input.accept = '*/*';
        input.onchange = function(event) {
            var file = event.target.files[0];
            if (file) {
                var reader = new FileReader();
                reader.onload = function(e) {
                    callback(e.target.result);
                };
                reader.readAsText(file);
            }
        };
        input.click();
    """)
}

@Composable
actual fun rememberCsvExporter(viewModel: BattleshipViewModel, context: Any?): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch {
            try {
                val csvData = viewModel.generateAllGamesCsv()
                jsDownloadCsv(csvData, "Battleship_Export.csv")
            } catch (e: Exception) {
                window.alert("Failed to export: ${e.message}")
            }
        }
    }
}

@Composable
actual fun rememberCsvImporter(viewModel: BattleshipViewModel, context: Any?): () -> Unit {
    return {
        try {
            jsImportCsv { fileContent ->
                if (fileContent.trim().startsWith("{") || fileContent.trim().startsWith("[")) {
                    println("Successfully imported AI weights! Size: ${fileContent.length} chars")
                    PlatformServices.showToast(context, "AI Brain Imported!")
                } else if (fileContent.contains("GameID")) {
                    viewModel.processCsvImport(context, fileContent)
                } else {
                    PlatformServices.showToast(context, "Unrecognized file format")
                }
            }
        } catch (e: Exception) {
            window.alert("Failed to import: ${e.message}")
        }
    }
}
