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
package edu.uw.cs.lil.tiny.mr.lambda.visitor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;

/**
 * Checks if a given logical expression has any free variables.
 * 
 * @author Yoav Artzi
 */
public class HasFreeVariables implements ILogicalExpressionVisitor {
	private final Set<Variable>	bindedVariable	= new HashSet<Variable>();
	private boolean				result			= false;
	
	public static boolean of(LogicalExpression exp) {
		final HasFreeVariables visitor = new HasFreeVariables();
		visitor.visit(exp);
		
		return visitor.result;
	}
	
	@Override
	public void visit(Lambda lambda) {
		bindedVariable.add(lambda.getArgument());
		lambda.getBody().accept(this);
	}
	
	@Override
	public void visit(Literal literal) {
		literal.getPredicate().accept(this);
		final Iterator<? extends LogicalExpression> iterator = literal
				.getArguments().iterator();
		while (!result && iterator.hasNext()) {
			iterator.next().accept(this);
		}
	}
	
	@Override
	public void visit(LogicalConstant logicalConstant) {
		// Nothing to do
		
	}
	
	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}
	
	@Override
	public void visit(Variable variable) {
		result |= !bindedVariable.contains(variable);
	}
	
}
