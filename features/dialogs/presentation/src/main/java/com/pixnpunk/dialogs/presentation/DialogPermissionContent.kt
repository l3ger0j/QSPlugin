package com.pixnpunk.dialogs.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DialogPermissionContent(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = """
                In order for its native libraries to function correctly, 
                the add-on application requires permission to access the folder containing the previously selected game.
            """.trimIndent()
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(
            onClick = onConfirm,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Confirm")
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Cancel")
        }
    }
}