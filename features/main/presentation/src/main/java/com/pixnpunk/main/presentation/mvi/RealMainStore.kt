package com.pixnpunk.main.presentation.mvi

import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class RealMainStore(
    private val storeFactory: StoreFactory
) : KoinComponent {
    internal fun create(): MainStore = MainImpl()

    private inner class MainImpl : MainStore,
        Store<MainStore.Intent, MainStore.State, MainStore.Label> by storeFactory.create(
            name = "MainStore",
            initialState = MainStore.State(),
            bootstrapper = SimpleBootstrapper(
                MainStore.Action.StartContentFlow,
                MainStore.Action.StartActionsVisFlow
            ),
            executorFactory = { MainExecutor(get(), get()) },
            reducer = MainReducer
        )
}