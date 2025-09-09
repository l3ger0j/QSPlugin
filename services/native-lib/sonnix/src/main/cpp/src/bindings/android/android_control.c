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

#include "../../actions.h"
#include "../../callbacks.h"
#include "../../coding.h"
#include "../../common.h"
#include "../../errors.h"
#include "../../game.h"
#include "../../locations.h"
#include "../../mathops.h"
#include "../../menu.h"
#include "../../objects.h"
#include "../../statements.h"
#include "../../text.h"
#include "../../time_qsp.h"
#include "../../variables.h"
#include "../../variant.h"
#include "../../common.h"

JavaVM *snxJvm;
jclass snxApiClass;
jobject snxApiObject;

jclass snxListItemClass;
jclass snxExecutionStateClass;
jclass snxErrorInfoClass;
jclass snxVarValResp;

/* ------------------------------------------------------------ */
/* Debugging */

/* Managing the debugging mode */
JNIEXPORT void JNICALL Java_org_libsnxqsp_jni_SNXLib_QSPEnableDebugMode(JNIEnv *env, jobject this, jboolean isDebug)
{
	qspIsDebug = isDebug;
}

/* Getting current state data */
JNIEXPORT jobject JNICALL Java_org_libsnxqsp_jni_SNXLib_QSPGetCurStateData(JNIEnv *env, jobject this)
{
	jfieldID fieldId;
	QSP_CHAR *locName;
	jobject jniExecutionState = (*env)->AllocObject(env, snxExecutionStateClass);

	locName = ((qspRealCurLoc >= 0 && qspRealCurLoc < qspLocsCount) ? qspLocs[qspRealCurLoc].Name : 0);

	fieldId = (*env)->GetFieldID(env, snxExecutionStateClass , "loc", "Ljava/lang/String;");
	(*env)->SetObjectField(env, jniExecutionState, fieldId, snxToJavaString(env, locName));

	fieldId = (*env)->GetFieldID(env, snxExecutionStateClass , "actIndex", "I");
	(*env)->SetIntField(env, jniExecutionState, fieldId, qspRealActIndex);

	fieldId = (*env)->GetFieldID(env, snxExecutionStateClass , "lineNum", "I");
	(*env)->SetIntField(env, jniExecutionState, fieldId, qspRealLine);

	return jniExecutionState;
}
/* ------------------------------------------------------------ */
/* Version Information */

/* Version */
JNIEXPORT jstring JNICALL Java_org_libsnxqsp_jni_SNXLib_QSPGetVersion(JNIEnv *env, jobject this)
{
	return snxToJavaString(env, QSP_VER);
}

/* Date and time of compilation */
JNIEXPORT jstring JNICALL Java_org_libsnxqsp_jni_SNXLib_QSPGetCompiledDateTime(JNIEnv *env, jobject this)
{
	return snxToJavaString(env, QSP_FMT(__DATE__) QSP_FMT(", ") QSP_FMT(__TIME__));
}
/* ------------------------------------------------------------ */
/* Number of full location updates */
JNIEXPORT jint JNICALL Java_org_libsnxqsp_jni_SNXLib_QSPGetFullRefreshCount(JNIEnv *env, jobject this)
{
	return qspFullRefreshCount;
}
/* ------------------------------------------------------------ */
/* Full path to the downloaded game file */
JNIEXPORT jstring JNICALL Java_org_libsnxqsp_jni_SNXLib_QSPGetQstFullPath(JNIEnv *env, jobject this)
{
	return snxToJavaString(env, qspQstFullPath);
}
/* ------------------------------------------------------------ */
/* Name of the current location */
JNIEXPORT jstring JNICALL Java_org_libsnxqsp_jni_SNXLib_QSPGetCurLoc(JNIEnv *env, jobject this)
{
	return snxToJavaString(env, qspCurLoc >= 0 ? qspLocs[qspCurLoc].Name : 0);
}
/* ------------------------------------------------------------ */
/* Basic description of the location */

/* Text of the main location description window */
JNIEXPORT jstring JNICALL Java_org_libsnxqsp_jni_SNXLib_getMainDesc(JNIEnv *env, jobject this)
{
	return snxToJavaString(env, qspCurDesc);
}

/* The ability to change the text of the main description */
JNIEXPORT jboolean JNICALL Java_org_libsnxqsp_jni_SNXLib_isMainDescChanged(JNIEnv *env, jobject this)
{
	return qspIsMainDescChanged;
}
/* ------------------------------------------------------------ */
/* Additional description of the location */

