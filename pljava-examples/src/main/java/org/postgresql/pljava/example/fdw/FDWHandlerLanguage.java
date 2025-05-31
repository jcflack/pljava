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

import java.lang.reflect.Constructor;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isPublic;

import java.sql.Connection;
import static java.sql.DriverManager.getConnection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLNonTransientException;

import java.util.List;
import java.util.Map;

import java.util.regex.Pattern;

import org.postgresql.pljava.Adapter.As;
import org.postgresql.pljava.Adapter.AsInt;
import org.postgresql.pljava.PLJavaBasedLanguage.Routines;
import org.postgresql.pljava.PLJavaBasedLanguage.Template;

import org.postgresql.pljava.annotation.SQLAction;

import org.postgresql.pljava.fdw.PLJavaBasedFDW;

import org.postgresql.pljava.model.Attribute;
import org.postgresql.pljava.model.ProceduralLanguage;
import org.postgresql.pljava.model.ProceduralLanguage.PLJavaBased;
import static org.postgresql.pljava.model.RegNamespace.PG_CATALOG;
import org.postgresql.pljava.model.RegProcedure;
import org.postgresql.pljava.model.RegType;
import static org.postgresql.pljava.model.RegType.OID;
import static org.postgresql.pljava.model.RegType.TEXT;
import static org.postgresql.pljava.model.RegType.VOID;
import org.postgresql.pljava.model.TupleTableSlot;

import org.postgresql.pljava.model.SlotTester; // temporary hack

import org.postgresql.pljava.sqlgen.Lexicals.Identifier.Simple;

/**
 * Implements a {@code pljavafdwhandler} language that is only usable for
 * declaring FDW handler and validator functions.
 *<p>
 * This 'language', and the {@code SQLAction} annotation actions that declare
 * it, ultimately will be incorporated into PL/Java's install actions and no
 * longer needed here.
 *<p>
 * A valid FDW handler function must be declared with no arguments and a return
 * type of {@code fdw_handler}.
 *<p>
 * A valid FDW validator function must be declared with two arguments: a text
 * array and an oid. PostgreSQL ignores the declared return type and does not
 * specify any; here it will be required to be void.
 * @author Chapman Flack
 */
@SQLAction(requires="pljavahandler language", provides="pljavafdwhandler",
install={
"CREATE OR REPLACE FUNCTION sqlj.pljavaFDWHandlerValidator(oid)" +
" RETURNS pg_catalog.void LANGUAGE pljavahandler" +
" AS 'org.postgresql.pljava.example.fdw.FDWHandlerLanguage'",

"COMMENT ON FUNCTION sqlj.pljavaFDWHandlerValidator(oid) IS " +
"'The validator function for the \"PL/Java FDW handler\" language (in which one " +
"can only write functions that are validators or handlers of foreign data " +
"wrappers implemented atop PL/Java).'",

"CREATE LANGUAGE pljavafdwhandler" +
" HANDLER sqlj.pljavaDispatchRoutine" +
" INLINE  sqlj.pljavaDispatchInline" +
" VALIDATOR sqlj.pljavaFDWHandlerValidator",

"COMMENT ON LANGUAGE pljavafdwhandler IS " +
"'The PL/Java \"FDW handler language\", used in implementing foreign data " +
"wrappers atop PL/Java. Only two kinds of function can be written in this " +
"\"language\", namely, an FDW validator and an FDW handler, and the AS " +
"string for each is simply the name of a Java class that must implement " +
"PLJavaBasedFDW, and that class will be used as the implementation of " +
"the new foreign data wrapper.'"
}, remove={
"DROP LANGUAGE pljavafdwhandler",
"DROP FUNCTION sqlj.pljavaFDWHandlerValidator(oid)"
})
public class FDWHandlerLanguage implements Routines
{
	private final ProceduralLanguage pl;

	public FDWHandlerLanguage(ProceduralLanguage pl)
	{
		this.pl = pl;
	}

	private static final Simple FDW_HANDLER = Simple.fromJava("fdw_handler");

	private static final List<RegType> HANDLER_ARGS = List.of();

	private static final List<RegType> VALIDATOR_ARGS =
		List.of(TEXT.array(), OID);

	private boolean isFDWHandler(RegType t)
	{
		return t.namespace() == PG_CATALOG && FDW_HANDLER.equals(t.name());
	}

	private static final Pattern CLASSNAME = Pattern.compile(String.format(
		"(?:\\p{%1$sStart}\\p{%1$sPart}*+(?:\\.(?!$))?+)++",
		"javaJavaIdentifier"));

