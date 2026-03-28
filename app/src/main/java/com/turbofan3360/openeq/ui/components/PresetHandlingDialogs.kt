package com.turbofan3360.openeq.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.turbofan3360.openeq.R
import com.turbofan3360.openeq.appdata.RoomDatabaseHandler
import com.turbofan3360.openeq.ui.screens.SmallSecondaryText

@Composable
fun SavePresetDialog(
    onPresetSave: (String) -> Unit,
    onTerminate: () -> Unit
) {
    // Handles a dialog box to save a new preset
    var dialogOpen by remember { mutableStateOf(true) }
    val textFieldState = rememberTextFieldState("")

    DialogStructure(
        showDialog = dialogOpen,
        onConfirm = { onPresetSave(textFieldState.text.toString()) },
        onDismiss = {
            dialogOpen = false
            onTerminate()
        },
        iconImageVector = Icons.Rounded.Save,
        iconDescription = stringResource(R.string.save_preset_dialog_icon_description),
        title = stringResource(R.string.save_preset_dialog_title),
        bodyText = stringResource(R.string.save_preset_dialog_text)
    ) {
        // Main content shown in the dialog
        PresetTextField(
            textFieldState,
            stringResource(R.string.save_preset_dialog_input_box_label)
        )
    }
}

@Composable
fun LoadPresetDialog(
    onPresetLoad: (String) -> Unit,
    onTerminate: () -> Unit
) {
    // Handles the dialog box to load a preset value
    var dialogOpen by remember { mutableStateOf(true) }
    var selectedPreset by remember { mutableStateOf("") }

    DialogStructure(
        showDialog = dialogOpen,
        onConfirm = { onPresetLoad(selectedPreset) },
        onDismiss = {
            dialogOpen = false
            onTerminate()
        },
        iconImageVector = Icons.Rounded.Cached,
        iconDescription = stringResource(R.string.load_preset_dialog_icon_description),
        title = stringResource(R.string.load_preset_dialog_title),
        bodyText = stringResource(R.string.load_preset_dialog_text)
    ) {
        // Main content shown in the dialog
        PresetIdsDropDown(
            RoomDatabaseHandler.idStrings
        ) {
            selectedPreset = it
        }
    }
}

@Composable
fun UpdatePresetDialog(
    onPresetUpdate: (String) -> Unit,
    onTerminate: () -> Unit
) {
    // Handles the dialog to update a preset to the current EQ values
    var dialogOpen by remember { mutableStateOf(true) }
    var selectedPreset by remember { mutableStateOf("") }

    DialogStructure(
        showDialog = dialogOpen,
        onConfirm = { onPresetUpdate(selectedPreset) },
        onDismiss = {
            dialogOpen = false
            onTerminate()
        },
        iconImageVector = Icons.Rounded.Edit,
        iconDescription = stringResource(R.string.update_preset_dialog_icon_description),
        title = stringResource(R.string.update_preset_dialog_title),
        bodyText = stringResource(R.string.update_preset_dialog_text)
    ) {
        // Main content shown in the dialog
        PresetIdsDropDown(
            RoomDatabaseHandler.idStrings
        ) {
            selectedPreset = it
        }
    }
}

@Composable
fun DeletePresetDialog(
    onPresetDelete: (String) -> Unit,
    onTerminate: () -> Unit
) {
    // Handles a dialog box to delete a preset
    var dialogOpen by remember { mutableStateOf(true) }
    var selectedPreset by remember { mutableStateOf("") }

    DialogStructure(
        showDialog = dialogOpen,
        onConfirm = { onPresetDelete(selectedPreset) },
        onDismiss = {
            dialogOpen = false
            onTerminate()
        },
        iconImageVector = Icons.Rounded.Delete,
        iconDescription = stringResource(R.string.delete_preset_dialog_icon_description),
        title = stringResource(R.string.delete_preset_dialog_title),
        bodyText = stringResource(R.string.delete_preset_dialog_text)
    ) {
        // Main content shown in the dialog
        PresetIdsDropDown(
            RoomDatabaseHandler.idStrings
        ) {
            selectedPreset = it
        }
    }
}

@Composable
private fun DialogStructure(
    showDialog: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    iconImageVector: ImageVector,
    iconDescription: String,
    title: String,
    bodyText: String,
    dialogContent: @Composable () -> Unit
) {
    // A pop-up dialog base structure used for user preset input
    if (showDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            iconContentColor = MaterialTheme.colorScheme.secondary,
            titleContentColor = MaterialTheme.colorScheme.tertiary,
            textContentColor = MaterialTheme.colorScheme.tertiary,

            icon = {
                Icon(
                    imageVector = iconImageVector,
                    contentDescription = iconDescription
                )
            },
            title = {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            },
            onDismissRequest = { onDismiss() },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    }
                ) { SmallSecondaryText(stringResource(R.string.dialog_confirm)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                    }
                ) { SmallSecondaryText(stringResource(R.string.dialog_dismiss)) }
            },
            text = {
                Column {
                    // Main body text of the dialog if extra detail required
                    Text(
                        text = bodyText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Whatever composable content you want to put here
                    dialogContent()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetIdsDropDown(
    presetIds: List<String>,
    onSelect: (String) -> Unit
) {
    var dropdownOpen by remember { mutableStateOf(false) }
    val textFieldState = rememberTextFieldState(stringResource(R.string.preset_dropdown_field_default))

    // Dropdown box to select a preset you want to do something to
    ExposedDropdownMenuBox(
        expanded = dropdownOpen,
        onExpandedChange = { dropdownOpen = it }
    ) {
        // Text field where the selected preset is shown (not user-editable)
        OutlinedTextField(
            readOnly = true,
            state = textFieldState,
            label = { stringResource(R.string.preset_dropdown_field_label) },
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),

            colors = TextFieldDefaults.colors(
                unfocusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedTextColor = MaterialTheme.colorScheme.tertiary
            )
        )
        // Defining the actual drop-down
        ExposedDropdownMenu(
            expanded = dropdownOpen,
            onDismissRequest = { dropdownOpen = false }
        ) {
            presetIds.forEach { id ->
                // Ignoring string that is DB key don't want appearing in UI
                if (id != stringResource(R.string.db_key_recent_eq_levels)) {
                    DropdownMenuItem(
                        onClick = {
                            textFieldState.setTextAndPlaceCursorAtEnd(id)
                            onSelect(id)
                            dropdownOpen = false
                        },
                        text = { SmallSecondaryText(id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetTextField(
    boxState: TextFieldState,
    boxLabel: String
) {
    // Text field composable to let the user input a value
    OutlinedTextField(
        state = boxState,
        label = { Text(boxLabel) },
        textStyle = MaterialTheme.typography.bodySmall,

        colors = TextFieldDefaults.colors(
            unfocusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            unfocusedTextColor = MaterialTheme.colorScheme.tertiary
        )
    )
}
