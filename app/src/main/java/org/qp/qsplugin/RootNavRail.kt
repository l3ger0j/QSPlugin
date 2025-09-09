package org.qp.qsplugin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.runtime.Composable

@Composable
fun RootNavRail(root: RealRootComponent, activeComponent: RootComponent.Child) {
    NavigationRail {
        NavigationRailItem(
            icon = { Icon(Icons.Default.Sell, contentDescription = "") },
            onClick = { root.navigation(RealRootComponent.ChildConfig.Main) },
            selected = activeComponent is RootComponent.Child.MainChild,
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Menu, contentDescription = "") },
            onClick = { root.navigation(RealRootComponent.ChildConfig.Extra) },
            selected = activeComponent is RootComponent.Child.ExtraChild,
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Work, contentDescription = "") },
            onClick = { root.navigation(RealRootComponent.ChildConfig.Object) },
            selected = activeComponent is RootComponent.Child.ObjectChild,
        )
    }
}