	/*
	 * Until there is an adapter manager implemented, getting adapters for
	 * needed data types remains hacky.
	 */
	private static final AsInt<?> ADP_OID;
	private static final As<List<String>,?> ADP_ARRTEXT;
	static
	{
		try ( Connection conn = getConnection("jdbc:default:connection") )
		{
			SlotTester t = conn.unwrap(SlotTester.class);

			ADP_OID = (AsInt<?>)t.adapterPlease(
				"org.postgresql.pljava.pg.adt.OidAdapter", "INT4_INSTANCE");

			@SuppressWarnings("unchecked") Object _1 =
			ADP_ARRTEXT = (As<List<String>,?>)t.adapterPlease(
				"org.postgresql.pljava.pg.adt.ArrayAdapter",
				"FLAT_STRING_LIST_INSTANCE");
		}
		catch ( SQLException | ReflectiveOperationException e )
		{
			throw new ExceptionInInitializerError(e);
		}
	}

	/*
	 * Just copy these magic numbers until this code moves to the internal
	 * module.
	 */
	private static final int AttributeRelationId          = 1249;
	private static final int ForeignDataWrapperRelationId = 2328;
	private static final int ForeignServerRelationId      = 1417;
	private static final int ForeignTableRelationId       = 3118;
	private static final int UserMappingRelationId        = 1418;

	/**
	 * Validates that <var>subject</var> has the proper form for an FDW handler
	 * or an FDW validator function.
	 */
	@Override
	public void essentialChecks(
		RegProcedure<?> subject, PLJavaBased memo, boolean checkBody)
	throws SQLException
	{
		boolean sigOK = false;

		List<RegType> argTypes = subject.argTypes();
		RegType returnType = subject.returnType();

		if ( null != subject.allArgTypes() )
			throw new SQLNonTransientException(String.format(
				"%s must not have arguments of mode other than IN", subject),
				"42P13");

		if ( HANDLER_ARGS.equals(argTypes) && isFDWHandler(returnType) )
			sigOK = true;
		else if ( VALIDATOR_ARGS.equals(argTypes) )
		{
			if ( VOID != returnType )
				throw new SQLNonTransientException(String.format(
				"%s must have void return type as an FDW validator", subject),
				"42P13");
			sigOK = true;
		}

		if ( ! sigOK )
			throw new SQLNonTransientException(String.format(
			"%s does not have proper signature for an FDW handler or validator",
			subject), "42P13");

		String src = subject.src();

		if ( ! CLASSNAME.matcher(src).matches() )
			throw new SQLNonTransientException(String.format(
			"%s AS value is not a valid Java class name", subject),
			"42P13");

		if ( ! checkBody )
			return;

		Class<?> cls;

		try
		{
			cls = Class.forName(src);
		}
		catch ( ClassNotFoundException e )
		{
			throw new SQLNonTransientException(String.format(
				"%s implementing class %s not found", subject, src),
				"42P13", e);
		}

		int mods = cls.getModifiers();

		if ( ! PLJavaBasedFDW.class.isAssignableFrom(cls)
			|| cls.isInterface() || ! isPublic(mods) || isAbstract(mods) )
			throw new SQLNonTransientException(String.format(
				"%s implementing class %s is not a public, non-abstract class" +
				" implementing PLJavaBasedFDW", subject, src), "42P13");

		try
		{
			cls.getConstructor();
		}
		catch ( NoSuchMethodException e )
		{
			throw new SQLNonTransientException(String.format(
				"%s implementing class %s public no-arg constructor not found",
				subject, src), "42P13", e);
		}
	}

