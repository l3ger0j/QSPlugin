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

#include <jni.h>

#ifndef QSP_ANDROIDDEFINES
	#define QSP_ANDROIDDEFINES

	static int qspEndiannessTestValue = 1;

	#ifdef _UNICODE
		#ifdef __APPLE__
			#include <stddef.h>
			#include <stdint.h>
		#else
			#include <uchar.h>
		#endif

		typedef char16_t QSP_CHAR;
		#define QSP_FMT2(x) u##x
		#define QSP_FMT(x) QSP_FMT2(x)

		#define QSP_STRCOLL qspStrsComp
		#define QSP_CHRLWR qspToWLower
		#define QSP_CHRUPR qspToWUpper
		#define QSP_ONIG_ENC ((*(char *)&(qspEndiannessTestValue) == 1) ? \
					(sizeof(QSP_CHAR) == 2 ? ONIG_ENCODING_UTF16_LE : ONIG_ENCODING_UTF32_LE) : \
					(sizeof(QSP_CHAR) == 2 ? ONIG_ENCODING_UTF16_BE : ONIG_ENCODING_UTF32_BE))
		#define QSP_FROM_OS_CHAR(a) qspReverseConvertUC(a, qspCP1251ToUnicodeTable)
		#define QSP_TO_OS_CHAR(a) qspDirectConvertUC(a, qspCP1251ToUnicodeTable)
		#define QSP_WCTOB
		#define QSP_BTOWC

		#ifdef _MSC_VER
			#define QSP_FOPEN _wfopen
		#else
			#define QSP_FOPEN qspFileOpen
		#endif
	#else
		#error "Non-Unicode build using Android binding is not supported"
	#endif

	#define QSP_FIXBYTESORDER(a) ((*(char *)&(qspEndiannessTestValue) == 1) ? \
			(a) : \
			((unsigned short)(((a) << 8) | ((a) >> 8))))

	#ifdef _MSC_VER
		#define QSP_TIME _time64
	#else
		#define QSP_TIME time
	#endif

	typedef jmethodID QSP_CALLBACK;

	#include "../qsp.h"
	#include "org_libsnxqsp_jni_SNXLib.h"

	extern JavaVM *snxJvm;
	extern jclass snxApiClass;
	extern jobject snxApiObject;

	extern jclass snxListItemClass;
	extern jclass snxExecutionStateClass;
	extern jclass snxErrorInfoClass;
	extern jclass snxVarValResp;

	typedef struct
	{
		jstring Image;
		jstring Name;
		jobject ListItem;
	} JNIListItem;

	char *qspW2C(QSP_CHAR *);
	QSP_CHAR *qspC2W(char *);
	jstring snxToJavaString(JNIEnv *env, QSP_CHAR *str);
	QSP_CHAR *snxFromJavaString(JNIEnv *env, jstring str);
	JNIListItem snxToJavaListItem(JNIEnv *env, QSP_CHAR *image, QSP_CHAR *name);
	void snxReleaseJavaListItem(JNIEnv *env, JNIListItem *listItem);

#endif
