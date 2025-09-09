package org.qp.dialogs.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DialogMenuContent(
    component: DialogsComponent
) {
    val settings by component.settingsFlow.collectAsState()
    val items = component.dialogConfig.dialogMenuItems

    Column(modifier = Modifier.padding(16.dp)) {
        LazyColumn {
            itemsIndexed(items) { key, value ->
                ListItem(
                    modifier = Modifier.clickable(true) {
                        component.onSelMenuItem(key)
                        component.onDismissed()
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color(settings.backColor)
                    ),
                    headlineContent = {
                        Text(
                            text = AnnotatedString.fromHtml(
                                htmlString = value.text,
                                linkStyles = TextLinkStyles(SpanStyle(Color(settings.linkColor)))
                            ),
                            style = LocalTextStyle.current.copy(
                                color = Color(settings.textColor),
                                fontSize = settings.fontSize.sp,
                                fontFamily = FontFamily(settings.typeface),
                                background = Color(settings.backColor)
                            )
                        )
                    }
                )
            }
        }
    }
}
