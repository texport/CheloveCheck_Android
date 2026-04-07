package com.chelovecheck.presentation.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import com.chelovecheck.R
import com.chelovecheck.domain.repository.SaveManyResult
import com.chelovecheck.presentation.components.M3MaxWidthColumn
import com.chelovecheck.presentation.viewmodel.SettingsUiEvent
import com.chelovecheck.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataBackupScreen(
    viewModel: SettingsViewModel,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var infoDialogMessage by remember { mutableStateOf<String?>(null) }
    var duplicateImportResult by remember { mutableStateOf<SaveManyResult?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsUiEvent.ExportSucceeded -> {
                    infoDialogMessage = context.getString(R.string.settings_export_success)
                }
                SettingsUiEvent.ExportFailed -> {
                    infoDialogMessage = context.getString(R.string.settings_export_error)
                }
                is SettingsUiEvent.ImportFinished -> {
                    infoDialogMessage = context.getString(
                        R.string.settings_import_result,
                        event.result.imported.size,
                        event.result.skipped.size,
                    )
                }
                is SettingsUiEvent.ImportDuplicatesPending -> {
                    duplicateImportResult = event.result
                }
                SettingsUiEvent.ImportFailed -> {
                    infoDialogMessage = context.getString(R.string.settings_import_error)
                }
                SettingsUiEvent.DeleteAllFinished -> {
                    infoDialogMessage = context.getString(R.string.settings_delete_all_done)
                }
                SettingsUiEvent.ExchangeRatesUpdated,
                SettingsUiEvent.ExchangeRatesUpdateFailed,
                -> Unit
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = viewModel.exportJson()
                    writeTextToUri(context, uri, json)
                }.onSuccess {
                    viewModel.notifyExportSucceeded()
                }.onFailure {
                    viewModel.notifyExportFailed()
                }
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val csv = viewModel.exportCsv()
                    writeTextToUri(context, uri, csv)
                }.onSuccess {
                    viewModel.notifyExportSucceeded()
                }.onFailure {
                    viewModel.notifyExportFailed()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { readTextFromUri(context, uri) }
                    .onSuccess { json -> viewModel.importJson(json) }
                    .onFailure { viewModel.notifyImportFailed() }
            }
        }
    }

    val dup = duplicateImportResult
    if (dup != null) {
        AlertDialog(
            onDismissRequest = {
                duplicateImportResult = null
                infoDialogMessage = context.getString(
                    R.string.settings_import_result,
                    dup.imported.size,
                    dup.skipped.size,
                )
            },
            title = { Text(stringResource(R.string.import_duplicates_title)) },
            text = { Text(stringResource(R.string.import_duplicates_body, dup.skipped.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        duplicateImportResult = null
                        viewModel.replaceImportDuplicates(dup.skipped)
                    },
                ) {
                    Text(stringResource(R.string.import_duplicates_replace))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        duplicateImportResult = null
                        infoDialogMessage = context.getString(
                            R.string.settings_import_result,
                            dup.imported.size,
                            dup.skipped.size,
                        )
                    },
                ) {
                    Text(stringResource(R.string.import_duplicates_keep))
                }
            },
        )
    }

    if (infoDialogMessage != null) {
        SettingsInfoDialog(
            message = infoDialogMessage.orEmpty(),
            onDismiss = { infoDialogMessage = null },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_data_backup_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_close))
                    }
                },
            )
        },
    ) { padding ->
        M3MaxWidthColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SettingsReceiptsSection(
                    onExport = { exportLauncher.launch("chelovecheck_receipts.json") },
                    onExportCsv = { exportCsvLauncher.launch("chelovecheck_receipts.csv") },
                    onImport = { importLauncher.launch("application/json") },
                    onDeleteAll = { showDeleteDialog = true },
                )
            }
        }
    }

    if (showDeleteDialog) {
        DeleteAllReceiptsDialog(
            onConfirm = {
                viewModel.deleteAllReceipts()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}
