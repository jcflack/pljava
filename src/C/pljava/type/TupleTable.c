/*
 * This file contains software that has been made available under
 * The Mozilla Public License 1.1. Use and distribution hereof are
 * subject to the restrictions set forth therein.
 *
 * Copyright (c) 2003 TADA AB - Taby Sweden
 * All Rights Reserved
 */
#include <postgres.h>
#include <executor/spi.h>
#include <executor/tuptable.h>

#include "pljava/Exception.h"
#include "pljava/type/Type_priv.h"
#include "pljava/type/TupleTable.h"
#include "pljava/type/TupleTableSlot.h"
#include "pljava/type/TupleTable_JNI.h"

static Type      s_TupleTable;
static TypeClass s_TupleTableClass;
static jclass    s_TupleTable_class;
static jmethodID s_TupleTable_init;

/*
 * org.postgresql.pljava.type.Tuple type.
 */
jobject TupleTable_create(JNIEnv* env, TupleTable tts)
{
	if(tts == 0)
		return 0;

	jobject jtts = NativeStruct_obtain(env, tts);
	if(jtts == 0)
	{
		jtts = (*env)->NewObject(env, s_TupleTable_class, s_TupleTable_init);
		NativeStruct_init(env, jtts, tts);
	}
	return jtts;
}

static jvalue _TupleTable_coerceDatum(Type self, JNIEnv* env, Datum arg)
{
	jvalue result;
	result.l = TupleTable_create(env, (TupleTable)DatumGetPointer(arg));
	return result;
}

static Type TupleTable_obtain(Oid typeId)
{
	return s_TupleTable;
}

/* Make this datatype available to the postgres system.
 */
extern Datum TupleTable_initialize(PG_FUNCTION_ARGS);
PG_FUNCTION_INFO_V1(TupleTable_initialize);
Datum TupleTable_initialize(PG_FUNCTION_ARGS)
{
	JNIEnv* env = (JNIEnv*)PG_GETARG_POINTER(0);

	s_TupleTable_class = (*env)->NewGlobalRef(
				env, PgObject_getJavaClass(env, "org/postgresql/pljava/TupleTable"));

	s_TupleTable_init = PgObject_getJavaMethod(
				env, s_TupleTable_class, "<init>", "()V");

	s_TupleTableClass = NativeStructClass_alloc("type.Tuple");
	s_TupleTableClass->JNISignature   = "Lorg/postgresql/pljava/TupleTable;";
	s_TupleTableClass->javaTypeName   = "org.postgresql.pljava.TupleTable";
	s_TupleTableClass->coerceDatum    = _TupleTable_coerceDatum;
	s_TupleTable = TypeClass_allocInstance(s_TupleTableClass);

	Type_registerJavaType("org.postgresql.pljava.TupleTable", TupleTable_obtain);
	PG_RETURN_VOID();
}

/****************************************
 * JNI methods
 ****************************************/
/*
 * Class:     org_postgresql_pljava_TupleTable
 * Method:    getCount
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_org_postgresql_pljava_TupleTable_getCount(JNIEnv* env, jobject _this)
{
	TupleTable tupleTable = (TupleTable)NativeStruct_getStruct(env, _this);
	if(tupleTable == 0)
		return 0;
	return tupleTable->next;
}

/*
 * Class:     org_postgresql_pljava_TupleTable
 * Method:    getSlot
 * Signature: (I)Lorg/postgresql/pljava/TupleTableSlot;
 */
JNIEXPORT jobject JNICALL
Java_org_postgresql_pljava_TupleTable_getSlot(JNIEnv* env, jobject _this, jint pos)
{
	TupleTable tupleTable = (TupleTable)NativeStruct_getStruct(env, _this);
	if(tupleTable == 0)
		return 0;
		
	if(pos < 0 || pos >= tupleTable->next)
		return 0;

	return TupleTableSlot_create(env, tupleTable->array + pos);
}