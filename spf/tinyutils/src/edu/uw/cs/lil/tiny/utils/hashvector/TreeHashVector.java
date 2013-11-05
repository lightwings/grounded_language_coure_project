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
package edu.uw.cs.lil.tiny.utils.hashvector;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import edu.uw.cs.utils.composites.Pair;

/**
 * Sparse vector based on a {@link TreeMap}. This implementation is less
 * efficient and has higher memory consumption than {@link TroveHashVector}.
 * However it does maintain a predictable order of iteration, which is important
 * for stability between runs.
 * 
 * @author Yoav Artzi
 */
class TreeHashVector implements IHashVector {
	private static final long				serialVersionUID	= 4294341073950816236L;
	private final TreeMap<KeyArgs, Double>	values				= new TreeMap<KeyArgs, Double>();
	
	TreeHashVector() {
	}
	
	TreeHashVector(IHashVectorImmutable other) {
		for (final Pair<KeyArgs, Double> o : other) {
			values.put(o.first(), o.second());
		}
	}
	
	private static void assertNull(Object o) {
		if (o == null) {
			throw new IllegalArgumentException("Unexpected null argument");
		}
	}
	
	@Override
	public void add(final double num) {
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			entry.setValue(entry.getValue() + num);
		}
	}
	
	@Override
	public IHashVector addTimes(final double times, IHashVectorImmutable other) {
		if (other instanceof TreeHashVector) {
			final TreeHashVector p = (TreeHashVector) other;
			final TreeHashVector ret = new TreeHashVector(this);
			for (final Entry<KeyArgs, Double> entry : p.values.entrySet()) {
				final KeyArgs key = entry.getKey();
				Double value = entry.getValue() * times;
				if (values.containsKey(key)) {
					value += values.get(key);
				}
				ret.values.put(key, value);
			}
			return ret;
		} else {
			return addTimes(times, new TreeHashVector(other));
		}
	}
	
	@Override
	public void addTimesInto(final double times, IHashVector other) {
		if (other instanceof TreeHashVector) {
			final TreeHashVector p = (TreeHashVector) other;
			
			for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
				final double value = times * entry.getValue();
				final KeyArgs key = entry.getKey();
				if (p.values.containsKey(key)) {
					p.values.put(key, value + p.values.get(key));
				} else {
					p.values.put(key, value);
				}
			}
		} else {
			addTimesInto(times, new TreeHashVector(other));
		}
	}
	
	@Override
	public void clear() {
		values.clear();
	}
	
	@Override
	public void divideBy(final double d) {
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			entry.setValue(entry.getValue() / d);
		}
	}
	
	@Override
	public void dropSmallEntries() {
		final Iterator<Entry<KeyArgs, Double>> iterator = values.entrySet()
				.iterator();
		while (iterator.hasNext()) {
			if (Math.abs(iterator.next().getValue()) < NOISE) {
				iterator.remove();
			}
		}
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
		final TreeHashVector other = (TreeHashVector) obj;
		if (values == null) {
			if (other.values != null) {
				return false;
			}
		} else if (!values.equals(other.values)) {
			return false;
		}
		return true;
	}
	
	@Override
	public double get(String arg1) {
		assertNull(arg1);
		final Double value = values.get(new KeyArgs(arg1));
		return value == null ? IHashVector.ZERO_VALUE : value;
	}
	
	@Override
	public double get(String arg1, String arg2) {
		assertNull(arg1);
		assertNull(arg2);
		final Double value = values.get(new KeyArgs(arg1, arg2));
		return value == null ? IHashVector.ZERO_VALUE : value;
	}
	
	@Override
	public double get(String arg1, String arg2, String arg3) {
		assertNull(arg1);
		assertNull(arg2);
		assertNull(arg3);
		final Double value = values.get(new KeyArgs(arg1, arg2, arg3));
		return value == null ? IHashVector.ZERO_VALUE : value;
	}
	
	@Override
	public double get(String arg1, String arg2, String arg3, String arg4) {
		assertNull(arg1);
		assertNull(arg2);
		assertNull(arg3);
		assertNull(arg4);
		final Double value = values.get(new KeyArgs(arg1, arg2, arg3, arg4));
		return value == null ? IHashVector.ZERO_VALUE : value;
	}
	
	@Override
	public double get(String arg1, String arg2, String arg3, String arg4,
			String arg5) {
		assertNull(arg1);
		assertNull(arg2);
		assertNull(arg3);
		assertNull(arg4);
		assertNull(arg5);
		final Double value = values.get(new KeyArgs(arg1, arg2, arg3, arg4,
				arg5));
		return value == null ? IHashVector.ZERO_VALUE : value;
	}
	
	@Override
	public IHashVector getAll(final String arg1) {
		final TreeHashVector result = new TreeHashVector();
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			final KeyArgs key = entry.getKey();
			if (arg1.equals(key.arg1)) {
				result.values.put(key, entry.getValue());
			}
		}
		return result;
	}
	
	@Override
	public IHashVector getAll(final String arg1, final String arg2) {
		final TreeHashVector result = new TreeHashVector();
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			final KeyArgs key = entry.getKey();
			if (arg1.equals(key.arg1) && arg2.equals(key.arg2)) {
				result.values.put(key, entry.getValue());
			}
		}
		return result;
	}
	
	@Override
	public IHashVector getAll(final String arg1, final String arg2,
			final String arg3) {
		final TreeHashVector result = new TreeHashVector();
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			final KeyArgs key = entry.getKey();
			if (arg1.equals(key.arg1) && arg2.equals(key.arg2)
					&& arg3.equals(key.arg3)) {
				result.values.put(key, entry.getValue());
			}
		}
		return result;
	}
	
	@Override
	public IHashVector getAll(final String arg1, final String arg2,
			final String arg3, final String arg4) {
		final TreeHashVector result = new TreeHashVector();
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			final KeyArgs key = entry.getKey();
			if (arg1.equals(key.arg1) && arg2.equals(key.arg2)
					&& arg3.equals(key.arg3) && arg4.equals(key.arg4)) {
				result.values.put(key, entry.getValue());
			}
		}
		return result;
	}
	
	@Override
	public IHashVector getAll(final String arg1, final String arg2,
			final String arg3, final String arg4, final String arg5) {
		final TreeHashVector result = new TreeHashVector();
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			final KeyArgs key = entry.getKey();
			if (arg1.equals(key.arg1) && arg2.equals(key.arg2)
					&& arg3.equals(key.arg3) && arg4.equals(key.arg4)
					&& arg5.equals(key.arg5)) {
				result.values.put(key, entry.getValue());
			}
		}
		return result;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((values == null) ? 0 : values.hashCode());
		return result;
	}
	
	@Override
	public boolean isBad() {
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			final Double value = entry.getValue();
			if (Double.isNaN(value) || Double.isInfinite(value)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public Iterator<Pair<KeyArgs, Double>> iterator() {
		return new Iterator<Pair<KeyArgs, Double>>() {
			private final Iterator<Entry<KeyArgs, Double>>	innerIterator	= values.entrySet()
																					.iterator();
			
			@Override
			public boolean hasNext() {
				return innerIterator.hasNext();
			}
			
			@Override
			public Pair<KeyArgs, Double> next() {
				if (innerIterator.hasNext()) {
					final Entry<KeyArgs, Double> next = innerIterator.next();
					return Pair.of(next.getKey(), next.getValue());
				} else {
					return null;
				}
			}
			
			@Override
			public void remove() {
				innerIterator.remove();
			}
		};
	}
	
	@Override
	public double l1Norm() {
		double sum = 0.0;
		for (final double value : values.values()) {
			sum += Math.abs(value);
		}
		return sum;
	}
	
	@Override
	public void multiplyBy(final double d) {
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			entry.setValue(entry.getValue() * d);
		}
	}
	
	@Override
	public String printValues(IHashVectorImmutable other) {
		final StringBuilder ret = new StringBuilder();
		ret.append("{");
		if (other instanceof TreeHashVector) {
			final TreeHashVector p = (TreeHashVector) other;
			for (final Entry<KeyArgs, Double> entry : p.values.entrySet()) {
				if (values.containsKey(entry.getKey())) {
					ret.append(entry.getKey() + "="
							+ values.get(entry.getKey()) + "("
							+ p.values.get(entry.getKey()) + "),");
				} else {
					ret.append(entry.getKey() + "=" + ZERO_VALUE + "("
							+ p.values.get(entry.getKey()) + "),");
				}
			}
			ret.deleteCharAt(ret.length() - 1);
			ret.append("}");
			return ret.toString();
		} else {
			return printValues(new TreeHashVector(other));
		}
	}
	
	@Override
	public void set(String arg1, double value) {
		assertNull(arg1);
		values.put(new KeyArgs(arg1), value);
	}
	
	@Override
	public void set(String arg1, String arg2, double value) {
		assertNull(arg1);
		assertNull(arg2);
		values.put(new KeyArgs(arg1, arg2), value);
	}
	
	@Override
	public void set(String arg1, String arg2, String arg3, double value) {
		assertNull(arg1);
		assertNull(arg2);
		assertNull(arg3);
		values.put(new KeyArgs(arg1, arg2, arg3), value);
	}
	
	@Override
	public void set(String arg1, String arg2, String arg3, String arg4,
			double value) {
		assertNull(arg1);
		assertNull(arg2);
		assertNull(arg3);
		assertNull(arg4);
		values.put(new KeyArgs(arg1, arg2, arg3, arg4), value);
	}
	
	@Override
	public void set(String arg1, String arg2, String arg3, String arg4,
			String arg5, double value) {
		assertNull(arg1);
		assertNull(arg2);
		assertNull(arg3);
		assertNull(arg4);
		assertNull(arg5);
		values.put(new KeyArgs(arg1, arg2, arg3, arg4, arg5), value);
	}
	
	@Override
	public int size() {
		return values.size();
	}
	
	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder();
		ret.append("{");
		final Iterator<Entry<KeyArgs, Double>> iterator = values.entrySet()
				.iterator();
		while (iterator.hasNext()) {
			final Entry<KeyArgs, Double> next = iterator.next();
			ret.append(next.getKey());
			ret.append("=");
			ret.append(next.getValue());
			if (iterator.hasNext()) {
				ret.append(", ");
			}
		}
		ret.append("}");
		return ret.toString();
	}
	
	@Override
	public boolean valuesInRange(final double min, final double max) {
		for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
			final Double value = entry.getValue();
			if (value < min || value > max) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public double vectorMultiply(IHashVectorImmutable other) {
		if (other instanceof TreeHashVector) {
			final TreeHashVector thv = (TreeHashVector) other;
			double sum = 0.0;
			if (size() <= other.size()) {
				for (final Entry<KeyArgs, Double> entry : values.entrySet()) {
					if (thv.values.containsKey(entry.getKey())) {
						sum += entry.getValue()
								* thv.values.get(entry.getKey());
					}
				}
			} else {
				return other.vectorMultiply(this);
			}
			return sum;
		} else {
			return vectorMultiply(new TreeHashVector(other));
		}
	}
}
