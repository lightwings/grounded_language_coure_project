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
package edu.uw.cs.lil.tiny.parser.ccg.rules.coordination;

import java.util.ArrayList;
import java.util.List;

import edu.uw.cs.lil.tiny.parser.ccg.rules.BinaryRulesSet;
import edu.uw.cs.lil.tiny.parser.ccg.rules.IBinaryParseRule;

public class CoordinationRule<Y> extends BinaryRulesSet<Y> {
	
	private CoordinationRule(List<IBinaryParseRule<Y>> rules) {
		super(rules);
	}
	
	public static <Y> CoordinationRule<Y> create(
			ICoordinationServices<Y> services) {
		final List<IBinaryParseRule<Y>> rules = new ArrayList<IBinaryParseRule<Y>>(
				3);
		
		rules.add(new C1Rule<Y>(services));
		rules.add(new C2Rule<Y>(services));
		rules.add(new CXRule<Y>(services));
		
		return new CoordinationRule<Y>(rules);
	}
	
}