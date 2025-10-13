package com.pixnpunk.qsplugin.mvi

import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class RealRootStore(
    private val storeFactory: StoreFactory
) : KoinComponent {
    internal fun create(): RootStore = RootImpl()

    private inner class RootImpl : RootStore,
        Store<RootStore.Intent, RootStore.State, RootStore.Label> by storeFactory.create(
            name = "RootStore",
            initialState = RootStore.State(),
            bootstrapper = SimpleBootstrapper(
                RootStore.Action.StartGameStateFlow,
                RootStore.Action.StartGamePopupFlow,
                RootStore.Action.StartGameElementVisFlow
            ),
            executorFactory = { RootExecutor(get(), get()) },
            reducer = RootReducer
        )
}