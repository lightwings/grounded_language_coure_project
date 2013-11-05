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
package edu.uw.cs.lil.tiny.parser.ccg.cky.genlex.exact;

import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.CKYLexicalStep;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.CKYParseStep;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Cell;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.CellFactory;

/**
 * Cell factory for {@link ExactMarkedCell}.
 * 
 * @author Yoav Artzi
 */
public class ExactMarkAwareCellFactory<MR> extends CellFactory<MR> {
	
	public ExactMarkAwareCellFactory(int sentenceSize) {
		super(sentenceSize);
	}
	
	@Override
	protected Cell<MR> doCreate(CKYLexicalStep<MR> parseStep, int start,
			int end, boolean isCompleteSpan) {
		return new ExactMarkedCell<MR>(parseStep, start, end, isCompleteSpan);
	}
	
	@Override
	protected Cell<MR> doCreate(CKYParseStep<MR> parseStep, int start, int end,
			boolean isCompleteSpan) {
		final int leftGeneratedLexicalEntries = parseStep.numChildren() > 0
				&& (parseStep.getChildCell(0) instanceof ExactMarkedCell) ? ((ExactMarkedCell<MR>) parseStep
				.getChildCell(0)).getNumMarkedLexicalEntries() : 0;
		final int rightGeneratedLexicalEntries = parseStep.numChildren() > 1
				&& (parseStep.getChildCell(1) instanceof ExactMarkedCell) ? ((ExactMarkedCell<MR>) parseStep
				.getChildCell(1)).getNumMarkedLexicalEntries() : 0;
		
		return new ExactMarkedCell<MR>(parseStep, start, end, isCompleteSpan,
				leftGeneratedLexicalEntries + rightGeneratedLexicalEntries);
	}
	
}
