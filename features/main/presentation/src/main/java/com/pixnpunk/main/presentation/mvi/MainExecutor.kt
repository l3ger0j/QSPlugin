package com.pixnpunk.main.presentation.mvi

import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.pixnpunk.natives.SupervisorViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import com.pixnpunk.dto.LibTypeWindow
import com.pixnpunk.main.presentation.mvi.MainStore.Message.*
import com.pixnpunk.settings.SettingsRepo
import com.pixnpunk.utils.HtmlUtil.appendPageTemplate
import com.pixnpunk.utils.HtmlUtil.getCleanHtmlAndMedia
import com.pixnpunk.utils.HtmlUtil.getCleanHtmlRemMedia

class MainExecutor(
    private val service: SupervisorViewModel,
    private val settingsRepo: SettingsRepo
) : CoroutineExecutor<MainStore.Intent, MainStore.Action, MainStore.State, MainStore.Message, MainStore.Label>() {
    private val outScope = CoroutineScope(Dispatchers.Default)

    override fun executeIntent(intent: MainStore.Intent) {
        when (intent) {
            is MainStore.Intent.ActionClick -> {
                service.onActionClicked(intent.index)
            }
        }
    }

    override fun executeAction(action: MainStore.Action) {
        when (action) {
            is MainStore.Action.StartActionsVisFlow -> {
                outScope.launch {
                    service.gameElementVis
                        .filter { it -> it.first == LibTypeWindow.ACTS }
                        .collect { vis ->
                            scope.launch { dispatch(UpdateVisActions(vis.second)) }
                        }
                }
            }

            is MainStore.Action.StartContentFlow -> {
                outScope.launch {
                    service.gameStateFlow
                        .combine(settingsRepo.settingsState) { state, settings -> state to settings }
                        .collect { (state, settings) ->
                            scope.launch {
                                dispatch(
                                    UpdateMainDesc(
                                        appendPageTemplate(
                                            settings,
                                            state.mainDesc
                                        ).let { s ->
                                            if (settings.isImageDisabled) {
                                                s.getCleanHtmlRemMedia()
                                            } else {
                                                s.getCleanHtmlAndMedia(settings)
                                            }
                                        }
                                    )
                                )
                                dispatch(UpdateActions(state.actionsList))
                            }
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