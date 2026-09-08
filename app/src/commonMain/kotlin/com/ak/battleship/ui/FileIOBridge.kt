package com.ak.battleship.ui

import androidx.compose.runtime.Composable
import com.ak.battleship.viewmodel.BattleshipViewModel

@Composable
expect fun rememberCsvExporter(viewModel: BattleshipViewModel, context: Any?): () -> Unit

@Composable
expect fun rememberCsvImporter(viewModel: BattleshipViewModel, context: Any?): () -> Unit