/* Text of the additional location description window */
JNIEXPORT jstring JNICALL Java_org_libsnxqsp_jni_SNXLib_getVarsDesc(JNIEnv *env, jobject this)
{
	return snxToJavaString(env, qspCurVars);
}

/* Possibility to change the text of the additional description */
JNIEXPORT jboolean JNICALL Java_org_libsnxqsp_jni_SNXLib_isVarsDescChanged(JNIEnv *env, jobject this)
{
	return qspIsVarsDescChanged;
}

/* ------------------------------------------------------------ */
/* Get the value of the specified expression */
//(const QSP_CHAR *expr, QSP_BOOL *isString, int *numVal, QSP_CHAR *strVal, int strValBufSize)
JNIEXPORT jobject JNICALL Java_org_libsnxqsp_jni_SNXLib_QSPGetExprValue(JNIEnv *env, jobject this)
{
	//!!!STUB
	//{
	//	QSPVariant v;
	//	if (qspIsExitOnError && qspErrorNum) return QSP_FALSE;
	//	qspResetError();
	//	if (qspIsDisableCodeExec) return QSP_FALSE;
	//	v = qspExprValue((QSP_CHAR *)expr);
	//	if (qspErrorNum) return QSP_FALSE;
	//	*isString = v.IsStr;
	//	if (v.IsStr)
	//	{
	//		qspStrNCopy(strVal, QSP_STR(v), strValBufSize - 1);
	//		free(QSP_STR(v));
	//		strVal[strValBufSize - 1] = 0;
	//	}
	//	else
	//		*numVal = QSP_NUM(v);
	//	return QSP_TRUE;
	//}
	return NULL;
}
/* ------------------------------------------------------------ */
/* Text of the input line */
JNIEXPORT void JNICALL Java_org_libsnxqsp_jni_SNXLib_setInputStrText(JNIEnv *env, jobject this, jstring val)
{
	QSP_CHAR *strConverted = snxFromJavaString(env, val);
	qspCurInputLen = qspAddText(&strConverted, (QSP_CHAR *)val, 0, -1, QSP_FALSE);
}
/* ------------------------------------------------------------ */
/* List of actions */

/* Data actions with the specified index */
JNIEXPORT jobjectArray JNICALL Java_org_libsnxqsp_jni_SNXLib_getActions(JNIEnv *env, jobject this)
{
	int i;
	JNIListItem item;
	jobjectArray res = (*env)->NewObjectArray(env, qspCurActionsCount, snxListItemClass, 0);
	for (i = 0; i < qspCurActionsCount; ++i)
	{
		item = snxToJavaListItem(env, qspCurActions[i].Image, qspCurActions[i].Desc);
		(*env)->SetObjectArrayElement(env, res, i, item.ListItem);
	}
	return res;
}

/* Executing the code of the selected action */
JNIEXPORT jboolean JNICALL Java_org_libsnxqsp_jni_SNXLib_executeSelActionCode(JNIEnv *env, jobject this, jboolean isRefresh)
{
	if (qspCurSelAction >= 0)
	{
		if (qspIsExitOnError && qspErrorNum) return JNI_FALSE;
		qspPrepareExecution();
		if (qspIsDisableCodeExec) return JNI_FALSE;
		qspExecAction(qspCurSelAction);
		if (qspErrorNum) return JNI_FALSE;
		if (isRefresh) qspCallRefreshInt(QSP_FALSE);
	}
	return JNI_TRUE;
}

/* Set the index of the selected action */
JNIEXPORT jboolean JNICALL Java_org_libsnxqsp_jni_SNXLib_setSelActionIndex(JNIEnv *env, jobject this, jint ind, jboolean isRefresh)
{
	if (ind >= 0 && ind < qspCurActionsCount && ind != qspCurSelAction)
	{
		if (qspIsExitOnError && qspErrorNum) return JNI_FALSE;
		qspPrepareExecution();
		if (qspIsDisableCodeExec) return JNI_FALSE;
		qspCurSelAction = ind;
		qspExecLocByVarNameWithArgs(QSP_FMT("ONACTSEL"), 0, 0);
		if (qspErrorNum) return JNI_FALSE;
		if ((QSP_BOOL)isRefresh) qspCallRefreshInt(QSP_FALSE);
	}
	return JNI_TRUE;
}

