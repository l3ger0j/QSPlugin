package com.pixnpunk.qsplugin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootAppBar(
    component: RootComponent,
    activeComponent: RootComponent.Child,
    onFinishActivity: () -> Unit
) {
    val state by component.model.collectAsState()

    TopAppBar(
        title = {
            Text(
                when (activeComponent) {
                    is RootComponent.Child.MainChild -> "Main"
                    is RootComponent.Child.ExtraChild -> "Extra"
                    is RootComponent.Child.ObjectChild -> "Object"
                    is RootComponent.Child.InputChild -> "Input"
                }
            )
        },
        navigationIcon = {
            IconButton(onClick = onFinishActivity) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = ""
                )
            }
        },
        actions = {
            IconButton(onClick = { component.restartGame() }) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = ""
                )
            }
            IconButton(onClick = { component.navigation(RealRootComponent.ChildConfig.Input) }) {
                Icon(
                    imageVector = Icons.Filled.Terminal,
                    contentDescription = ""
                )
            }
            if (state.isGameRunning) {
                RootAppBarMenu(component)
            }
        }
    )
}