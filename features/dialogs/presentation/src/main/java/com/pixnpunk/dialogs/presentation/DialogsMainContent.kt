package com.pixnpunk.dialogs.presentation

import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogsMainContent(
    component: DialogsComponent
) {
    BasicAlertDialog(
        onDismissRequest = component.onDismissed,
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            when (component.dialogConfig.dialogState) {
                DialogState.DIALOG_ERROR, DialogState.DIALOG_MESSAGE -> {
                    DialogOutputContent(component)
                }

                DialogState.DIALOG_PICTURE -> {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(component.dialogConfig.dialogInputString)
                            .build(),
                        contentDescription = "Image"
                    )
                }

                DialogState.DIALOG_INPUT -> {
                    DialogInputContent(component)
                }

                DialogState.DIALOG_MENU -> {
                    DialogMenuContent(component)
                }
            }
        }
    }
}