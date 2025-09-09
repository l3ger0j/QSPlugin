package org.qp.main.presentation.mvi

import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.qp.dto.LibTypeWindow
import org.qp.main.presentation.mvi.MainStore.Message.UpdateActions
import org.qp.main.presentation.mvi.MainStore.Message.UpdateMainDesc
import org.qp.main.presentation.mvi.MainStore.Message.UpdateVisActions
import org.qp.settings.SettingsRepo
import org.qp.supervisor.SupervisorService
import org.qp.utils.HtmlUtil.appendPageTemplate
import org.qp.utils.HtmlUtil.getCleanHtmlAndMedia
import org.qp.utils.HtmlUtil.getCleanHtmlRemMedia

class MainExecutor(
    private val service: SupervisorService,
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
                        .filter { it -> it.first == LibTypeWindow.ACTIONS }
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