package com.pixnpunk.qsplugin

import android.os.Process.killProcess
import android.os.Process.myPid
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.pixnpunk.dialogs.presentation.DialogsMainContent
import com.pixnpunk.extra.presentation.ExtraContent
import com.pixnpunk.input.presentation.InputContent
import com.pixnpunk.main.presentation.MainContent
import com.pixnpunk.`object`.presentation.ObjectContent
import com.pixnpunk.qsplugin.mvi.RootStore
import com.pixnpunk.qsplugin.theme.QSPluginTheme

@Composable
fun RootContent(
    component: RootComponent
) {
    val stack by component.childStack.subscribeAsState()
    val dialogSlot by component.dialogSlot.subscribeAsState()

    val activeComponent = stack.active.instance

    QSPluginTheme {
        dialogSlot.child?.instance?.also {
            DialogsMainContent(it)
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                RootAppBar(
                    component = component,
                    activeComponent = activeComponent,
                    onFinishActivity = {
                        killProcess(myPid())
                    }
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

                    is RootComponent.Child.InputChild -> {
                        InputContent(instance.component)
                    }
                }
            }
        }
    }
}