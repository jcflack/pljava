/*
 * Copyright (c) 2025 Tada AB and other contributors, as listed below.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the The BSD 3-Clause License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Contributors:
 *   Chapman Flack
 */
package org.postgresql.pljava.example.fdw;

import java.sql.SQLException;
import java.sql.SQLNonTransientException;

import java.util.List;
import java.util.Map;

import org.postgresql.pljava.annotation.SQLAction;

import org.postgresql.pljava.fdw.*;

import org.postgresql.pljava.model.RegClass;
import org.postgresql.pljava.model.TupleTableSlot;

import org.postgresql.pljava.sqlgen.Lexicals.Identifier.Simple;

/**
 * A dummy foreign data wrapper example.
 * @author Chapman Flack
 */
@SQLAction(requires="pljavafdwhandler", install={
	"CREATE FUNCTION javatest.dummyfdwhandler()" +
	" RETURNS fdw_handler LANGUAGE pljavafdwhandler" +
	" AS 'org.postgresql.pljava.example.fdw.DummyFDW'",

	"CREATE FUNCTION javatest.dummyfdwvalidator(text[], oid)" +
	" RETURNS void LANGUAGE pljavafdwhandler" +
	" AS 'org.postgresql.pljava.example.fdw.DummyFDW'",

	"CREATE FOREIGN DATA WRAPPER pljavadummy" +
	" HANDLER javatest.dummyfdwhandler" +
	" VALIDATOR javatest.dummyfdwvalidator"
}, remove={
	"DROP FOREIGN DATA WRAPPER pljavadummy",
	"DROP FUNCTION javatest.dummyfdwvalidator(text[], oid)",
	"DROP FUNCTION javatest.dummyfdwhandler()"
})
public class DummyFDW implements PLJavaBasedFDW
{
	public DummyFDW()
	{
	}

	@Override
	public void validateWrapperOptions(Map<Simple,String> opts)
	throws SQLException
	{
		System.out.println("WrapperOptions: " + opts);
	}

	@Override
	public void validateServerOptions(Map<Simple,String> opts)
	throws SQLException
	{
		System.out.println("ServerOptions: " + opts);
	}

	@Override
	public void validateTableOptions(Map<Simple,String> opts)
	throws SQLException
	{
		System.out.println("TableOptions: " + opts);
	}

	@Override
	public void validateAttributeOptions(Map<Simple,String> opts)
	throws SQLException
	{
		System.out.println("AttributeOptions: " + opts);
	}

	@Override
	public void validateUserMappingOptions(Map<Simple,String> opts)
	throws SQLException
	{
		System.out.println("UserMappingOptions: " + opts);
	}

	@Override
	public void getRelSize(
		PlannerInfo root, RelOptInfo baserel, RegClass foreigntable)
	throws SQLException
	{
	}

	@Override
	public void getPaths(
		PlannerInfo root, RelOptInfo baserel, RegClass foreigntable)
	throws SQLException
	{
	}

	@Override
	public ForeignScan getPlan(
		PlannerInfo root, RelOptInfo baserel, RegClass foreigntable,
		ForeignPath best_path, List<TBD> tlist, List<TBD> scan_clauses,
		Plan outer_plan)
	throws SQLException
	{
		return null;
	}

	@Override
	public void beginScan(ForeignScanState node, TBD eflags)
	throws SQLException
	{
	}

	@Override
	public TupleTableSlot iterateScan(ForeignScanState node)
	throws SQLException
	{
		return null;
	}

	@Override
	public void rescan(ForeignScanState node)
	throws SQLException
	{
	}

	@Override
	public void endScan(ForeignScanState node)
	throws SQLException
	{
	}
}
