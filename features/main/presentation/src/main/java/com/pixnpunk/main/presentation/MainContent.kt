package com.pixnpunk.main.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(component: MainComponent) {
    val state by component.model.collectAsState()
    val settings by component.settingsFlow.collectAsState()

    if (!state.isActionsVis || !state.actions.isNotEmpty()) {
        MainContentWebView(
            mainDesc = state.mainDesc,
            backColor = settings.backColor,
            paddingValues = PaddingValues(),
            setupWebClient = { component.getDefaultWebClient(it) }
        )
    } else {
        BottomSheetScaffold(
            sheetContent = {
                MainContentActionsList(
                    countActsVis = settings.countActsVis,
                    settings = settings,
                    actions = state.actions,
                    onActionClicked = component.onActionClicked
                )
            },
        ) { sheetValues ->
            MainContentWebView(
                mainDesc = state.mainDesc,
                backColor = settings.backColor,
                paddingValues = sheetValues,
                setupWebClient = { component.getDefaultWebClient(it) }
            )
        }
    }
}
