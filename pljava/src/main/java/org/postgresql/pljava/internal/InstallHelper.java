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
package org.postgresql.pljava.internal;

/**
 * Group of methods intended to streamline the PL/Java installation/startup
 * experience.
 *
 * @author Chapman Flack
 */
public class InstallHelper
{
	public static String hello(
		String nativeVer, String user, String datadir, String libdir)
	{
		String implVersion =
			InstallHelper.class.getPackage().getImplementationVersion();
		System.setProperty( "user.name", user);
		System.setProperty( "org.postgresql.datadir", datadir);
		System.setProperty( "org.postgresql.libdir", libdir);
		System.setProperty( "org.postgresql.pljava.version", implVersion);
		System.setProperty( "org.postgresql.pljava.native.version", nativeVer);
		System.setProperty( "org.postgresql.version",
			Backend.getConfigOption( "server_version"));

		String jreName = System.getProperty( "java.runtime.name");
		String jreVer = System.getProperty( "java.runtime.version");

		if ( null == jreName || null == jreVer )
		{
			jreName = System.getProperty( "java.vendor");
			jreVer = System.getProperty( "java.version");
		}

		String vmName = System.getProperty( "java.vm.name");
		String vmVer = System.getProperty( "java.vm.version");
		String vmInfo = System.getProperty( "java.vm.info");

		StringBuilder sb = new StringBuilder();
		sb.append( "PL/Java native code: ").append( nativeVer).append( '\n');
		sb.append( "PL/Java common code: ").append( implVersion).append( '\n');
		sb.append( jreName).append( " (").append( jreVer).append( ")\n");
		sb.append( vmName).append( " (").append( vmVer);
		if ( null != vmInfo )
			sb.append( ", ").append( vmInfo);
		sb.append( ')');
		return sb.toString();
	}
}
