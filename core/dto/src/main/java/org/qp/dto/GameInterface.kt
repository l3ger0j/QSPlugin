package org.qp.dto

interface GameInterface {
    fun requestReceiveFile(filePath: String)
    fun requestCreateFile(path: String, mimeType: String)
    fun isPlayingFile(filePath: String)
    fun closeAllFiles()
    fun closeFile(filePath: String)
    fun playFile(path: String, volume: Int)
    fun changeGameDir(filePath: String)
    fun showLibDialog(
        dialogType: LibTypeDialog,
        inputString: String = "",
        menuItems: List<LibGenItem> = listOf(),
    )
    fun showLibPopup(popupType: LibTypePopup)

    fun changeVisWindow(type: LibTypeWindow, show: Boolean)

    /**
     * Set the counter location processing interval to `delayMillis` milliseconds.
     */
    fun setCountInter(delayMillis: Long)

    /**
     * Execute `runnable` without processing the location counter.
     */
    fun doWithCounterDisabled(runnable: Runnable)

    fun setGameState(gameState: LibGameState)
    fun setUIConfig(uiConfig: LibUIConfig)
}
