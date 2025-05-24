/*
 * Copyright (c) 2025 Tada AB and other contributors, as listed below.
 * Portions Copyright (c) 2011-2025, PostgreSQL Global Development Group
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the The BSD 3-Clause License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Contributors:
 *   Chapman Flack
 */
package org.postgresql.pljava.fdw;

import java.sql.SQLException;

import java.util.List;
import java.util.Map;

import org.postgresql.pljava.model.RegClass;
import org.postgresql.pljava.model.TupleTableSlot;

import org.postgresql.pljava.sqlgen.Lexicals.Identifier.Simple;

/**
 * Interface to be implemented by a foreign data wrapper built in PL/Java.
 * @author Chapman Flack
 */
public interface PLJavaBasedFDW
{
	/**
	 * Returns normally if all <var>opts</var> are valid in a
	 * {@code CREATE FOREIGN DATA WRAPPER} command making an instance of this
	 * wrapper, otherwise throwing a suitable exception.
	 */
	void validateWrapperOptions(Map<Simple,String> opts) throws SQLException;

	/**
	 * Returns normally if all <var>opts</var> are valid in a
	 * {@code CREATE SERVER} command using an instance of this
	 * wrapper, otherwise throwing a suitable exception.
	 */
	void validateServerOptions(Map<Simple,String> opts) throws SQLException;

	/**
	 * Returns normally if all <var>opts</var> are valid as table options in a
	 * {@code CREATE FOREIGN TABLE} command for a foreign server using this
	 * wrapper, otherwise throwing a suitable exception.
	 */
	void validateTableOptions(Map<Simple,String> opts) throws SQLException;

	/**
	 * Returns normally if all <var>opts</var> are valid as column options in a
	 * {@code CREATE FOREIGN TABLE} command for a foreign server using this
	 * wrapper, otherwise throwing a suitable exception.
	 */
	void validateAttributeOptions(Map<Simple,String> opts) throws SQLException;

	/**
	 * Returns normally if all <var>opts</var> are valid in a
	 * {@code CREATE USER MAPPING} command for a foreign server using this
	 * wrapper, otherwise throwing a suitable exception.
	 */
	void validateUserMappingOptions(Map<Simple,String> opts)
	throws SQLException;

	/**
	 * Obtains relation size estimates for a foreign table.
	 *<p>
	 * Obscurely but crucially, this method is invoked first in any operation
	 * using the foreign data wrapper, and is able to use an {@code fdw_private}
	 * field on <var>baserel</var> to retain wrapper information for methods
	 * invoked later. (Those details might never be exposed in this public API,
	 * but rather handled behind the scenes in PL/Java's implementation.)
	 * @see <a href='https://www.postgresql.org/docs/17/fdw-planning.html'>
	 * Foreign Data Wrapper Query Planning</a>
	 */
	void getRelSize(
		PlannerInfo root, RelOptInfo baserel, RegClass foreigntable)
	throws SQLException;

	void getPaths(
		PlannerInfo root, RelOptInfo baserel, RegClass foreigntable)
	throws SQLException;

	ForeignScan getPlan(
		PlannerInfo root, RelOptInfo baserel, RegClass foreigntable,
		ForeignPath best_path, List<TBD> tlist, List<TBD> scan_clauses,
		Plan outer_plan)
	throws SQLException;

	void beginScan(ForeignScanState node, TBD eflags)
	throws SQLException;

	TupleTableSlot iterateScan(ForeignScanState node)
	throws SQLException;

	void rescan(ForeignScanState node)
	throws SQLException;

	void endScan(ForeignScanState node)
	throws SQLException;

	/**
	 * Interface additionally implemented by a foreign data wrapper that can
	 * perform joins remotely (rather than by fetching both tables' data and
	 * doing the join locally).
	 */
	interface Joins extends PLJavaBasedFDW
	{
		void getJoinPaths(PlannerInfo root, RelOptInfo joinrel,
			RelOptInfo outerrel, RelOptInfo innerRel,
			JoinType jointype, JoinPathExtraData extra)
		throws SQLException;
	}

	/**
	 * Interface additionally implemented by a foreign data wrapper that can
	 * perform remote post-scan/join processing, such as remote aggregation.
	 */
	interface PostScanJoinProcessing extends PLJavaBasedFDW
	{
		/* ... */
	}

	/* ... */
}
