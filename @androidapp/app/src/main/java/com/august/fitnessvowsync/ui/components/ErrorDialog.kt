package com.august.fitnessvowsync.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ErrorDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
) {
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Unexpected Error") },
            text = {
                Column {
                    Text(errorMessage)
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}
