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

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.text.ParseException;
import java.util.Scanner;

import org.postgresql.pljava.jdbc.SQLUtils;
import org.postgresql.pljava.management.SQLDeploymentDescriptor;
import static org.postgresql.pljava.sqlgen.DDRWriter.eQuote;

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
		setPropertyIfNull( "java.awt.headless", "true");
		setPropertyIfNull( "org.postgresql.datadir", datadir);
		setPropertyIfNull( "org.postgresql.libdir", libdir);
		setPropertyIfNull( "org.postgresql.sharedir", sharedir);
		setPropertyIfNull( "org.postgresql.etcdir", etcdir);
		setPropertyIfNull( "org.postgresql.pljava.version", implVersion);
		setPropertyIfNull( "org.postgresql.pljava.native.version", nativeVer);
		setPropertyIfNull( "org.postgresql.version",
			Backend.getConfigOption( "server_version"));
		/*
		 * As stipulated by JRT-2003
		 */
		setPropertyIfNull( "sqlj.defaultconnection", "jdbc:default:connection");

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
		sb.append( "PL/Java native code (").append( nativeVer).append( ")\n");
		sb.append( "PL/Java common code (").append( implVersion).append( ")\n");
		sb.append( jreName).append( " (").append( jreVer).append( ")\n");
		sb.append( vmName).append( " (").append( vmVer);
		if ( null != vmInfo )
			sb.append( ", ").append( vmInfo);
		sb.append( ')');
		return sb.toString();
	}

	public static void groundwork( String module_pathname)
	throws SQLException, ParseException
	{
		Connection c = null;
		Statement s = null;
		try
		{
			c = SQLUtils.getDefaultConnection();
			s = c.createStatement();

			schema(c, s);
			handlers(c, s, module_pathname);
			languages(c, s);
			deployment(c, s);
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
			c.rollback(p);
			if ( ! "42P06".equals(sqle.getSQLState()) )
				throw sqle;
		}
	}

	private static void handlers( Connection c, Statement s, String module_path)
	throws SQLException
	{
		s.execute(
			"CREATE OR REPLACE FUNCTION sqlj.java_call_handler()" +
			" RETURNS language_handler" +
			" AS " + eQuote(module_path) +
			" LANGUAGE C");
		s.execute("REVOKE ALL PRIVILEGES" +
			" ON FUNCTION sqlj.java_call_handler() FROM public");

		s.execute(
			"CREATE OR REPLACE FUNCTION sqlj.javau_call_handler()" +
			" RETURNS language_handler" +
			" AS " + eQuote(module_path) +
			" LANGUAGE C");
		s.execute("REVOKE ALL PRIVILEGES" +
			" ON FUNCTION sqlj.javau_call_handler() FROM public");
	}

	private static void languages( Connection c, Statement s)
	throws SQLException
	{
		Savepoint p = null;
		try
		{
			p = c.setSavepoint();
			s.execute(
				"CREATE TRUSTED LANGUAGE java HANDLER sqlj.java_call_handler");
			c.releaseSavepoint(p);
		}
		catch ( SQLException sqle )
		{
			c.rollback(p);
			if ( ! "42710".equals(sqle.getSQLState()) )
				throw sqle;
		}
		try
		{
			p = c.setSavepoint();
			s.execute(
				"CREATE LANGUAGE javaU HANDLER sqlj.javau_call_handler");
			c.releaseSavepoint(p);
		}
		catch ( SQLException sqle )
		{
			c.rollback(p);
			if ( ! "42710".equals(sqle.getSQLState()) )
				throw sqle;
		}
	}

	private static void deployment( Connection c, Statement s)
	throws SQLException, ParseException
	{
		InputStream is = InstallHelper.class.getResourceAsStream("/pljava.ddr");
		String raw = new Scanner(is, "utf-8").useDelimiter("\\A").next();
		SQLDeploymentDescriptor sdd = new SQLDeploymentDescriptor(raw);
		sdd.install(c);
	}

	private static final SchemaVariant currentSchema =
		SchemaVariant.UNREL20130301b;

	private enum SchemaVariant
	{
		UNREL20130301b ("c51cffa34acd5a228325143ec29563174891a873"),
		UNREL20130301a ("624d78ca98d80ff2ded215eeca92035da5126bc0"),
		REL_1_3_0      ("d23804a7e1154de58181a8aa48bfbbb2c8adf68b"),
		UNREL20060212  ("671eadf7f13a7996af31f1936946bf6677ecdc73"),
		UNREL20060125  ("8afd33ccb8a2a56e92dee9c9ced81185ff0bb34d"),
		REL_1_1_0      ("039db412fa91a23b67ceb8d90d30bc540fef7c5d"),
		REL_1_0_0      ("94e23ba02b55e8008a935fcf3e397db0adb4671b"),
		UNREL20040121  ("67eea979bcd4575f285c30c581fd0d674c13c1fa"),
		UNREL20040120  ("5e4131738cd095b7ff6367d64f809f6cec6a7ba7");

		String sha;
		SchemaVariant( String sha)
		{
			this.sha = sha;
		}
	}
}
