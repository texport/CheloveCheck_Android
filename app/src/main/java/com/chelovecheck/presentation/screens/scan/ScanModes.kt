package com.chelovecheck.presentation.screens.scan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chelovecheck.R
import com.chelovecheck.presentation.config.FeatureFlags
import com.chelovecheck.presentation.theme.AppSpacing
import com.chelovecheck.presentation.viewmodel.ScanUiState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ManualEntryContent(
    state: ScanUiState,
    onTChanged: (String) -> Unit,
    onIChanged: (String) -> Unit,
    onFChanged: (String) -> Unit,
    onSChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val input = state.manualInput
    val hasT = parseManualDateTime(input.t) != null
    val hasI = input.i.trim().length >= 3
    val hasF = input.f.trim().length >= 3
    val sumValid = input.s.isBlank() || input.s.trim().replace(',', '.').toDoubleOrNull() != null
    val formValid = if (FeatureFlags.enhancedScanValidationEnabled) {
        hasT && hasI && hasF && sumValid
    } else {
        true
    }
    val dateTimePattern = stringResource(R.string.date_time_format)
    val displayDateTime = remember(input.t, dateTimePattern) {
        parseManualDateTime(input.t)?.let {
            DateTimeFormatter.ofPattern(dateTimePattern).format(it)
        }.orEmpty()
    }
    val existingDateTime = remember(input.t) { parseManualDateTime(input.t) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = existingDateTime?.atZone(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli(),
    )
    val timePickerState = rememberTimePickerState(
        initialHour = existingDateTime?.hour ?: LocalTime.now().hour,
        initialMinute = existingDateTime?.minute ?: LocalTime.now().minute,
        is24Hour = true,
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val dateMillis = datePickerState.selectedDateMillis
                        if (dateMillis != null) {
                            pendingDate = Instant.ofEpochMilli(dateMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            showDatePicker = false
                            showTimePicker = true
                        } else {
                            showDatePicker = false
                        }
                    },
                ) { Text(stringResource(R.string.action_choose)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val date = pendingDate ?: existingDateTime?.toLocalDate() ?: LocalDate.now()
                        val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        onTChanged(formatManualDateTime(date, time))
                        pendingDate = null
                        showTimePicker = false
                    },
                ) { Text(stringResource(R.string.action_choose)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDate = null
                        showTimePicker = false
                    },
                ) { Text(stringResource(R.string.action_cancel)) }
            },
            text = { TimePicker(state = timePickerState) },
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = AppSpacing.scanContentTop,
                bottom = 16.dp,
            )
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.TextFields,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.scan_manual_title), style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = stringResource(R.string.scan_manual_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 4.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = displayDateTime,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.scan_manual_date)) },
                    placeholder = { Text(stringResource(R.string.scan_manual_date_hint)) },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Outlined.Event, contentDescription = null)
                        }
                    },
                    singleLine = true,
                    isError = FeatureFlags.enhancedScanValidationEnabled && !hasT && input.t.isNotBlank(),
                    supportingText = {
                        if (FeatureFlags.enhancedScanValidationEnabled && !hasT && input.t.isNotBlank()) {
                            Text(stringResource(R.string.scan_manual_date_hint))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { showDatePicker = true },
                )
                OutlinedTextField(
                    value = input.i,
                    onValueChange = onIChanged,
                    label = { Text(stringResource(R.string.scan_manual_ticket)) },
                    placeholder = { Text(stringResource(R.string.scan_manual_ticket_hint)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                    singleLine = true,
                    isError = FeatureFlags.enhancedScanValidationEnabled && !hasI && input.i.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = input.f,
                    onValueChange = onFChanged,
                    label = { Text(stringResource(R.string.scan_manual_reg_number)) },
                    placeholder = { Text(stringResource(R.string.scan_manual_reg_hint)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                    singleLine = true,
                    isError = FeatureFlags.enhancedScanValidationEnabled && !hasF && input.f.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = input.s,
                    onValueChange = onSChanged,
                    label = { Text(stringResource(R.string.scan_manual_sum)) },
                    placeholder = { Text(stringResource(R.string.scan_manual_sum_hint)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { if (formValid && !state.isProcessing) onSubmit() }),
                    singleLine = true,
                    isError = FeatureFlags.enhancedScanValidationEnabled && !sumValid && input.s.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = onSubmit,
                    enabled = !state.isProcessing && formValid,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) { Text(stringResource(R.string.action_find_receipt)) }
            }
        }
    }
}

@Composable
internal fun UrlEntryContent(
    url: String,
    onUrlChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val urlValid = remember(url) {
        val trimmed = url.trim()
        !FeatureFlags.enhancedScanValidationEnabled ||
            trimmed.startsWith("http://") || trimmed.startsWith("https://")
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = AppSpacing.scanContentTop,
                bottom = 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.scan_url_title), style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = stringResource(R.string.scan_url_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChanged,
                    label = { Text(stringResource(R.string.scan_url_input)) },
                    placeholder = { Text(stringResource(R.string.scan_manual_url_hint)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { if (urlValid) onSubmit() }),
                    isError = FeatureFlags.enhancedScanValidationEnabled && url.isNotBlank() && !urlValid,
                    supportingText = {
                        if (FeatureFlags.enhancedScanValidationEnabled && url.isNotBlank() && !urlValid) {
                            Text(stringResource(R.string.scan_manual_url_hint))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = onSubmit,
                    enabled = urlValid,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) { Text(stringResource(R.string.action_find_receipt)) }
            }
        }
    }
}

@Composable
internal fun PhotoScanContent(
    onPickPhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = AppSpacing.scanContentTop,
                bottom = 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.scan_photo_title), style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = stringResource(R.string.scan_photo_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Button(
            onClick = onPickPhoto,
            contentPadding = PaddingValues(vertical = 14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.action_pick_photo)) }
    }
}

@Composable
internal fun InstructionCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.scan_instruction_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = stringResource(R.string.scan_instruction_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun PermissionCard(
    onRequest: () -> Unit,
    isPermanentlyDenied: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.scan_permission_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (isPermanentlyDenied) {
                    stringResource(R.string.error_camera_denied)
                } else {
                    stringResource(R.string.scan_permission_body)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isPermanentlyDenied) {
                Button(onClick = onOpenSettings) {
                    Text(stringResource(R.string.action_open_settings))
                }
            } else {
                Button(onClick = onRequest) {
                    Text(stringResource(R.string.action_allow))
                }
            }
        }
    }
}
