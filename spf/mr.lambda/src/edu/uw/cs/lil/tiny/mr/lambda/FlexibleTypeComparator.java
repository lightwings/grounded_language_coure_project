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

import edu.uw.cs.lil.tiny.mr.language.type.Type;

/**
 * Flexible type comparison. Allow the usage of an argument even if it's not the
 * hierarchy of the signature type, but they still have a common parent.
 * 
 * @author Yoav Artzi
 */
public class FlexibleTypeComparator implements ITypeComparator {
	
	@Override
	public boolean verifyArgType(Type signatureType, Type argType) {
		return argType.isExtendingOrExtendedBy(LogicLanguageServices
				.getTypeRepository().generalizeType(signatureType));
	}
	
}
