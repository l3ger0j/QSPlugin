/* Copyright (C) 2005-2010 Valeriy Argunov (nporep AT mail DOT ru) */
/*
* This library is free software; you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation; either version 2.1 of the License, or
* (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

#include "../../declarations.h"

#ifdef _ANDROID

#include "../../callbacks.h"
#include "../../actions.h"
#include "../../coding.h"
#include "../../common.h"
#include "../../errors.h"
#include "../../objects.h"
#include "../../text.h"

INLINE JNIEnv* snxGetJniEnv()
{
	JNIEnv* snxEnv;
	/* Callbacks should be called on the JVM threads only */
	(*snxJvm)->GetEnv(snxJvm, (void**)&snxEnv, JNI_VERSION_1_6);
	return snxEnv;
}

void qspInitCallBacks()
{
	int i;
	qspIsInCallBack = QSP_FALSE;
	qspIsDisableCodeExec = QSP_FALSE;
	qspIsExitOnError = QSP_FALSE;
	for (i = 0; i < QSP_CALL_DUMMY; ++i)
		qspCallBacks[i] = 0;
}

void qspSetCallBack(int type, QSP_CALLBACK func)
{
	qspCallBacks[type] = func;
}

void qspCallDebug(QSP_CHAR* str)
{
	if (str == NULL) return;
	if (qspCallBacks[QSP_CALL_DEBUG])
	{
		QSPCallState state;
		JNIEnv *javaEnv = snxGetJniEnv();
		jstring qspText = snxToJavaString(javaEnv, str);

		qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, snxApiObject, qspCallBacks[QSP_CALL_DEBUG], qspText);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);
	}
}

void qspCallSetTimer(int msecs)
{
	/* Set timer interval */
	if (qspCallBacks[QSP_CALL_SETTIMER])
	{
		QSPCallState state;
		JNIEnv *javaEnv = snxGetJniEnv();

		qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, snxApiObject, qspCallBacks[QSP_CALL_SETTIMER], msecs);
		qspRestoreCallState(&state);
	}
}

void qspCallRefreshInt(QSP_BOOL isRedraw)
{
	/* Refresh UI to show the latest state */
	if (qspCallBacks[QSP_CALL_REFRESHINT])
	{
		QSPCallState state;
		JNIEnv *javaEnv = snxGetJniEnv();

		qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, snxApiObject, qspCallBacks[QSP_CALL_REFRESHINT], isRedraw);
		qspRestoreCallState(&state);
	}
}

void qspCallSetInputStrText(QSP_CHAR* text)
{
	if (text == NULL) return;
	if (qspCallBacks[QSP_CALL_SETINPUTSTRTEXT])
	{
		QSPCallState state;
		JNIEnv *javaEnv = snxGetJniEnv();
		jstring qspText = snxToJavaString(javaEnv, text);

		qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, snxApiObject, qspCallBacks[QSP_CALL_SETINPUTSTRTEXT], qspText);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);
	}
}

void qspCallAddMenuItem(QSP_CHAR* name, QSP_CHAR* imgPath)
{
	if (name == NULL || imgPath == NULL) return;
	if (qspCallBacks[QSP_CALL_ADDMENUITEM]) {
		QSPCallState state;
		JNIEnv *javaEnv = snxGetJniEnv();
		jstring menuItemName = snxToJavaString(javaEnv, name);
		jstring menuItemImg = snxToJavaString(javaEnv, imgPath);

		qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, snxApiObject, qspCallBacks[QSP_CALL_ADDMENUITEM], menuItemName, menuItemImg);
		(*javaEnv)->DeleteLocalRef(javaEnv, menuItemName);
		(*javaEnv)->DeleteLocalRef(javaEnv, menuItemImg);
		qspRestoreCallState(&state);
	}
}

void qspCallSystem(QSP_CHAR* cmd)
{
	if (cmd == NULL) return;
	if (qspCallBacks[QSP_CALL_SYSTEM])
	{
		QSPCallState state;
		JNIEnv *javaEnv = snxGetJniEnv();
		jstring qspText = snxToJavaString(javaEnv, cmd);

		qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, snxApiObject, qspCallBacks[QSP_CALL_SYSTEM], qspText);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);
	}
}

void qspCallOpenGame(QSP_CHAR* file)
{
	if (file == NULL) return;
	if (qspCallBacks[QSP_CALL_OPENGAMESTATUS]) {
		QSPCallState state;
		JNIEnv *javaEnv = snxGetJniEnv();
		jstring qspText = snxToJavaString(javaEnv, file);

		qspSaveCallState(&state, QSP_FALSE, QSP_TRUE);
		(*javaEnv)->CallVoidMethod(javaEnv, snxApiObject, qspCallBacks[QSP_CALL_OPENGAMESTATUS], qspText);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);
	}
}

void qspCallSaveGame(QSP_CHAR* file)
{
	if (file == NULL) return;
	if (qspCallBacks[QSP_CALL_SAVEGAMESTATUS]) {
		QSPCallState state;
		JNIEnv *javaEnv = snxGetJniEnv();
		jstring qspText = snxToJavaString(javaEnv, file);

		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, snxApiObject, qspCallBacks[QSP_CALL_SAVEGAMESTATUS], qspText);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);
	}
}

void qspCallShowMessage(QSP_CHAR* text)
{
	if (text == NULL) return;
	if (qspCallBacks[QSP_CALL_SHOWMSGSTR]) {
		QSPCallState state;
		JNIEnv *javaEnv = snxGetJniEnv();
		jstring qspText = snxToJavaString(javaEnv, text);

		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, snxApiObject, qspCallBacks[QSP_CALL_SHOWMSGSTR], qspText);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);
	}
}