	/**
	 * Returns a {@code Template} to execute an FDW validator or handler.
	 */
	@Override
	public Template prepare(RegProcedure<?> target, PLJavaBased memo)
	throws SQLException
	{
		PLJavaBasedFDW fdw;

		try
		{
			Class<? extends PLJavaBasedFDW> cls =
				Class.forName(target.src()).asSubclass(PLJavaBasedFDW.class);

			/*
			 * Instantiating the class here at prepare() time entails deciding
			 * that the instance itself should have no state that encapsulates
			 * *which* ForeignDataWrapper it represents, as there are no clues
			 * available, at the time a handler is called, as to which FDW it is
			 * being called for. The class must, therefore, be an umbrella class
			 * equally representative of any and all FDWs declared with it for
			 * a handler.
			 *
			 * That's the right call, because the instance is needed when
			 * dispatching validator calls, and those come with no indication of
			 * which specific object the options being validated are for.
			 *
			 * Any per-wrapper/server/connection/etc. state that is worth saving
			 * will need other subordinate classes to hold it.
			 *
			 * It's convenient to instantiate the class here in the same try
			 * block as the class lookup, though we haven't yet decided whether
			 * we are preparing a validator or handler template. The validator
			 * will definitely need the instance, to call validator instance
			 * methods on it; the handler might not end up needing the instance
			 * at all. More ruminations below at: // it's a handler.
			 */
			fdw = cls.getConstructor().newInstance();
		}
		catch ( ReflectiveOperationException e )
		{
			throw new SQLException(target.toString(), e); // shouldn't happen!
		}

		if ( target.argTypes().isEmpty() ) // it's a handler.
		{
			/*
			 * Anything done where this comment is, is done at prepare() time
			 * and cached for the lifespan of the handler RegProcedure. This is
			 * a good place to do pretty much everything needed here, because
			 *
			 * 1. it can all be precomputed (the handler takes no arguments, so
			 *    there will be no new info at specialize() or call() time) and
			 *
			 * 2. PostgreSQL makes weirdly heavy use of an FDW handler function,
			 *    calling it to get a struct of function pointers, throwing all
			 *    that away when done with the instant query, then calling the
			 *    handler again the next time the (probably precomputed and
			 *    completely unchanging) info is needed again. Surprising, but
			 *    that's what it does.
			 *
			 * So deferring anything to specialize() or call() time below that
			 * could be done here at prepare() time would be hard to explain.
			 *
			 * As for exactly what to precompute here, we've got options. Could
			 * use FFM to generate upcall stubs with the class instance bound
			 * in, and populate a prototype FdwRoutine struct with those. All
			 * that would be needed at call() time would be to copy that into
			 * palloc()d space to return.
			 *
			 * Or, there could be a static prototype FdwRoutine struct in the C
			 * code, populated with fixed pointers to C functions that will
			 * handle dispatching when called. All that would need to be
			 * computed here would be which optional interfaces/methods cls
			 * supports.
			 *
			 * Why is that needed? Because PostgreSQL looks at which FdwRoutine
			 * function pointers are null/not null to learn the capabilities of
			 * the FDW. So a simple approach here at prepare() time would be to
			 * reflect on cls, determine which optional interfaces it does
			 * and doesn't implement, and compute a bitmapm say, corresponding
			 * to the FdwRoutine function slots. All that would need to happen
			 * at call() time would be to pass the bits to a little C helper
			 * that will palloc0() space for an FdwRoutine struct and copy
			 * just the indicated pointers into it from the prototype.
			 */
			return flinfo -> fcinfo ->
			{
				/*
				 * Anything done where this comment is, is done at call() time.
				 * It shouldn't be much, because PostgreSQL calls FDW handler
				 * functions more than you'd think. See the prepare-time
				 * comments above.
				 *
				 * Note that, even at handler call time, we get *no* information
				 * about *which* declared FDW the call is targeting. The handler
				 * needs to return a struct of pointers to functions that can
				 * dispatch work involving *any* FDW declared with the same
				 * handler function. Each of those functions has to be able to
				 * determine, from the arguments it is passed, just which
				 * foreign tables / server / wrapper to dispatch to.
				 */
			};
		}

		/* it's a validator */

		return flinfo -> fcinfo ->
		{
			TupleTableSlot args = fcinfo.arguments();

			/*
			 * Munge the options needing validation from the text[] form
			 * supplied as arg 1 to the Map<Simple,String> form the validate...
			 * methods take.
			 */
			List<String> opts = args.sqlGet(1, ADP_ARRTEXT);
			Map<Simple,String> optm = Map.ofEntries(
				opts.stream().map(s ->
				{
					int pos = s.indexOf('=');
					if ( -1 == pos )
						return Map.entry(Simple.fromCatalog(s), "");
					return
						Map.entry(Simple.fromCatalog(s.substring(0, pos)),
							s.substring(1 + pos));
				})
				.toArray(Map.Entry[]::new));

			/*
			 * Dispatch to the right validate... method based on the oid passed
			 * as arg 2.
			 *
			 * Note that each validate... method must do its job with
			 * far-from-complete information. validateAttributeOptions gets
			 * a bunch of options to decide about without even knowing what
			 * table that column is in, let alone its table options, or even
			 * the type of the column these options are applied to.
			 * validateTableOptions and validateUserMappingOptions have to do
			 * their jobs without a clue what table or user mapping the options
			 * are being put on, let alone what server is responsible for the
			 * mystery table or mapping. validateServerOptions has to validate
			 * those with no idea what server they are for or what FDW it is
			 * to be associated with--and moreover, a server has 'type' and
			 * 'version' properties, also arbitrary strings just like options,
			 * but not passed to the validator at all.
			 *
			 * Evan validateWrapperOptions has to get by with less than full
			 * information. If it were passed the handler RegProc, it could at
			 * least sanity-check that the wrapper declaration has mentioned
			 * handler and validator functions that aren't, say, from completely
			 * unrelated extensions. But no such luck.
			 */
			int oid = args.sqlGet(2, ADP_OID);
			switch ( oid )
			{
			case ForeignDataWrapperRelationId:
				fdw.validateWrapperOptions(optm);
				break;
			case ForeignServerRelationId:
				fdw.validateServerOptions(optm);
				break;
			case ForeignTableRelationId:
				fdw.validateTableOptions(optm);
				break;
			case AttributeRelationId:
				fdw.validateAttributeOptions(optm);
				break;
			case UserMappingRelationId:
				fdw.validateUserMappingOptions(optm);
				break;
			default:
				throw new SQLFeatureNotSupportedException(
					"validate FDW options with relation id " + oid);
			}
		};
	}
}
