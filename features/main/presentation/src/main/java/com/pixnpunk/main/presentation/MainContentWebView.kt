package com.pixnpunk.main.presentation

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun MainContentWebView(
    mainDesc: String,
    backColor: Int,
    paddingValues: PaddingValues,
    setupWebClient: (WebView) -> WebView
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            update = { webView ->
                webView.loadDataWithBaseURL(
                    "file:///",
                    mainDesc,
                    "text/html",
                    "UTF-8",
                    null
                )
            },
            factory = { ctx ->
                setupWebClient(
                    WebView(ctx).apply {
                        setBackgroundColor(backColor)
                        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                        loadDataWithBaseURL(
                            "file:///",
                            mainDesc,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                )
            }
        )
    }
}