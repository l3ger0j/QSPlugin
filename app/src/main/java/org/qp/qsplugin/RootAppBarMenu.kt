package org.qp.qsplugin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun RootAppBarMenu(
    component: RootComponent
) {
    val state by component.model.collectAsState()
    val isNestedLoadExpanded = state.inLoadExpanded
    val isNestedSaveExpanded = state.isSaveExpanded

//    val MAX_SAVE_SLOTS = 5

    Row {
        Box {
            IconButton(onClick = component::doCreateSaveIntent) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = ""
                )
            }
//            DropdownMenu(
//                expanded = isNestedSaveExpanded,
//                onDismissRequest = { component.setStateNestedSave(false) }
//            ) {
//                state.listStateSlots.forEach {
//                    DropdownMenuItem(text = { Text(text = "Slot ${it + 1} [Empty]") }, onClick = { })
//                }
//                DropdownMenuItem(
//                    text = { Text("To File…") },
//                    onClick = { }
//                )
//            }
        }
        Box {
            IconButton(onClick = component::doCreateLoadIntent) {
                Icon(
                    imageVector = Icons.Default.Upload,
                    contentDescription = ""
                )
            }
//            DropdownMenu(
//                expanded = isNestedLoadExpanded,
//                onDismissRequest = { component.setStateNestedLoad(false) }
//            ) {
//                if (state.listStateSlots.isNotEmpty()) {
//                    state.listStateSlots.filterNot { s -> s.contains("[Empty]") }.forEach {
//                        DropdownMenuItem(text = { Text(text = "Slot ${it + 1}") }, onClick = { })
//                    }
//                }
//                DropdownMenuItem(
//                    text = { Text("From File…") },
//                    onClick = { }
//                )
//            }
        }
    }
}