int qspCallShowMenu()
{
	if (qspCallBacks[QSP_CALL_SHOWMENU])
	{
		QSPCallState state;
		int index;
		JNIEnv *javaEnv = snxGetJniEnv();

		qspSaveCallState(&state, QSP_FALSE, QSP_TRUE);
		index = (*javaEnv)->CallIntMethod(javaEnv, snxApiObject, qspCallBacks[QSP_CALL_SHOWMENU]);
		qspRestoreCallState(&state);

		return index;
	}
	return -1;
}

void qspCallShowPicture(QSP_CHAR* file)
{
	if (file == NULL) return;
	if (qspCallBacks[QSP_CALL_SHOWIMAGE]) {
		QSPCallState state;
		JNIEnv *javaEnv = snxGetJniEnv();
		jstring qspText = snxToJavaString(javaEnv, file);

		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, snxApiObject, qspCallBacks[QSP_CALL_SHOWIMAGE], qspText);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);
	}
}

void qspCallShowWindow(int type, QSP_BOOL isShow)
{
	if (qspCallBacks[QSP_CALL_SHOWWINDOW]) {
		QSPCallState state;
		JNIEnv *javaEnv = snxGetJniEnv();

		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, snxApiObject, qspCallBacks[QSP_CALL_SHOWWINDOW], type, isShow);
		qspRestoreCallState(&state);
	}
}

void qspCallPlayFile(QSP_CHAR* file, int volume)
{
	if (file == NULL) return;
	if (qspCallBacks[QSP_CALL_PLAYFILE]) {
		QSPCallState state;
		JNIEnv *javaEnv = snxGetJniEnv();
		jstring qspText = snxToJavaString(javaEnv, file);

		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, snxApiObject, qspCallBacks[QSP_CALL_PLAYFILE], qspText, volume);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);
	}
}

QSP_BOOL qspCallIsPlayingFile(QSP_CHAR* file)
{
	if (file == NULL) return JNI_FALSE;
	if (qspCallBacks[QSP_CALL_ISPLAYINGFILE]) {
		QSPCallState state;
		QSP_BOOL isPlaying;
		JNIEnv *javaEnv = snxGetJniEnv();
		jstring qspText = snxToJavaString(javaEnv, file);

		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		isPlaying = (QSP_BOOL)(*javaEnv)->CallBooleanMethod(javaEnv, snxApiObject, qspCallBacks[QSP_CALL_ISPLAYINGFILE], qspText);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);

		return isPlaying;
	}
	return JNI_FALSE;
}

void qspCallSleep(int msecs)
{
	if (qspCallBacks[QSP_CALL_SLEEP]) {
		QSPCallState state;
		JNIEnv *javaEnv = snxGetJniEnv();

		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, snxApiObject, qspCallBacks[QSP_CALL_SLEEP], msecs);
		qspRestoreCallState(&state);
	}
}

int qspCallGetMSCount(void)
{
	if (qspCallBacks[QSP_CALL_GETMSCOUNT]) {
		QSPCallState state;
		int count;
		JNIEnv *javaEnv = snxGetJniEnv();

		qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		count = (*javaEnv)->CallIntMethod(javaEnv, snxApiObject, qspCallBacks[QSP_CALL_GETMSCOUNT]);
		qspRestoreCallState(&state);
		return count;
	}
	return 0;
}

void qspCallCloseFile(QSP_CHAR* file)
{
	if (file == NULL) return;
	if (qspCallBacks[QSP_CALL_CLOSEFILE]) {
		QSPCallState state;
		JNIEnv *javaEnv = snxGetJniEnv();
		jstring qspText = snxToJavaString(javaEnv, file);

		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, snxApiObject, qspCallBacks[QSP_CALL_CLOSEFILE], qspText);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);
	}
}

void qspCallDeleteMenu(void)
{
	if (qspCallBacks[QSP_CALL_DELETEMENU]) {
		QSPCallState state;
		JNIEnv *javaEnv = snxGetJniEnv();

		qspSaveCallState(&state, QSP_FALSE, QSP_FALSE);
		(*javaEnv)->CallVoidMethod(javaEnv, snxApiObject, qspCallBacks[QSP_CALL_DELETEMENU]);
		qspRestoreCallState(&state);
	}
}

QSP_CHAR* qspCallInputBox(QSP_CHAR* text)
{
	if (text == NULL) return qspGetNewText(QSP_FMT(""), 0);
	if (qspCallBacks[QSP_CALL_INPUTBOX]) {
		QSPCallState state;
		QSP_CHAR* buffer;
		JNIEnv *javaEnv = snxGetJniEnv();
		jstring qspText = snxToJavaString(javaEnv, text);

		qspSaveCallState(&state, QSP_TRUE, QSP_FALSE);
		jstring jResult = (jstring)((*javaEnv)->CallObjectMethod(javaEnv, snxApiObject, qspCallBacks[QSP_CALL_INPUTBOX], qspText));
		const char* str = (*javaEnv)->GetStringUTFChars(javaEnv, jResult, NULL);
		if (str != NULL)
			buffer = qspC2W(str);
		else
			buffer = qspGetNewText(QSP_FMT(""), 0);
		(*javaEnv)->ReleaseStringUTFChars(javaEnv, jResult, str);
		(*javaEnv)->DeleteLocalRef(javaEnv, qspText);
		qspRestoreCallState(&state);
		return buffer;
	}
	return qspGetNewText(QSP_FMT(""), 0);
}

#endif