/* Get the index of the selected action */
JNIEXPORT jint JNICALL Java_org_libsnxqsp_jni_SNXLib_getSelActionIndex(JNIEnv *env, jobject this)
{
	return qspCurSelAction;
}

/* Ability to change the list of actions */
JNIEXPORT jboolean JNICALL Java_org_libsnxqsp_jni_SNXLib_isActionsChanged(JNIEnv *env, jobject this)
{
	return qspIsActionsChanged;
}
/* ------------------------------------------------------------ */
/* List of objects */

/* Object data with the specified index */
JNIEXPORT jobjectArray JNICALL Java_org_libsnxqsp_jni_SNXLib_getObjects(JNIEnv *env, jobject this)
{
	int i;
	JNIListItem item;
	jobjectArray res = (*env)->NewObjectArray(env, qspCurObjectsCount, snxListItemClass, 0);
	for (i = 0; i < qspCurObjectsCount; ++i)
	{
		item = snxToJavaListItem(env, qspCurObjects[i].Image, qspCurObjects[i].Desc);
		(*env)->SetObjectArrayElement(env, res, i, item.ListItem);
	}
	return res;
}

/* Set the index of the selected object */
JNIEXPORT jboolean JNICALL Java_org_libsnxqsp_jni_SNXLib_setSelObjectIndex(JNIEnv *env, jobject this, jint ind, jboolean isRefresh)
{
	if (ind >= 0 && ind < qspCurObjectsCount && ind != qspCurSelObject)
	{
		if (qspIsExitOnError && qspErrorNum) return QSP_FALSE;
		qspPrepareExecution();
		if (qspIsDisableCodeExec) return QSP_FALSE;
		qspCurSelObject = ind;
		qspExecLocByVarNameWithArgs(QSP_FMT("ONOBJSEL"), 0, 0);
		if (qspErrorNum) return QSP_FALSE;
		if (isRefresh) qspCallRefreshInt(QSP_FALSE);
	}
	return QSP_TRUE;
}

/* Get the index of the selected object */
JNIEXPORT jint JNICALL Java_org_libsnxqsp_jni_SNXLib_getSelObjectIndex(JNIEnv *env, jobject this)
{
	return qspCurSelObject;
}

/* Ability to change the list of objects */
JNIEXPORT jboolean JNICALL Java_org_libsnxqsp_jni_SNXLib_isObjectsChanged(JNIEnv *env, jobject this)
{
	return qspIsObjectsChanged;
}
/* ------------------------------------------------------------ */
/* Show/hide windows */
JNIEXPORT void JNICALL Java_org_libsnxqsp_jni_SNXLib_QSPShowWindow(JNIEnv *env, jobject this, jint type, jboolean isShow)
{
	switch (type) {
	case QSP_WIN_ACTS:
		qspCurIsShowActs = isShow;
		break;
	case QSP_WIN_OBJS:
		qspCurIsShowObjs = isShow;
		break;
	case QSP_WIN_VARS:
		qspCurIsShowVars = isShow;
		break;
	case QSP_WIN_INPUT:
		qspCurIsShowInput = isShow;
		break;
	}
}
/* ------------------------------------------------------------ */
/* Variables */

/* Get the number of array elements */
//QSP_BOOL QSPGetVarValuesCount(const QSP_CHAR *name, int *count)
JNIEXPORT jint JNICALL Java_org_libsnxqsp_jni_SNXLib_getVarValuesCount(JNIEnv *env, jobject this, jstring name)
{
	QSP_CHAR *varName = snxFromJavaString(env, name);
	QSPVar *var = qspVarReference(varName, QSP_FALSE);
	if (var) return var->ValsCount;
	return 0;
}

/* Get the values of the specified array element */
//QSP_BOOL QSPGetVarValues(const QSP_CHAR *name, int ind, int *numVal, QSP_CHAR **strVal)
QSP_BOOL QSPGetVarValues(const QSP_CHAR *name, int ind, int *numVal, QSP_CHAR **strVal)
{
	QSPVar *var;
	if (qspIsExitOnError && qspErrorNum) return JNI_FALSE;
	qspResetError();
	var = qspVarReference((QSP_CHAR *)name, QSP_FALSE);
	if (qspErrorNum || ind < 0 || ind >= var->ValsCount) return JNI_FALSE;
	*numVal = var->Values[ind].Num;
	*strVal = var->Values[ind].Str;
	return JNI_TRUE;
}

