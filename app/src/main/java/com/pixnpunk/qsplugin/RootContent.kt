package com.pixnpunk.qsplugin

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.pixnpunk.dialogs.presentation.DialogsMainContent
import com.pixnpunk.extra.presentation.ExtraContent
import com.pixnpunk.main.presentation.MainContent
import com.pixnpunk.`object`.presentation.ObjectContent
import com.pixnpunk.qsplugin.theme.QSPluginTheme

@Composable
fun RootContent(
    isDarkTheme: Boolean,
    component: RootComponent,
    onFinish: () -> Unit
) {
    val stack by component.childStack.subscribeAsState()
    val dialogSlot by component.dialogSlot.subscribeAsState()

    val activeComponent = stack.active.instance

    QSPluginTheme(
        darkTheme = isDarkTheme
    ) {
        dialogSlot.child?.instance?.also {
            DialogsMainContent(it)
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                RootAppBar(
                    component = component,
                    activeComponent = activeComponent,
                    onFinishActivity = onFinish
                )
            },
            bottomBar = { RootNavBar(component, activeComponent) }
        ) { paddingValues ->
            Children(
                modifier = Modifier.padding(paddingValues),
                stack = component.childStack
            ) { child ->
                when (val instance = child.instance) {
                    is RootComponent.Child.MainChild -> {
                        MainContent(instance.component)
                    }

                    is RootComponent.Child.ExtraChild -> {
                        ExtraContent(instance.component)
                    }

                    is RootComponent.Child.ObjectChild -> {
                        ObjectContent(instance.component)
                    }
                }
            }
        }
    }
}