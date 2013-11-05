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
package edu.uw.cs.lil.tiny.parser.ccg.cky;

import java.util.Collection;

import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Cell;
import edu.uw.cs.lil.tiny.parser.ccg.rules.IBinaryParseRule;
import edu.uw.cs.lil.tiny.parser.ccg.rules.ParseRuleResult;

public class CKYBinaryParsingRule<Y> {
	private final IBinaryParseRule<Y>	ccgParseRule;
	
	public CKYBinaryParsingRule(IBinaryParseRule<Y> ccgParseRule) {
		this.ccgParseRule = ccgParseRule;
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
		@SuppressWarnings("rawtypes")
		final CKYBinaryParsingRule other = (CKYBinaryParsingRule) obj;
		if (ccgParseRule == null) {
			if (other.ccgParseRule != null) {
				return false;
			}
		} else if (!ccgParseRule.equals(other.ccgParseRule)) {
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((ccgParseRule == null) ? 0 : ccgParseRule.hashCode());
		return result;
	}
	
	/**
	 * Takes two cell, left and right, as input. Assumes these cells are
	 * adjacent.
	 */
	protected Collection<ParseRuleResult<Y>> apply(Cell<Y> left, Cell<Y> right,
			boolean isCompleteSentence) {
		return ccgParseRule.apply(left.getCategroy(), right.getCategroy(),
				isCompleteSentence);
	}
}