JNIEXPORT jobject JNICALL Java_org_libsnxqsp_jni_SNXLib_getVarValues(JNIEnv *env, jobject this, jstring name, jint ind)
{
	//Convert array name to QSP string
	QSP_CHAR *strConverted = snxFromJavaString(env, name);

	//Call QSP function
	int numVal = 0;
	QSP_CHAR *strVal;
	QSP_BOOL result = QSPGetVarValues(strConverted, (int)ind, &numVal, &strVal);

	// If this class does not exist then return null.
	if (snxVarValResp == 0)
		return NULL;
	jobject obj = (*env)->AllocObject(env, snxVarValResp);

	jfieldID successFid = (*env)->GetFieldID(env, snxVarValResp, "isSuccess", "Z");
	if (successFid == 0)
		return NULL;
	if (result == QSP_TRUE) {
		(*env)->SetBooleanField(env, obj, successFid, JNI_TRUE);

		char *sz = qspW2C(strVal);
		jstring jstringVal = (*env)->NewStringUTF(env, sz);
		if (sz != NULL)
			free(sz);

		jfieldID stringValueFid = (*env)->GetFieldID(env, snxVarValResp, "stringValue", "Ljava/lang/String;");
		if (stringValueFid == 0)
			return NULL;
		(*env)->SetObjectField(env, obj, stringValueFid, jstringVal);

		jfieldID intValueFid = (*env)->GetFieldID(env, snxVarValResp, "intValue", "I");
		if (intValueFid == 0)
			return NULL;
		(*env)->SetIntField(env, obj, intValueFid, numVal);
	} else {
		(*env)->SetBooleanField(env, obj, successFid, JNI_FALSE);
	}

	return obj;
}

/* Get the maximum number of variables */
JNIEXPORT jint JNICALL Java_org_libsnxqsp_jni_SNXLib_QSPGetMaxVarsCount(JNIEnv *env, jobject this)
{
	return QSP_VARSCOUNT;
}

/* Get the variable name with the specified index */
JNIEXPORT jobject JNICALL Java_org_libsnxqsp_jni_SNXLib_QSPGetVarNameByIndex(JNIEnv *env, jobject this, jint index)
{
	//!!!STUB
//{
//	if (index < 0 || index >= QSP_VARSCOUNT || !qspVars[index].Name) return QSP_FALSE;
//	*name = qspVars[index].Name;
//	return QSP_TRUE;
//}
	return NULL;
}
/* ------------------------------------------------------------ */
/* Code Execution */

/* Executing a line of code */
JNIEXPORT jboolean JNICALL Java_org_libsnxqsp_jni_SNXLib_execString(JNIEnv *env, jobject this, jstring s, jboolean isRefresh)
{
	if (qspIsExitOnError && qspErrorNum) return JNI_FALSE;
	qspPrepareExecution();
	if (qspIsDisableCodeExec) return JNI_FALSE;
	QSP_CHAR *strConverted = snxFromJavaString(env, s);
	qspExecStringAsCodeWithArgs((QSP_CHAR *)strConverted, 0, 0);
	if (qspErrorNum) return QSP_FALSE;
	if ((QSP_BOOL)isRefresh) qspCallRefreshInt(QSP_FALSE);
	return JNI_TRUE;
}

/* Executing the code of the specified location */
JNIEXPORT jboolean JNICALL Java_org_libsnxqsp_jni_SNXLib_execLocationCode(JNIEnv *env, jobject this, jstring name, jboolean isRefresh)
{
	if (qspIsExitOnError && qspErrorNum) return JNI_FALSE;
	qspPrepareExecution();
	if (qspIsDisableCodeExec) return JNI_FALSE;
	QSP_CHAR *strConverted = snxFromJavaString(env, name);
	qspExecLocByName(strConverted, QSP_FALSE);
	if (qspErrorNum) return JNI_FALSE;
	if ((QSP_BOOL)isRefresh) qspCallRefreshInt(QSP_FALSE);
	return JNI_TRUE;
}

