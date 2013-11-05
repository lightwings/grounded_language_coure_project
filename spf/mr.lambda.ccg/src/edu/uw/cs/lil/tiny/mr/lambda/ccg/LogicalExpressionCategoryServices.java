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
package edu.uw.cs.lil.tiny.mr.lambda.ccg;

import edu.uw.cs.lil.tiny.ccg.categories.AbstractCategoryServices;
import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.SimpleCategory;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.ApplyAndSimplify;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.IsValid;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.IsWellTyped;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.Simplify;
import edu.uw.cs.lil.tiny.mr.language.type.ComplexType;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

public class LogicalExpressionCategoryServices extends
		AbstractCategoryServices<LogicalExpression> {
	private static final ILogger				LOG					= LoggerFactory
																			.create(LogicalExpressionCategoryServices.class);
	
	private final boolean						doTypeChecking;
	private final Category<LogicalExpression>	EMP					= new SimpleCategory<LogicalExpression>(
																			Syntax.EMPTY,
																			null);
	private final Category<LogicalExpression>	EMPTY_CATEGORY_NP	= new SimpleCategory<LogicalExpression>(
																			Syntax.NP,
																			null);
	
	private final Category<LogicalExpression>	EMPTY_CATEGORY_S	= new SimpleCategory<LogicalExpression>(
																			Syntax.S,
																			null);
	
	private final boolean						lockOntology;
	
	private final boolean						validateLogExps;
	
	public LogicalExpressionCategoryServices() {
		this(false, false, false);
	}
	
	public LogicalExpressionCategoryServices(boolean doTypeChecking,
			boolean lockOntology) {
		this(doTypeChecking, lockOntology, false);
	}
	
	public LogicalExpressionCategoryServices(boolean doTypeChecking,
			boolean lockOntology, boolean validateLogExps) {
		this.doTypeChecking = doTypeChecking;
		this.lockOntology = lockOntology;
		this.validateLogExps = validateLogExps;
	}
	
	@Override
	public LogicalExpression doSemanticApplication(LogicalExpression function,
			LogicalExpression argument) {
		final LogicalExpression result;
		
		// Combined application and simplification
		final LogicalExpression applicationResult = ApplyAndSimplify.of(
				function, argument);
		// Verify application result is well typed, only if verification is
		// turned on
		if (applicationResult != null && doTypeChecking
				&& !IsWellTyped.of(applicationResult)) {
			result = null;
		} else {
			result = applicationResult;
		}
		
		if (result != null && validateLogExps && !IsValid.of(result)) {
			LOG.error("Application result invalid");
			LOG.error("function=%s", function);
			LOG.error("arg=%s", argument);
			LOG.error("result=%s", result);
			throw new IllegalStateException(
					"Invalid logical expression detected");
		}
		
		return result;
	}
	
	/**
	 * Function composition
	 * 
	 * @param f
	 * @param g
	 * @return \lambda x .f(g(x))
	 */
	@Override
	public LogicalExpression doSemanticComposition(LogicalExpression f,
			LogicalExpression g) {
		
		if (!(f.getType().isComplex() && g.getType().isComplex())) {
			return null;
		}
		
		// Function composition
		final ComplexType fType = (ComplexType) f.getType();
		final ComplexType gType = (ComplexType) g.getType();
		
		if (!gType.getRange().isExtendingOrExtendedBy(fType.getDomain())) {
			return null;
		}
		
		// Make a new variable x. Generalization is required in the case g is
		// not a lambda expression, so its type was not generalized.
		final Variable x = new Variable(LogicLanguageServices
				.getTypeRepository().generalizeType(gType.getDomain()));
		
		final LogicalExpression gBodyWithNewVar = ApplyAndSimplify.of(g, x);
		if (gBodyWithNewVar != null) {
			final LogicalExpression newbody = ApplyAndSimplify.of(f,
					gBodyWithNewVar);
			if (newbody != null) {
				final LogicalExpression newComposedExp = new Lambda(x, newbody);
				// Do type checking, if verification is turned on
				if (doTypeChecking && !IsWellTyped.of(newComposedExp)) {
					return null;
				} else {
					// If gBodyWithNewVar is a variable (such as will happen
					// when g is the identity function), it is possible that we
					// need to simplify, since the simplify code can fold and
					// drop Lambda operators under certain conditions. The same
					// is true for the cases where newbody is identical to
					// gBodyWithNewVar (such as the case when f is the identity
					// function).
					// See AbstractSimplify.visit(Lambda).
					
					final LogicalExpression result = gBodyWithNewVar instanceof Variable
							|| gBodyWithNewVar == newbody ? Simplify
							.of(newComposedExp) : newComposedExp;
					
					if (result != null && validateLogExps
							&& !IsValid.of(result)) {
						LOG.error("Composition result invalid");
						LOG.error("g=%s", g);
						LOG.error("f=%s", f);
						LOG.error("result=%s", result);
						throw new IllegalStateException(
								"Invalid logical expression detected");
					}
					
					return result;
				}
			}
		}
		
		// Case composition failed
		return null;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final LogicalExpressionCategoryServices other = (LogicalExpressionCategoryServices) obj;
		if (doTypeChecking != other.doTypeChecking) {
			return false;
		}
		return true;
	}
	
	@Override
	public Category<LogicalExpression> getEmptyCategory() {
		return EMP;
	}
	
	@Override
	public Category<LogicalExpression> getNounPhraseCategory() {
		return EMPTY_CATEGORY_NP;
	}
	
	@Override
	public Category<LogicalExpression> getSentenceCategory() {
		return EMPTY_CATEGORY_S;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (doTypeChecking ? 1231 : 1237);
		return result;
	}
	
	@Override
	public LogicalExpression parseSemantics(String string, boolean checkType) {
		final LogicalExpression exp = LogicalExpression.parse(string,
				lockOntology);
		if (checkType && !IsWellTyped.of(exp)) {
			throw new IllegalStateException("Semantics not well typed: "
					+ string);
		}
		return Simplify.of(exp);
	}
	
	@Override
	public String toString() {
		return LogicalExpressionCategoryServices.class.getName();
	}
}
