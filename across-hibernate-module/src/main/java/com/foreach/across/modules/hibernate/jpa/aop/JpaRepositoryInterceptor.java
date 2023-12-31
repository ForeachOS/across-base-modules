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
package com.foreach.across.modules.hibernate.jpa.aop;

import com.foreach.across.modules.hibernate.aop.EntityInterceptor;
import com.foreach.across.modules.hibernate.support.TransactionWrapper.InvocationCallback;
import org.aopalliance.intercept.MethodInvocation;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Persistable;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Intercepts persistence calls on a {@link org.springframework.data.jpa.repository.JpaRepository}.
 *
 * @author Andy Somers
 */
public class JpaRepositoryInterceptor extends AbstractCrudRepositoryInterceptor
{
	static final String SAVE_AND_FLUSH = "saveAndFlush";
	static final String DELETE_IN_BATCH = "deleteInBatch";
	static final String DELETE_ALL_IN_BATCH = "deleteAllInBatch";

	public JpaRepositoryInterceptor( Collection<EntityInterceptor> interceptors ) {
		super( interceptors );
	}

	@Override
	protected InvocationCallback<Object> determineCallbackMethod( MethodInvocation invocation ) {
		InvocationCallback<Object> handler = super.determineCallbackMethod( invocation );

		if ( handler == null ) {
			Method method = invocation.getMethod();
			Object[] arguments = invocation.getArguments();
			String methodName = method.getName();
			if ( DELETE_ALL_IN_BATCH.equalsIgnoreCase( methodName ) ) {
				handler = () -> {
					Class<?> entityClass = getEntityClass( invocation );
					Collection<EntityInterceptor> interceptors = findInterceptorsToApply( entityClass,
					                                                                      getInterceptors() );
					callBeforeDeleteAll( interceptors, entityClass );

					Object returnValue = invocation.proceed();
					callAfterDeleteAll( interceptors, entityClass );
					return returnValue;
				};
			}
			else if ( DELETE_IN_BATCH.equalsIgnoreCase( methodName ) ) {
				handler = () -> {
					Class<?> entityClassForDelete = getEntityClass( invocation );
					Collection<EntityInterceptor> interceptorsForDelete = findInterceptorsToApply( entityClassForDelete,
					                                                                               getInterceptors() );

					for ( Object o : (Iterable) arguments[0] ) {
						callBeforeDelete( interceptorsForDelete, o );
					}
					Object returnValueForDelete = invocation.proceed();
					for ( Object o : (Iterable) arguments[0] ) {
						callAfterDelete( interceptorsForDelete, o );
					}
					return returnValueForDelete;
				};
			}
			else if ( SAVE_AND_FLUSH.equalsIgnoreCase( methodName ) ) {
				handler = () -> {
					Object objectToSave = arguments[0];
					Class<?> entityClassForSave = ClassUtils.getUserClass( Hibernate.getClass( objectToSave ) );
					Collection<EntityInterceptor> interceptorsForSave = findInterceptorsToApply( entityClassForSave,
					                                                                             getInterceptors() );

					boolean isNew = ( (Persistable) objectToSave ).isNew();
					if ( isNew ) {
						callBeforeCreate( interceptorsForSave, objectToSave );
					}
					else {
						callBeforeUpdate( interceptorsForSave, objectToSave );
					}

					Object returnValueForSave = invocation.proceed();

					if ( isNew ) {
						callAfterCreate( interceptorsForSave, objectToSave );
					}
					else {
						callAfterUpdate( interceptorsForSave, objectToSave );
					}

					return returnValueForSave;
				};
			}
		}

		return handler;
	}
}