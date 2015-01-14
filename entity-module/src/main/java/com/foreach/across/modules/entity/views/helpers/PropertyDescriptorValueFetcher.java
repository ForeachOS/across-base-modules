/*
 * Copyright 2014 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.foreach.across.modules.entity.views.helpers;

import java.beans.PropertyDescriptor;

/**
 * @author Arne Vandamme
 */
public class PropertyDescriptorValueFetcher<T> implements ValueFetcher<T>
{
	private final PropertyDescriptor descriptor;

	public PropertyDescriptorValueFetcher( PropertyDescriptor descriptor ) {
		this.descriptor = descriptor;
	}

	@Override
	public Object getValue( T entity ) {
		try {
			return descriptor.getReadMethod().invoke( entity );
		}
		catch ( Exception e ) {
			return null;
		}
	}
}