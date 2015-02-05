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
package com.foreach.across.modules.entity.views.elements;

import com.foreach.across.modules.entity.registry.EntityConfiguration;
import com.foreach.across.modules.entity.registry.EntityRegistry;
import com.foreach.across.modules.entity.registry.properties.EntityPropertyDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ClassUtils;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Arne Vandamme
 */
public class CommonViewElementTypeLookupStrategy implements ViewElementTypeLookupStrategy
{
	@Autowired
	private EntityRegistry entityRegistry;

	@Override
	public String findElementType( EntityConfiguration entityConfiguration,
	                               EntityPropertyDescriptor descriptor,
	                               ViewElementMode viewElementMode ) {
		if ( viewElementMode == ViewElementMode.FOR_READING ) {
			return CommonViewElements.TEXT;
		}
		if ( descriptor.isHidden() ) {
			return CommonViewElements.HIDDEN;
		}

		if ( descriptor.isWritable() ) {
			Class propertyType = descriptor.getPropertyType();

			if ( propertyType != null ) {
				if ( propertyType.isArray() || Collection.class.isAssignableFrom( propertyType ) ) {
					return CommonViewElements.MULTI_CHECKBOX;
				}

				if ( propertyType.isEnum() ) {
					return CommonViewElements.SELECT;
				}

				if ( !ClassUtils.isPrimitiveOrWrapper( propertyType ) ) {
					EntityConfiguration member = entityRegistry.getEntityConfiguration( propertyType );

					if ( member != null ) {
						return CommonViewElements.SELECT;
					}
				}

				if ( ClassUtils.isAssignable( Boolean.class, propertyType )
						|| ClassUtils.isAssignable( AtomicBoolean.class, propertyType ) ) {
					return CommonViewElements.CHECKBOX;
				}

				return CommonViewElements.TEXTBOX;
			}
		}

		return null;
	}
}