/* Execution of the location-counter code */
JNIEXPORT jboolean JNICALL Java_org_libsnxqsp_jni_SNXLib_execCounter(JNIEnv *env, jobject this, jboolean isRefresh)
{
	if (!qspIsInCallBack)
	{
		qspPrepareExecution();
		qspExecLocByVarNameWithArgs(QSP_FMT("COUNTER"), 0, 0);
		if (qspErrorNum) return JNI_FALSE;
		if (isRefresh) qspCallRefreshInt(QSP_FALSE);
	}
	return JNI_TRUE;
}

/* Execution of the code of the input line handler location */
JNIEXPORT jboolean JNICALL Java_org_libsnxqsp_jni_SNXLib_execUserInput(JNIEnv *env, jobject this, jboolean isRefresh)
{
	if (qspIsExitOnError && qspErrorNum) return JNI_FALSE;
	qspPrepareExecution();
	if (qspIsDisableCodeExec) return JNI_FALSE;
	qspExecLocByVarNameWithArgs(QSP_FMT("USERCOM"), 0, 0);
	if (qspErrorNum) return JNI_FALSE;
	if (isRefresh) qspCallRefreshInt(QSP_FALSE);
	return JNI_TRUE;
}
/* ------------------------------------------------------------ */
/* Errors */

/* Get information about the latest error */
JNIEXPORT jobject JNICALL Java_org_libsnxqsp_jni_SNXLib_getLastErrorData(JNIEnv *env, jobject this)
{
	jfieldID fieldId;
	jobject jniErrorInfo = (*env)->AllocObject(env, snxErrorInfoClass);

	fieldId = (*env)->GetFieldID(env, snxErrorInfoClass, "locName", "Ljava/lang/String;");
	(*env)->SetObjectField(env, jniErrorInfo, fieldId, snxToJavaString(env, qspErrorLoc >= 0 && qspErrorLoc < qspLocsCount ? qspLocs[qspErrorLoc].Name : 0));

	fieldId = (*env)->GetFieldID(env, snxErrorInfoClass, "errorNum", "I");
	(*env)->SetIntField(env, jniErrorInfo, fieldId, qspErrorNum);

	fieldId = (*env)->GetFieldID(env, snxErrorInfoClass, "index", "I");
	(*env)->SetIntField(env, jniErrorInfo, fieldId, qspErrorActIndex);

	fieldId = (*env)->GetFieldID(env, snxErrorInfoClass, "line", "I");
	(*env)->SetIntField(env, jniErrorInfo, fieldId, qspErrorLine);

	return jniErrorInfo;
}

/* Get a description of the error by its number */
JNIEXPORT jstring JNICALL Java_org_libsnxqsp_jni_SNXLib_getErrorDesc(JNIEnv *env, jobject this, jint errorNum)
{
	QSP_CHAR *str;
	switch (errorNum)
	{
		case QSP_ERR_DIVBYZERO: str = QSP_FMT("Division by zero!"); break;
		case QSP_ERR_TYPEMISMATCH: str = QSP_FMT("Type mismatch!"); break;
		case QSP_ERR_STACKOVERFLOW: str = QSP_FMT("Stack overflow!"); break;
		case QSP_ERR_TOOMANYITEMS: str = QSP_FMT("Too many items in expression!"); break;
		case QSP_ERR_FILENOTFOUND: str = QSP_FMT("File not found!"); break;
		case QSP_ERR_CANTLOADFILE: str = QSP_FMT("Can't load file!"); break;
		case QSP_ERR_GAMENOTLOADED: str = QSP_FMT("Game not loaded!"); break;
		case QSP_ERR_COLONNOTFOUND: str = QSP_FMT("Sign [:] not found!"); break;
		case QSP_ERR_CANTINCFILE: str = QSP_FMT("Can't add file!"); break;
		case QSP_ERR_CANTADDACTION: str = QSP_FMT("Can't add action!"); break;
		case QSP_ERR_EQNOTFOUND: str = QSP_FMT("Sign [=] not found!"); break;
		case QSP_ERR_LOCNOTFOUND: str = QSP_FMT("Location not found!"); break;
		case QSP_ERR_ENDNOTFOUND: str = QSP_FMT("[end] not found!"); break;
		case QSP_ERR_LABELNOTFOUND: str = QSP_FMT("Label not found!"); break;
		case QSP_ERR_NOTCORRECTNAME: str = QSP_FMT("Incorrect variable's name!"); break;
		case QSP_ERR_QUOTNOTFOUND: str = QSP_FMT("Quote not found!"); break;
		case QSP_ERR_BRACKNOTFOUND: str = QSP_FMT("Bracket not found!"); break;
		case QSP_ERR_BRACKSNOTFOUND: str = QSP_FMT("Brackets not found!"); break;
		case QSP_ERR_SYNTAX: str = QSP_FMT("Syntax error!"); break;
		case QSP_ERR_UNKNOWNACTION: str = QSP_FMT("Unknown action!"); break;
		case QSP_ERR_ARGSCOUNT: str = QSP_FMT("Incorrect arguments' count!"); break;
		case QSP_ERR_CANTADDOBJECT: str = QSP_FMT("Can't add object!"); break;
		case QSP_ERR_CANTADDMENUITEM: str = QSP_FMT("Can't add menu's item!"); break;
		case QSP_ERR_TOOMANYVARS: str = QSP_FMT("Too many variables!"); break;
		case QSP_ERR_INCORRECTREGEXP: str = QSP_FMT("Regular expression's error!"); break;
		case QSP_ERR_CODENOTFOUND: str = QSP_FMT("Code not found!"); break;
		default: str = QSP_FMT("Unknown error!"); break;
	}
	return snxToJavaString(env, str);
}
/* ------------------------------------------------------------ */
/* Game Management */

