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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;

import org.postgresql.pljava.jdbc.SQLUtils;

/**
 * Group of methods intended to streamline the PL/Java installation/startup
 * experience.
 *
 * @author Chapman Flack
 */
public class InstallHelper
{
	private static void setPropertyIfNull( String property, String value)
	{
		if ( null == System.getProperty( property) )
			System.setProperty( property, value);
	}

	public static String hello(
		String nativeVer, String user,
		String datadir, String libdir, String sharedir, String etcdir)
	{
		String implVersion =
			InstallHelper.class.getPackage().getImplementationVersion();
		System.setProperty( "user.name", user);
		setPropertyIfNull( "org.postgresql.datadir", datadir);
		setPropertyIfNull( "org.postgresql.libdir", libdir);
		setPropertyIfNull( "org.postgresql.sharedir", sharedir);
		setPropertyIfNull( "org.postgresql.etcdir", etcdir);
		setPropertyIfNull( "org.postgresql.pljava.version", implVersion);
		setPropertyIfNull( "org.postgresql.pljava.native.version", nativeVer);
		setPropertyIfNull( "org.postgresql.version",
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

	public static void groundwork( String module_pathname)
	throws SQLException
	{
		Connection c = null;
		Statement s = null;
		try
		{
			c = SQLUtils.getDefaultConnection();
			s = c.createStatement();

			schema(c, s);
		}
		finally
		{
			SQLUtils.close(s);
			SQLUtils.close(c);
		}
	}

	private static void schema( Connection c, Statement s)
	throws SQLException
	{
		Savepoint p = null;
		try
		{
			p = c.setSavepoint();
			s.execute("CREATE SCHEMA sqlj");
			s.execute("GRANT USAGE ON SCHEMA sqlj TO public");
			c.releaseSavepoint(p);
		}
		catch ( SQLException sqle )
		{
			if ( ! "42P06".equals(sqle.getSQLState()) )
				throw sqle;
			c.rollback(p); // schema exists already, no problem
		}
	}
}
