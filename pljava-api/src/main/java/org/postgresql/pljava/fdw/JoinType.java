/*
 * Copyright (c) 2025 Tada AB and other contributors, as listed below.
 * Portions Copyright (c) 1996-2025, PostgreSQL Global Development Group
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

/**
 * Indicates the exact semantics of joining two relations using a matching
 * qualification, such as what to do with a tuple that has no match in the
 * other relation.
 */
public enum JoinType
{
	INNER, LEFT, FULL, RIGHT,
	SEMI, ANTI, RIGHT_SEMI, RIGHT_ANTI,
	UNIQUE_OUTER, UNIQUE_INNER
}
