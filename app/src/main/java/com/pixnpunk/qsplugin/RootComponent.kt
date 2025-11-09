package com.pixnpunk.qsplugin

import android.net.Uri
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.pixnpunk.dialogs.presentation.DialogState
import com.pixnpunk.dialogs.presentation.DialogsComponent
import com.pixnpunk.dto.LibGenItem
import com.pixnpunk.extra.presentation.ExtraComponent
import com.pixnpunk.main.presentation.MainComponent
import com.pixnpunk.`object`.presentation.ObjectComponent
import com.pixnpunk.qsplugin.RealRootComponent.ChildConfig
import com.pixnpunk.qsplugin.mvi.RootStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed interface RootComponent {
    val childStack: Value<ChildStack<*, Child>>
    val dialogSlot: Value<ChildSlot<*, DialogsComponent>>
    val model: StateFlow<RootStore.State>
    val label: Flow<RootStore.Label>

    fun navigation(child: ChildConfig)

    fun runGame(
        gameId: Long,
        gameTitle: String,
        gameDirUri: Uri,
        gameFileUri: Uri
    )

    fun doShowReqPermDialog()
    fun doCreateSaveIntent()
    fun doCreateLoadIntent()
    fun restartGame()

    fun doShowDefaultDialog(inputString: String, dialogState: DialogState)
    fun doShowDialogMessage(inputString: String)
    fun doShowDialogMenu(inputString: String, inputListItems: List<LibGenItem>)
    fun doShowDialogInput()
    fun doShowDialogExecutor()

    sealed class Child {
        class MainChild(val component: MainComponent) : Child()
        class ExtraChild(val component: ExtraComponent) : Child()
        class ObjectChild(val component: ObjectComponent) : Child()
    }
}