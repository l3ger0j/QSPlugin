package com.pixnpunk.qsplugin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable

@Composable
fun RootAppBarMenu(
    component: RootComponent
) {
    Row {
        Box {
            IconButton(onClick = component::doCreateSaveIntent) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = ""
                )
            }
        }
        Box {
            IconButton(onClick = component::doCreateLoadIntent) {
                Icon(
                    imageVector = Icons.Default.Upload,
                    contentDescription = ""
                )
            }
        }
    }
}