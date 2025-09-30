package org.qp.qsplugin

import android.net.Uri
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.qp.dialogs.presentation.DialogState
import org.qp.dialogs.presentation.DialogsComponent
import org.qp.dto.LibGenItem
import org.qp.extra.presentation.ExtraComponent
import org.qp.main.presentation.MainComponent
import org.qp.presentation.InputComponent
import org.qp.presentation.ObjectComponent
import org.qp.qsplugin.RealRootComponent.ChildConfig
import org.qp.qsplugin.mvi.RootStore
import org.qp.supervisor.SupervisorService

sealed interface RootComponent {
    val childStack: Value<ChildStack<*, Child>>
    val dialogSlot: Value<ChildSlot<*, DialogsComponent>>
    val model: StateFlow<RootStore.State>
    val label: Flow<RootStore.Label>

    fun navigation(child: ChildConfig)

    fun setStateNestedSave(newState: Boolean)
    fun setStateNestedLoad(newState: Boolean)

    fun runGame(
        gameId: Long,
        gameTitle: String,
        gameDirUri: Uri,
        gameFileUri: Uri
    )
    fun doCreateSaveIntent()
    fun doCreateLoadIntent()
    fun restartGame()

    fun doShowDefaultDialog(inputString: String, dialogState: DialogState)
    fun doShowDialogMessage(inputString: String)
    fun doShowDialogMenu(inputString: String, inputListItems: List<LibGenItem>)

    sealed class Child {
        class MainChild(val component: MainComponent) : Child()
        class ExtraChild(val component: ExtraComponent) : Child()
        class ObjectChild(val component: ObjectComponent) : Child()
        class InputChild(val component: InputComponent) : Child()
    }
}