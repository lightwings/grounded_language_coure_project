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

import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;

/**
 * Get all the literals from an expression of a specific arity
 * 
 * @author Yoav Artzi
 */
public class GetAllLiterals implements ILogicalExpressionVisitor {
	private final Integer	arity;
	final List<Literal>		literals	= new LinkedList<Literal>();
	
	/**
	 * Usage only through static 'of' method
	 * 
	 * @param arity
	 */
	private GetAllLiterals(Integer arity) {
		this.arity = arity;
		
	}
	
	public static List<Literal> of(LogicalExpression exp) {
		return of(exp, null);
	}
	
	public static List<Literal> of(LogicalExpression exp, Integer arity) {
		final GetAllLiterals visitor = new GetAllLiterals(arity);
		visitor.visit(exp);
		return visitor.getLiterals();
	}
	
	public List<Literal> getLiterals() {
		return literals;
	}
	
	@Override
	public void visit(Lambda lambda) {
		lambda.getBody().accept(this);
	}
	
	@Override
	public void visit(Literal literal) {
		if (arity == null || literal.getArguments().size() == arity) {
			literals.add(literal);
		}
		for (final LogicalExpression exp : literal.getArguments()) {
			exp.accept(this);
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
		// Nothing to do
	}
	
}
