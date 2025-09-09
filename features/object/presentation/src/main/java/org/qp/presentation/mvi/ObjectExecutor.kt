package org.qp.presentation.mvi

import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.qp.presentation.mvi.ObjectStore.Message.UpdateObjects
import org.qp.settings.SettingsRepo
import org.qp.supervisor.SupervisorService

class ObjectExecutor(
    private val service: SupervisorService
) : CoroutineExecutor<ObjectStore.Intent, ObjectStore.Action, ObjectStore.State, ObjectStore.Message, ObjectStore.Label>() {
    private val outScope = CoroutineScope(Dispatchers.Default)

    override fun executeIntent(intent: ObjectStore.Intent) {
        when (intent) {
            is ObjectStore.Intent.ObjectSelected -> {
                service.onObjectSelected(intent.index)
            }
        }
    }

    override fun executeAction(action: ObjectStore.Action) {
        when (action) {
            is ObjectStore.Action.StartObjectsFlow -> {
                outScope.launch {
                    service.gameStateFlow.collect { state ->
                        scope.launch { dispatch(UpdateObjects(state.objectsList)) }
                    }
                }
            }
        }
    }

    override fun dispose() {
        super.dispose()
        outScope.cancel()
    }
}