/* Working with memory */

/* Loading a new game from memory */
QSP_BOOL QSPLoadGameWorldFromData(const char *data, int dataSize, const QSP_CHAR *fileName)
{
	char *ptr;
	if (qspIsExitOnError && qspErrorNum) return QSP_FALSE;
	qspResetError();
	if (qspIsDisableCodeExec) return QSP_FALSE;
	ptr = (char *)malloc(dataSize + 3);
	memcpy(ptr, data, dataSize);
	ptr[dataSize] = ptr[dataSize + 1] = ptr[dataSize + 2] = 0;
	qspOpenQuestFromData(ptr, dataSize + 3, (QSP_CHAR *)fileName, QSP_FALSE);
	free(ptr);
	if (qspErrorNum) return QSP_FALSE;
	return QSP_TRUE;
}
JNIEXPORT jboolean JNICALL Java_org_libsnxqsp_jni_SNXLib_loadGameWorldFromData(JNIEnv *env, jobject this, jbyteArray data, jstring fileName)
{
	//converting data
	jint dataSize = (*env)->GetArrayLength(env, data);
	jbyte *jbuf = malloc(dataSize * sizeof(jbyte));
	if (jbuf == NULL)
		return JNI_FALSE;

	(*env)->GetByteArrayRegion(env, data, 0, dataSize, jbuf);
	int size = dataSize;
	char *mydata = (char *)jbuf;

	/* assume the prompt string and user input has less than 128
		characters */
	int fileNameLen = (*env)->GetStringLength(env, fileName) + 1;
	char buf[fileNameLen];
	const jbyte *str;
	str = (*env)->GetStringUTFChars(env, fileName, NULL);
	if (str == NULL) {
		free(jbuf);
		return JNI_FALSE; /* OutOfMemoryError already thrown */
	}

	QSP_CHAR *wcs = qspC2W(str);
	jboolean result = QSPLoadGameWorldFromData(mydata, size, wcs);
	(*env)->ReleaseStringUTFChars(env, fileName, str);
	free(wcs);

	free(jbuf);
	return result;
}

/* Saving state to memory */
QSP_BOOL QSPSaveGameAsData(void **buf, int *realSize, QSP_BOOL isRefresh)
{
	int len, size;
	QSP_CHAR *data;
	if (qspIsExitOnError && qspErrorNum) return QSP_FALSE;
	qspPrepareExecution();
	if (qspIsDisableCodeExec) return QSP_FALSE;
	if (!(len = qspSaveGameStatusToString(&data)))
	{
		*realSize = 0;
		return QSP_FALSE;
	}
	size = len * sizeof(QSP_CHAR);
	*realSize = size;

	*buf = malloc(size);
	if (*buf == NULL)
	{
		free(data);
		return QSP_FALSE;
	}

	memcpy(*buf, data, size);
	free(data);
	if (isRefresh) qspCallRefreshInt(QSP_FALSE);
	return QSP_TRUE;
}
JNIEXPORT jbyteArray JNICALL Java_org_libsnxqsp_jni_SNXLib_saveGameAsData(JNIEnv *env, jobject this, jboolean isRefresh)
{
	void *buffer = NULL;
	int bufferSize = 0;
	if (QSPSaveGameAsData(&buffer, &bufferSize, (QSP_BOOL)isRefresh) == QSP_FALSE)
		return NULL;

	jbyteArray result;
	result = (*env)->NewByteArray(env, bufferSize);
	if (result == NULL)
		return NULL;

	(*env)->SetByteArrayRegion(env, result, 0, bufferSize, buffer);

	return result;
}

