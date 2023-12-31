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
package com.foreach.across.modules.hibernate.unitofwork;

import lombok.NonNull;
import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate5.SessionFactoryUtils;
import org.springframework.orm.hibernate5.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * @see com.foreach.across.modules.hibernate.unitofwork.UnitOfWorkFactory
 */
public class UnitOfWorkFactoryImpl implements UnitOfWorkFactory
{
	private final Collection<SessionFactory> sessionFactories;

	public UnitOfWorkFactoryImpl( @NonNull Collection<SessionFactory> sessionFactories ) {
		this.sessionFactories = sessionFactories;
	}

	/**
	 * Wraps a Runnable into a unit of work.
	 *
	 * @param runnable Original runnable instance.
	 * @return Wrapped Runnable.
	 */
	public Runnable create( Runnable runnable ) {
		return new RunnableUnitOfWork( this, runnable );
	}

	/**
	 * Wraps a Callable into a unit of work.
	 *
	 * @param callable Original callable instance.
	 * @return Wrapped Callable.
	 */
	public <T> Callable<T> create( Callable<T> callable ) {
		return new CallableUnitOfWork<T>( this, callable );
	}

	/**
	 * Starts a new unit of work: opens all Sessions.
	 */
	public UnitOfWork start() {
		start( false );
		return new UnitOfWork( this );
	}

	/**
	 * When called, this will close and reopen all Sessions attached
	 * to the current thread.
	 */
	public void restart() {
		stop();
		start();
	}

	private void start( boolean ignoreCache ) {
		for ( SessionFactory sessionFactory : sessionFactories ) {
			try {
				if ( !TransactionSynchronizationManager.hasResource( sessionFactory ) ) {
					LOG.trace( "Opening session for {}", sessionFactory );
					Session session = sessionFactory.openSession();
					if ( ignoreCache ) {
						session.setCacheMode( CacheMode.IGNORE );
					}
					TransactionSynchronizationManager.bindResource( sessionFactory, new SessionHolder( session ) );
				}
				else {
					LOG.trace( "Not opening session for {} as sessionFactory is already bound", sessionFactory );
				}
			}
			catch ( Exception e ) {
				LOG.error( "Exception starting unit of work for {}", sessionFactory, e );
			}
		}
	}

	/**
	 * Stops the unit of work: closes all Sessions.
	 */
	public void stop() {
		for ( SessionFactory sessionFactory : sessionFactories ) {
			try {
				SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource( sessionFactory );

				if ( holder != null ) {
					// If there is still a transaction running, don't close, it should be closed then transaction finishes
					if ( !TransactionSynchronizationManager.isActualTransactionActive() ) {
						LOG.trace( "Closing session for {}", sessionFactory );
						Session session = holder.getSession();
						session.flush();

						SessionFactoryUtils.closeSession( session );
					}
					else {
						LOG.trace( "Not closing session for {} as transaction is active", sessionFactory );
					}

					TransactionSynchronizationManager.unbindResource( sessionFactory );
				}
			}
			catch ( Exception e ) {
				LOG.error( "Exception stopping unit of work for {}", sessionFactory, e );
			}
		}
	}
}
