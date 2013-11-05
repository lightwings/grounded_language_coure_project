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
package edu.uw.cs.lil.tiny.data.resources;

import edu.uw.cs.lil.tiny.data.collection.CompositeDataCollection;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.utils.collections.ListUtils;

public class CompositeDataCollectionCreator<T> implements
		IResourceObjectCreator<CompositeDataCollection<T>> {
	private static final String	DEFAULT_NAME	= "data.composite";
	private final String		resourceName;
	
	public CompositeDataCollectionCreator() {
		this(DEFAULT_NAME);
	}
	
	public CompositeDataCollectionCreator(String resourceName) {
		this.resourceName = resourceName;
	}
	
	@Override
	public CompositeDataCollection<T> create(Parameters parameters,
			final IResourceRepository resourceRepo) {
		return new CompositeDataCollection<T>(ListUtils.map(
				parameters.getSplit("sets"),
				new ListUtils.Mapper<String, IDataCollection<? extends T>>() {
					
					@Override
					public IDataCollection<T> process(String obj) {
						return resourceRepo.getResource(obj);
					}
				}));
	}
	
	@Override
	public String type() {
		return resourceName;
	}
	
	@Override
	public ResourceUsage usage() {
		return new ResourceUsage.Builder(type(), CompositeDataCollection.class)
				.setDescription(
						"Composite dataset. Concatenates separate datasets of the same type into a single one")
				.addParam("sets", "list of datasets",
						"List of datasets of the same type (e.g., 'data1,data2,data3')")
				.build();
	}
	
}