/* Loading state from memory */
QSP_BOOL QSPOpenSavedGameFromData(const void *data, int dataSize, QSP_BOOL isRefresh)
{
	int dataLen;
	QSP_CHAR *ptr;
	if (qspIsExitOnError && qspErrorNum) return QSP_FALSE;
	qspPrepareExecution();
	if (qspIsDisableCodeExec) return QSP_FALSE;
	dataLen = dataSize / sizeof(QSP_CHAR);
	ptr = (QSP_CHAR *)malloc((dataLen + 1) * sizeof(QSP_CHAR));
	memcpy(ptr, data, dataSize);
	ptr[dataLen] = 0;
	qspOpenGameStatusFromString(ptr);
	free(ptr);
	if (qspErrorNum) return QSP_FALSE;
	if (isRefresh) qspCallRefreshInt(QSP_FALSE);
	return QSP_TRUE;
}
JNIEXPORT jboolean JNICALL Java_org_libsnxqsp_jni_SNXLib_openSavedGameFromData(JNIEnv *env, jobject this, jbyteArray data, jboolean isRefresh)
{
	//converting data
	jint dataSize = (*env)->GetArrayLength(env, data);
	jbyte *jbuf = malloc(dataSize * sizeof(jbyte));
	if (jbuf == NULL)
		return JNI_FALSE;

	(*env)->GetByteArrayRegion(env, data, 0, dataSize, jbuf);
	int size = dataSize;
	void *mydata = (void *)jbuf;

	jboolean result = QSPOpenSavedGameFromData(mydata, size, (QSP_BOOL)isRefresh) == QSP_TRUE;

	free(jbuf);
	return result;
}

/* Loading the state from memory via a string */
QSP_BOOL QSPOpenSavedGameFromString(const QSP_CHAR* str, QSP_BOOL isRefresh)
{
	if (qspIsExitOnError && qspErrorNum) return QSP_FALSE;
	qspPrepareExecution();
	if (qspIsDisableCodeExec) return QSP_FALSE;
	qspOpenGameStatusFromString((QSP_CHAR*)str);
	if (qspErrorNum) return QSP_FALSE;
	if (isRefresh) qspCallRefreshInt(QSP_FALSE);
	return QSP_TRUE;
}

/* Restarting the game */
JNIEXPORT jboolean JNICALL Java_org_libsnxqsp_jni_SNXLib_restartGame(JNIEnv *env, jobject this, jboolean isRefresh)
{
	if (qspIsExitOnError && qspErrorNum) return JNI_FALSE;
	qspPrepareExecution();
	if (qspIsDisableCodeExec) return JNI_FALSE;
	qspNewGame(QSP_TRUE);
	if (qspErrorNum) return JNI_FALSE;
	if (isRefresh) qspCallRefreshInt(QSP_FALSE);
	return JNI_TRUE;
}

