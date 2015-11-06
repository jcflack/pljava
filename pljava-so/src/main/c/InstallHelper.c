/*
 * Copyright (c) 2015 Tada AB and other contributors, as listed below.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the The BSD 3-Clause License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Contributors:
 *   Chapman Flack
 */
#include <postgres.h>
#include <access/htup_details.h>
#include <catalog/pg_language.h>
#include <catalog/pg_proc.h>
#include <commands/portalcmds.h>
#include <tcop/pquery.h>
#include <utils/builtins.h>
#include <utils/memutils.h>
#include <utils/syscache.h>

#include "pljava/InstallHelper.h"

char const *pljavaLoadPath = NULL;

char const *pljavaHandlerPath = NULL;

void pljavaCheckLoadPath()
{
	if ( NULL == ActivePortal )
		return;
	List *l = ActivePortal->stmts;
	if ( NULL == l )
		return;
	if ( 1 < list_length( l) )
		elog(DEBUG1, "ActivePortal lists %d statements", list_length( l));
	Node *ut = (Node *)linitial(l);
	if ( NULL == ut )
	{
		elog(DEBUG1, "got null for first statement from ActivePortal");
		return;
	}
	if ( T_LoadStmt != nodeTag(ut) )
		return;
	LoadStmt *ls = (LoadStmt *)ut;
	if ( NULL == ls->filename )
	{
		elog(DEBUG1, "got null for a LOAD statement's filename");
		return;
	}
	pljavaLoadPath =
		(char const *)MemoryContextStrdup( TopMemoryContext, ls->filename);
}

void pljavaCheckHandlerPath(bool trusted, PG_FUNCTION_ARGS)
{
	bool isnull;
	Oid myOid = fcinfo->flinfo->fn_oid;
	HeapTuple myPT = SearchSysCache1(PROCOID, ObjectIdGetDatum(myOid));
	if (!HeapTupleIsValid(myPT))
		elog(ERROR, "cache lookup failed for function %u", myOid);
	Form_pg_proc myPS = (Form_pg_proc) GETSTRUCT(myPT);
	Oid langId = myPS->prolang;
	ReleaseSysCache(myPT);
	HeapTuple langTup = SearchSysCache1(LANGOID, ObjectIdGetDatum(langId));
	if (!HeapTupleIsValid(langTup))
		elog(ERROR, "cache lookup failed for language %u", langId);
	Form_pg_language langSt = (Form_pg_language) GETSTRUCT(langTup);
	Oid handlerOid = langSt->lanplcallfoid;
	ReleaseSysCache(langTup);
	HeapTuple handlerPT =
		SearchSysCache1(PROCOID, ObjectIdGetDatum(handlerOid));
	if (!HeapTupleIsValid(handlerPT))
		elog(ERROR, "cache lookup failed for function %u", handlerOid);
	Datum probinattr =
		SysCacheGetAttr(PROCOID, handlerPT, Anum_pg_proc_probin, &isnull);
	if ( isnull )
		elog(ERROR, "null probin for C function %u", handlerOid);
	char *probinstring = TextDatumGetCString(probinattr);
	pljavaHandlerPath =
		(char const *)MemoryContextStrdup( TopMemoryContext, probinstring);
	pfree(probinstring);
	ReleaseSysCache(handlerPT);
}
