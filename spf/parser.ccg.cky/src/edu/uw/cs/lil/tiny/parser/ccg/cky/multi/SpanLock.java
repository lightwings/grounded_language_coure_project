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
package edu.uw.cs.lil.tiny.parser.ccg.cky.multi;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SpanLock {
	
	private final Lock[][]	spans;
	
	public SpanLock(int length) {
		this.spans = new Lock[length][length];
		for (int i = 0; i < length; ++i) {
			for (int j = 0; j < length; ++j) {
				spans[i][j] = new ReentrantLock();
			}
		}
	}
	
	public void lock(int start, int end) {
		spans[start][end].lock();
	}
	
	public void unlock(int start, int end) {
		spans[start][end].unlock();
	}
	
}
