package org.qp.utils

import android.util.Base64
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.safety.Safelist
import org.qp.dto.LibUIConfig
import org.qp.dto.GameSettings
import org.qp.utils.Base64Util.encodeBase64
import org.qp.utils.ViewUtil.asString
import java.util.regex.Pattern

object HtmlUtil {
    private val EXEC_PATTERN: Pattern =
        Pattern.compile("href=\"exec:([\\s\\S]*?)\"", Pattern.CASE_INSENSITIVE)
    private val HTML_PATTERN: Pattern = Pattern.compile("<(\"[^\"]*\"|'[^']*'|[^'\">])*>")

    private const val PAGE_HEAD_TEMPLATE: String = """
            <!DOCTYPE html>
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <style type="text/css">
              body {
                margin: 0;
                padding: 0.5em;
                color: QSPTEXTCOLOR;
                background-color: QSPBACKCOLOR;
                font-size: QSPFONTSIZE;
                font-family: QSPFONTSTYLE;
              }
              a { color: QSPLINKCOLOR; }
              a:link { color: QSPLINKCOLOR; }
            </style>
            </head>
        """
    private const val PAGE_BODY_TEMPLATE: String = "<body>REPLACETEXT</body>"

    fun appendPageTemplate(settings: GameSettings, body: String): String {
        val fixBody = body.replaceIndent("<br>")

        if (settings.isUseHtml) {
            if (fixBody.contains("\\\"")) {
                fixBody.replace("\\\"", "'")
            }

            if (EXEC_PATTERN.matcher(fixBody).find()) {
                fixBody.encodeExec()
            }
        }

        val pageHeadTemplate = PAGE_HEAD_TEMPLATE
            .replace("QSPTEXTCOLOR", settings.textColor.toHexString())
            .replace("QSPBACKCOLOR", settings.backColor.toHexString())
            .replace("QSPLINKCOLOR", settings.linkColor.toHexString())
            .replace("QSPFONTSTYLE", settings.typeface.asString())
            .replace("QSPFONTSIZE", settings.fontSize.toString())

        return pageHeadTemplate + PAGE_BODY_TEMPLATE.replace("REPLACETEXT", fixBody)
    }

    /**
     * Bring the HTML code `html` obtained from the library to
     * HTML code acceptable for display in [android.webkit.WebView].
     */
    fun String.getCleanHtmlAndMedia(settings: GameSettings): String {
        return Jsoup.parse(this).apply {
            outputSettings().prettyPrint(true)
            body().handleImagesInHtml(settings)
            body().handleVideosInHtml(settings)
        }.html()
    }

    fun String.getCleanHtmlRemMedia(): String {
        return Jsoup.parse(this).apply {
            outputSettings().prettyPrint(true)
            body().select("img").remove()
            body().select("video").remove()
        }.html()
    }

    fun String.getSrcDir(): String {
        val document = Jsoup.parse(this)
        val imageElement = document.select("img").first() ?: return ""
        return imageElement.attr("src")
    }

    fun String.isContainsHtmlTags(): Boolean {
        return HTML_PATTERN.matcher(this).find()
    }

    /**
     * Remove HTML tags from the `html` string and return the resulting string.
     */
    fun String.removeHtmlTags(): String {
        val result = StringBuilder()
        val len = this.length
        var fromIdx = 0

        while (fromIdx < len) {
            val idx = this.indexOf('<', fromIdx)
            if (idx == -1) {
                result.append(this.substring(fromIdx))
                break
            }
            result.append(this, fromIdx, idx)
            val endIdx = this.indexOf('>', idx + 1)
            if (endIdx == -1) {
                return Jsoup.clean(this, Safelist.none())
            }
            fromIdx = endIdx + 1
        }

        return result.toString()
    }

    private fun String.encodeExec(): String {
        val matcher = EXEC_PATTERN.matcher(this)
        val buffer = StringBuffer()

        while (matcher.find()) {
            val path = matcher.group(1) ?: continue
            val encodedExec = path.normalizePathsInExec().encodeBase64(Base64.NO_WRAP)
            matcher.appendReplacement(buffer, "href=\"exec:$encodedExec\"")
        }

        matcher.appendTail(buffer)
        return buffer.toString()
    }

    private fun String.normalizePathsInExec(): String {
        return this.replace("\\", "/")
    }

    private fun Element.handleImagesInHtml(
        settings: GameSettings
    ) {
        if (settings.isUseFullscreenImages) {
            val dynBlackList = mutableListOf<String>()
            this.select("a").forEach {
                if (it.attr("href").contains("exec:")) {
                    dynBlackList.add(it.select("img").attr("src"))
                }
            }
            this.select("img").forEach {
                if (!dynBlackList.contains(it.attr("src"))) {
                    it.attr("onclick", "img.onClickImage(this.src);")
                }
            }
        }

        this.select("img").forEach { img: Element ->
            if (settings.isUseAutoWidth && settings.isUseAutoHeight) {
                img.attr("style", "display: inline; height: auto; max-width: 100%;")
            } else {
                if (!settings.isUseAutoWidth) {
                    img.attr("style", "max-width: ${settings.customWidthImage};")
                }
                if (!settings.isUseAutoHeight) {
                    img.attr("style", "max-height: ${settings.customHeightImage};")
                }
            }
        }
    }

    private fun Element.handleVideosInHtml(
        settings: GameSettings
    ) {
        this.select("video").apply {
            attr("style", "max-width:100%;")
            if (settings.isVideoMute) {
                attr("muted", "true")
                removeAttr("controls")
            } else {
                attr("controls", "true")
                removeAttr("muted")
            }
        }
    }
}