/* Initialization */
JNIEXPORT void JNICALL Java_org_libsnxqsp_jni_SNXLib_init(JNIEnv *env, jobject this)
{
	jclass clazz;

	qspInitRuntime();

	/* Get JVM references */
	(*env)->GetJavaVM(env, &snxJvm);

	clazz = (*env)->FindClass(env, "org/libsnxqsp/jni/SNXLib");
	snxApiClass = (jclass)(*env)->NewGlobalRef(env, clazz);
	snxApiObject = (jobject)(*env)->NewGlobalRef(env, this);

	clazz = (*env)->FindClass(env, "org/libsnxqsp/jni/SNXLib$ListItem");
	snxListItemClass = (jclass)(*env)->NewGlobalRef(env, clazz);

	clazz = (*env)->FindClass(env, "org/libsnxqsp/jni/SNXLib$ExecutionState");
	snxExecutionStateClass = (jclass)(*env)->NewGlobalRef(env, clazz);

	clazz = (*env)->FindClass(env, "org/libsnxqsp/jni/SNXLib$ErrorData");
	snxErrorInfoClass = (jclass)(*env)->NewGlobalRef(env, clazz);

	clazz = (*env)->FindClass(env, "org/libsnxqsp/jni/SNXLib$VarValResp");
	snxVarValResp = (jclass)(*env)->NewGlobalRef(env, clazz);

	/* Get references to callbacks */
	qspSetCallBack(QSP_CALL_DEBUG, (*env)->GetMethodID(env, snxApiClass, "onDebug", "(Ljava/lang/String;)V"));
	qspSetCallBack(QSP_CALL_ISPLAYINGFILE, (*env)->GetMethodID(env, snxApiClass, "onIsPlayingFile", "(Ljava/lang/String;)Z"));
	qspSetCallBack(QSP_CALL_PLAYFILE, (*env)->GetMethodID(env, snxApiClass, "onPlayFile", "(Ljava/lang/String;I)V"));
	qspSetCallBack(QSP_CALL_CLOSEFILE, (*env)->GetMethodID(env, snxApiClass, "onCloseFile", "(Ljava/lang/String;)V"));
	qspSetCallBack(QSP_CALL_SHOWIMAGE, (*env)->GetMethodID(env, snxApiClass, "onShowImage", "(Ljava/lang/String;)V"));
	qspSetCallBack(QSP_CALL_SHOWWINDOW, (*env)->GetMethodID(env, snxApiClass, "onShowWindow", "(IZ)V"));
	qspSetCallBack(QSP_CALL_SHOWMENU, (*env)->GetMethodID(env, snxApiClass, "onShowMenu", "()I"));
	qspSetCallBack(QSP_CALL_SHOWMSGSTR, (*env)->GetMethodID(env, snxApiClass, "onShowMessage", "(Ljava/lang/String;)V"));
	qspSetCallBack(QSP_CALL_REFRESHINT, (*env)->GetMethodID(env, snxApiClass, "onRefreshInt", "()V"));
	qspSetCallBack(QSP_CALL_SETTIMER, (*env)->GetMethodID(env, snxApiClass, "onSetTimer", "(I)V"));
	qspSetCallBack(QSP_CALL_SYSTEM, (*env)->GetMethodID(env, snxApiClass, "onSystem", "(Ljava/lang/String;)V"));
	qspSetCallBack(QSP_CALL_SETINPUTSTRTEXT, (*env)->GetMethodID(env, snxApiClass, "onSetInputStrText", "(Ljava/lang/String;)V"));
	qspSetCallBack(QSP_CALL_DELETEMENU, (*env)->GetMethodID(env, snxApiClass, "onDeleteMenu", "()V"));
	qspSetCallBack(QSP_CALL_OPENGAMESTATUS, (*env)->GetMethodID(env, snxApiClass, "onOpenGame", "(Ljava/lang/String;)V"));
	qspSetCallBack(QSP_CALL_SAVEGAMESTATUS, (*env)->GetMethodID(env, snxApiClass, "onSaveGame", "(Ljava/lang/String;)V"));
	qspSetCallBack(QSP_CALL_SLEEP, (*env)->GetMethodID(env, snxApiClass, "onSleep", "(I)V"));
	qspSetCallBack(QSP_CALL_GETMSCOUNT, (*env)->GetMethodID(env, snxApiClass, "onGetMsCount", "()I"));
	qspSetCallBack(QSP_CALL_INPUTBOX, (*env)->GetMethodID(env, snxApiClass, "onInputBox", "(Ljava/lang/String;)Ljava/lang/String;"));
	qspSetCallBack(QSP_CALL_ADDMENUITEM, (*env)->GetMethodID(env, snxApiClass, "onAddMenuItem", "(Ljava/lang/String;Ljava/lang/String;)V"));
}

/* Deinitialization */
JNIEXPORT void JNICALL Java_org_libsnxqsp_jni_SNXLib_terminate(JNIEnv *env, jobject this)
{
	qspTerminateRuntime();

	/* Release references */
	(*env)->DeleteGlobalRef(env, snxApiObject);
	(*env)->DeleteGlobalRef(env, snxApiClass);
	(*env)->DeleteGlobalRef(env, snxListItemClass);
	(*env)->DeleteGlobalRef(env, snxExecutionStateClass);
	(*env)->DeleteGlobalRef(env, snxErrorInfoClass);
}

#endif
