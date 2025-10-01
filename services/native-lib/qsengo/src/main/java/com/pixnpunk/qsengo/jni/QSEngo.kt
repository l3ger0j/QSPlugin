package com.pixnpunk.qsengo.jni

import java.io.FileDescriptor

abstract class QSEngo {
    @JvmRecord
    data class ListItem(val image: String?, val text: String?)

    @JvmRecord
    data class ExecutionState(val loc: String?, val actIndex: Int, val lineNum: Int)

    @JvmRecord
    data class VarValResp(val isSuccess: Boolean, val stringValue: String?, val intValue: Int)

    @JvmRecord
    data class ErrorData(val locName: String?, val errorNum: Int, val index: Int, val line: Int)

    companion object {
        init {
            System.loadLibrary("ndkqsp")
        }
    }

    // --- Lifecycle and State ---
    external fun init()
    external fun terminate()
    external fun enableDebugMode(isDebug: Boolean)

    // --- Main Description ---
    external fun getMainDesc(): String?

    // --- Variables Description ---
    external fun getVarsDesc(): String?

    /**
     * Gets the number of values for a given variable array.
     * @param name The name of the variable.
     * @return The count of values, or 0 if the variable is not found.
     */
    external fun getVarValues(name: String, ind: Int): VarValResp? //!!!STUB

    // --- Input ---
    external fun setInputStrText(text: String)

    // --- Actions ---
    external fun getActionData(): Array<ListItem?>?
    external fun executeSelActionCode(isRefresh: Boolean): Boolean
    external fun setSelActionIndex(ind: Int, isRefresh: Boolean): Boolean

    // --- Objects ---
    external fun getObjectData(): Array<ListItem?>?
    external fun setSelObjectIndex(ind: Int, isRefresh: Boolean): Boolean

    // --- Code execution ---
    external fun execString(s: String?, isRefresh: Boolean): Boolean
    external fun execCounter(isRefresh: Boolean): Boolean
    external fun execUserInput(isRefresh: Boolean): Boolean

    // --- Errors ---
    external fun getLastErrorData(): ErrorData?
    external fun getErrorDesc(errorNum: Int): String?

    // --- Game ---
    external fun loadGameWorldFromData(data: ByteArray, fileName: String): Boolean
    external fun loadGameWorldFromFD(fileDescriptor: FileDescriptor, fileName: String): Boolean
    external fun saveGameAsData(isRefresh: Boolean): ByteArray?
    external fun openSavedGameFromData(data: ByteArray?, isRefresh: Boolean): Boolean
    external fun openSavedGameFromFD(fileDescriptor: FileDescriptor, isRefresh: Boolean): Boolean
    external fun restartGame(isRefresh: Boolean): Boolean
    external fun selectMenuItem(index: Int)

    // --- Callbacks from Native Code ---
    abstract fun callDebug(str: String?)
    abstract fun refreshInt()

    abstract fun onShowImage(path: String?)
    abstract fun onShowMessage(message: String?)
    abstract fun onShowWindow(type: Int, isShow: Boolean)

    abstract fun onPlayFile(path: String?, volume: Int)
    abstract fun onIsPlayingFile(path: String?): Boolean
    abstract fun onCloseFile(path: String?)

    abstract fun onOpenGame(filename: String?)
    abstract fun onSaveGameStatus(filename: String?)
    abstract fun onInputBox(prompt: String?): String?

    abstract fun onSetTimer(msecs: Int)
    abstract fun onGetMsCount(): Int
    abstract fun onSleep(msecs: Int)

    abstract fun onAddMenuItem(name: String?, imgPath: String?)
    abstract fun onShowMenu()

    abstract fun onGetFileContents(path: String?): ByteArray?
    abstract fun onChangeQuestPath(path: String?)
}