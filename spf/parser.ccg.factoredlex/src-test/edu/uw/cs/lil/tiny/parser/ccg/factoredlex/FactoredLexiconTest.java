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
package edu.uw.cs.lil.tiny.parser.ccg.factoredlex;

import junit.framework.Assert;

import org.junit.Test;

import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.FactoredLexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.factored.lambda.LexicalTemplate;
import edu.uw.cs.lil.tiny.mr.lambda.FlexibleTypeComparator;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.ccg.LogicalExpressionCategoryServices;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;

public class FactoredLexiconTest {
	
	private final LogicalExpressionCategoryServices	categoryServices;
	
	public FactoredLexiconTest() {
		// //////////////////////////////////////////
		// Init typing system
		// //////////////////////////////////////////
		
		// Init the logical expression type system
		LogicLanguageServices.setInstance(new LogicLanguageServices.Builder(
				new TypeRepository()).setNumeralTypeName("n")
				.setTypeComparator(new FlexibleTypeComparator()).build());
		
		// //////////////////////////////////////////////////
		// Category services for logical expressions
		// //////////////////////////////////////////////////
		
		this.categoryServices = new LogicalExpressionCategoryServices(true,
				false);
	}
	
	@Test
	public void test() {
		final LexicalEntry<LogicalExpression> e1 = LexicalEntry
				.parse("turn :- S/NP : (lambda $0:e (lambda $1:e (and:<t*,t> (turn:<e,t> $1) (dir:<e,<e,t>> $1 $0))))",
						categoryServices, LexicalEntry.Origin.FIXED_DOMAIN);
		final LexicalEntry<LogicalExpression> e2 = LexicalEntry
				.parse("walk :- S/NP : (lambda $0:e (lambda $1:e (and:<t*,t> (move:<e,t> $1) (dir:<e,<e,t>> $1 $0))))",
						categoryServices, LexicalEntry.Origin.FIXED_DOMAIN);
		System.out.println(e1);
		System.out.println(e2);
		final LexicalTemplate t1 = FactoredLexicon.factor(e1).getTemplate();
		System.out.println(t1);
		final LexicalTemplate t2 = FactoredLexicon.factor(e2).getTemplate();
		System.out.println(t2);
		Assert.assertEquals(t1, t2);
	}
	
}
