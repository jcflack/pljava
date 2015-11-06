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

extern char const *pljavaLoadPath;

extern char const *pljavaHandlerPath;

extern void pljavaCheckLoadPath();

extern void pljavaCheckHandlerPath(bool trusted, PG_FUNCTION_ARGS);
