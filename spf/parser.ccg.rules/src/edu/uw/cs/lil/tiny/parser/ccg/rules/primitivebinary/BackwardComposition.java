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
package edu.uw.cs.lil.tiny.parser.ccg.rules.primitivebinary;

import java.util.Collection;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;

/**
 * A rule for logical composition. Backward composition rule:
 * <ul>
 * <li>Y\Z X\Y => X\Z</li>
 * </ul>
 * 
 * @author Yoav Artzi
 */
public class BackwardComposition<Y> extends AbstractComposition<Y> {
	private static String	RULE_NAME	= "bcomp";
	
	public BackwardComposition(ICategoryServices<Y> categoryServices) {
		super(RULE_NAME, categoryServices);
	}
	
	public BackwardComposition(ICategoryServices<Y> categoryServices,
			boolean useEisnerNormalForm) {
		super(RULE_NAME, categoryServices, useEisnerNormalForm);
	}
	
	@Override
	public Collection<ParseRuleResult<Y>> apply(Category<Y> left,
			Category<Y> right, boolean isCompleteSentence) {
		return doComposition(right, left, true);
	}
	
	public static class Creator<MR> implements
			IResourceObjectCreator<BackwardComposition<MR>> {
		
		private String	type;
		
		public Creator() {
			this("rule.composition.backward");
		}
		
		public Creator(String type) {
			this.type = type;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public BackwardComposition<MR> create(Parameters params,
				IResourceRepository repo) {
			return new BackwardComposition<MR>(
					(ICategoryServices<MR>) repo
							.getResource(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
					"true".equals(params.get("eisnerNormalForm")));
		}
		
		@Override
		public String type() {
			return type;
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type, BackwardComposition.class)
					.addParam("eisnerNormalForm", "boolean",
							"Use Eisner normal form for composition (default: false).")
					.build();
		}
		
	}
	
}
