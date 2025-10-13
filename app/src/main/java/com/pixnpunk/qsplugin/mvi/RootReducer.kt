package com.pixnpunk.qsplugin.mvi

import com.arkivanov.mvikotlin.core.store.Reducer

object RootReducer : Reducer<RootStore.State, RootStore.Message> {
    override fun RootStore.State.reduce(msg: RootStore.Message): RootStore.State {
        return when (msg) {
            is RootStore.Message.UpdateGameStatus -> copy(isGameRunning = msg.isGameRunning)
            is RootStore.Message.UpdateVisExtraElement -> copy(isExtraEnabled = msg.isExtraEnabled)
            is RootStore.Message.UpdateVisInputElement -> copy(isInputEnabled = msg.isInputEnabled)
            is RootStore.Message.UpdateVisObjElement -> copy(isObjectsEnabled = msg.isObjectsEnabled)
            is RootStore.Message.UpdateStateNestedLoad -> copy(inLoadExpanded = msg.newState)
            is RootStore.Message.UpdateStateNestedSave -> copy(isSaveExpanded = msg.newState)
        }
    }
}