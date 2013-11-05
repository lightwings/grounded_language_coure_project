/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework
 * <p>
 * Copyright (C) 2013 Yoav Artzi
 * <p>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 ******************************************************************************/
package edu.uw.cs.lil.tiny.mr.lambda;

import java.util.Map;

import edu.uw.cs.lil.tiny.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Lambda calculus variable.
 * 
 * @author Yoav Artzi
 */
public class Variable extends Term {
	public static final String		PREFIX				= "$";
	private static final ILogger	LOG					= LoggerFactory
																.create(Variable.class);
	private static final long		serialVersionUID	= -2489052410662325680L;
	
	public Variable(Type type) {
		super(type);
	}
	
	protected static Variable doParse(String string,
			Map<String, Variable> variables, TypeRepository typeRepository) {
		try {
			final String[] split = string.split(Term.TYPE_SEPARATOR);
			if (split.length == 2) {
				// Case of variable definition
				final Type type = typeRepository
						.getTypeCreateIfNeeded(split[1]);
				if (type == null) {
					throw new LogicalExpressionRuntimeException(
							"Invalid type for variable: " + string);
				}
				if (variables.containsKey(split[0])) {
					throw new LogicalExpressionRuntimeException(
							"Variable overwrite is not supported, please supply unique names: "
									+ string);
				}
				final Variable variable = new Variable(type);
				variables.put(split[0], variable);
				return variable;
			} else {
				// Case variable reference
				if (variables.containsKey(string)) {
					return variables.get(string);
				} else {
					throw new LogicalExpressionRuntimeException(
							"Undefined variable reference: " + string);
				}
			}
		} catch (final RuntimeException e) {
			LOG.error("Variable error: %s", string);
			throw e;
		}
		
	}
	
	@Override
	public void accept(ILogicalExpressionVisitor visitor) {
		visitor.visit(this);
	}
	
	@Override
	public boolean equals(Object obj) {
		// Without variable mapping, variables are only equal when they are
		// identical
		return this == obj;
	}
	
	@Override
	protected boolean doEquals(Object obj,
			Map<Variable, Variable> variablesMapping) {
		if (variablesMapping.containsKey(this)) {
			// Comparison through mapping of variables
			return variablesMapping.get(this) == obj;
		} else if (!variablesMapping.containsKey(this)
				&& !variablesMapping.values().contains(obj)) {
			// Case both are not mapped, do instance comparison for free
			// variables
			return obj == this;
		} else {
			// Not equal
			return false;
		}
	}
}
