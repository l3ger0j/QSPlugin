package com.pixnpunk.dialogs.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp

@Composable
fun DialogInputContent(
    component: DialogsComponent
) {
    var enterValue by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = enterValue,
            onValueChange = {
                enterValue = it
                if (component.dialogConfig.isDialogInputError) {
                    component.onCleanError()
                }
            },
            singleLine = true,
            isError = component.dialogConfig.isDialogInputError,
            supportingText = {
                if (component.dialogConfig.isDialogInputError) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = AnnotatedString.fromHtml(component.dialogConfig.dialogInputErrorString),
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = AnnotatedString.fromHtml(component.dialogConfig.dialogInputString)
                    )
                }
            }
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(
            onClick = {
                val isBox = component.dialogConfig.isDialogInputBox
                if (component.dialogConfig.dialogState == DialogState.DIALOG_EXECUTOR) {
                    component.onExecValue(enterValue)
                } else {
                    component.onEnterValue(enterValue to isBox)
                }
                component.onDismissed()
            },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Confirm")
        }
    }
}
