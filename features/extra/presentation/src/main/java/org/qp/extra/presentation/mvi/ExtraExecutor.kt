package org.qp.extra.presentation.mvi

import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.qp.extra.presentation.mvi.ExtraStore.Message.UpdateExtraDesc
import org.qp.settings.SettingsRepo
import org.qp.supervisor.SupervisorService
import org.qp.utils.HtmlUtil.appendPageTemplate
import org.qp.utils.HtmlUtil.getCleanHtmlAndMedia
import org.qp.utils.HtmlUtil.getCleanHtmlRemMedia

class ExtraExecutor(
    private val service: SupervisorService,
    private val settingsRepo: SettingsRepo
) : CoroutineExecutor<ExtraStore.Intent, ExtraStore.Action, ExtraStore.State, ExtraStore.Message, ExtraStore.Label>() {
    private val outScope = CoroutineScope(Dispatchers.Default)

    override fun executeAction(action: ExtraStore.Action) {
        when (action) {
            is ExtraStore.Action.StartExtraReceiveFlow -> {
                outScope.launch {
                    service.gameStateFlow
                        .combine(settingsRepo.settingsState) { state, settings -> state to settings }
                        .collect { (state, settings) ->
                            scope.launch {
                                dispatch(
                                    UpdateExtraDesc(
                                        appendPageTemplate(
                                            settings,
                                            state.varsDesc
                                        ).let { s ->
                                            if (settings.isImageDisabled) {
                                                s.getCleanHtmlRemMedia()
                                            } else {
                                                s.getCleanHtmlAndMedia(settings)
                                            }
                                        }
                                    )
                                )
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