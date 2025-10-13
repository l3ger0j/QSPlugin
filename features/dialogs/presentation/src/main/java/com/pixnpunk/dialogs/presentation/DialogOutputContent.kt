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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp

@Composable
fun DialogOutputContent(
    component: DialogsComponent
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AnnotatedString.Companion.fromHtml(component.dialogConfig.dialogInputString)
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(
            onClick = component.onDismissed,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Confirm")
        }
    }
}
