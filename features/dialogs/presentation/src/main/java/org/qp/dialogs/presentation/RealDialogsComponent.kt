package org.qp.dialogs.presentation

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.qp.settings.SettingsRepo

class RealDialogsComponent(
    private val componentContext: ComponentContext,
    override val dialogConfig: DialogConfig,
    override val onCleanError: () -> Unit,
    override val onEnterValue: (String) -> Unit,
    override val onSelMenuItem: (Int) -> Unit,
    override val onDismissed: () -> Unit,
) : ComponentContext by componentContext, DialogsComponent, KoinComponent {
    private val settingsRepo: SettingsRepo by inject()

    override val settingsFlow = settingsRepo.settingsState
}