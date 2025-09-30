package org.qp.qsplugin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun RootNavBar(
    component: RootComponent,
    activeComponent: RootComponent.Child
) {
    val state by component.model.collectAsState()

    BottomAppBar {
        NavigationBarItem(
            enabled = state.isExtraEnabled,
            icon = {
                BadgedBox(
                    badge = { }
                ) {
                    if (activeComponent is RootComponent.Child.ExtraChild) {
                        Icon(Icons.Filled.Sell, contentDescription = "")
                    } else {
                        Icon(Icons.Outlined.Sell, contentDescription = "")
                    }

                }
            },
            label = { Text("Extra") },
            alwaysShowLabel = false,
            onClick = { component.navigation(RealRootComponent.ChildConfig.Extra) },
            selected = activeComponent is RootComponent.Child.ExtraChild,
        )
        NavigationBarItem(
            icon = {
                BadgedBox(
                    badge = { }
                ) {
                    Icon(Icons.Filled.Menu, contentDescription = "")
                }
            },
            label = { Text("Main") },
            alwaysShowLabel = false,
            onClick = { component.navigation(RealRootComponent.ChildConfig.Main) },
            selected = activeComponent is RootComponent.Child.MainChild,
        )
        NavigationBarItem(
            enabled = state.isObjectsEnabled,
            icon = {
                BadgedBox(
                    badge = { }
                ) {
                    if (activeComponent is RootComponent.Child.ObjectChild) {
                        Icon(Icons.Filled.Work, contentDescription = "")
                    } else {
                        Icon(Icons.Outlined.WorkOutline, contentDescription = "")
                    }
                }
            },
            label = { Text("Object") },
            alwaysShowLabel = false,
            onClick = { component.navigation(RealRootComponent.ChildConfig.Object) },
            selected = activeComponent is RootComponent.Child.ObjectChild,
        )
    }
}