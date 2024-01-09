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
package com.foreach.across.modules.hibernate.jpa;

import com.foreach.across.config.AcrossContextConfigurer;
import com.foreach.across.core.AcrossContext;
import com.foreach.across.core.annotations.Exposed;
import com.foreach.across.core.context.registry.AcrossContextBeanRegistry;
import com.foreach.across.core.registry.IncrementalRefreshableRegistry;
import com.foreach.across.core.registry.RefreshableRegistry;
import com.foreach.across.modules.hibernate.aop.EntityInterceptor;
import com.foreach.across.modules.hibernate.testmodules.jpa.Customer;
import com.foreach.across.modules.hibernate.testmodules.jpa.CustomerRepository;
import com.foreach.across.modules.hibernate.testmodules.jpa.SimpleJpaModule;
import com.foreach.across.modules.hibernate.testmodules.springdata.Client;
import com.foreach.across.modules.hibernate.testmodules.springdata.ClientRepository;
import com.foreach.across.modules.hibernate.testmodules.springdata.SpringDataJpaModule;
import com.foreach.across.test.AcrossTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@DirtiesContext
@ContextConfiguration(classes = TestJpaInterceptors.Config.class)
public class TestJpaInterceptors
{
	@Autowired
	private AcrossContextBeanRegistry beanRegistry;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	@Qualifier("customerInterceptor")
	private EntityInterceptor<Customer> customerInterceptor;

	@Autowired
	@Qualifier("clientInterceptor")
	private EntityInterceptor<Client> clientInterceptor;

	@Autowired
	@Qualifier("allInterceptor")
	private EntityInterceptor<Object> allInterceptor;

	@SuppressWarnings("unchecked")
	@BeforeEach
	public void resetMocks() {
		reset( customerInterceptor, allInterceptor, clientInterceptor );

		when( customerInterceptor.handles( Customer.class ) ).thenReturn( true );
		when( clientInterceptor.handles( Client.class ) ).thenReturn( true );
		when( allInterceptor.handles( any( Class.class ) ) ).thenReturn( true );
	}

	@Test
	public void singleRefreshableRegistryShouldBeIncremental() {
		Map<String, RefreshableRegistry> registries
				= beanRegistry.getBeansOfTypeAsMap(
				TypeDescriptor.collection( RefreshableRegistry.class,
				                           TypeDescriptor.valueOf( EntityInterceptor.class ) )
				              .getResolvableType(),
				true );

		assertEquals( 1, registries.size() );
		assertTrue( beanRegistry.getBean( "entityInterceptors" ) instanceof IncrementalRefreshableRegistry );
	}

	@Test
	public void customerInterceptorShouldNotBeCalled() {
		Customer customer = new Customer();
		customer.setName( UUID.randomUUID().toString() );

		customerRepository.save( customer );

		Customer update = new Customer();
		update.setId( customer.getId() );
		update.setName( "updated" );

		customerRepository.save( update );

		customerRepository.delete( customer );

		verifyNoInteractions( customerInterceptor, clientInterceptor, allInterceptor );
	}

	@Test
	public void clientShouldBeIntercepted() {
		when( allInterceptor.handles( any( Class.class ) ) ).thenReturn( false );

		Client client = new Client();
		client.setName( UUID.randomUUID().toString() );

		verifyNoInteractions( clientInterceptor );

		clientRepository.save( client );

		verify( clientInterceptor ).handles( Client.class );
		verify( clientInterceptor ).beforeCreate( client );
		verify( clientInterceptor ).afterCreate( client );
		verifyNoMoreInteractions( clientInterceptor );

		reset( clientInterceptor );
		when( clientInterceptor.handles( Client.class ) ).thenReturn( true );

		Client update = new Client();
		update.setId( client.getId() );
		update.setName( "updated" );

		clientRepository.save( update );

		verify( clientInterceptor ).handles( Client.class );
		verify( clientInterceptor ).beforeUpdate( client );
		verify( clientInterceptor ).afterUpdate( client );
		verifyNoMoreInteractions( clientInterceptor );

		reset( clientInterceptor );
		when( clientInterceptor.handles( Client.class ) ).thenReturn( true );

		clientRepository.delete( client );

		verify( clientInterceptor ).handles( Client.class );
		verify( clientInterceptor ).beforeDelete( client );
		verify( clientInterceptor ).afterDelete( client );
		verifyNoMoreInteractions( clientInterceptor );

		verify( allInterceptor, times( 3 ) ).handles( Client.class );
		verify( customerInterceptor, times( 3 ) ).handles( Client.class );
		verifyNoMoreInteractions( allInterceptor, customerInterceptor );
	}

	@Test
	public void deleteAllJpaRepository() {
		verifyNoInteractions( clientInterceptor );

		clientRepository.deleteAllInBatch();

		verify( clientInterceptor ).beforeDeleteAll( Client.class );
		verify( clientInterceptor ).afterDeleteAll( Client.class );

		clientRepository.deleteAll();

		verify( clientInterceptor, times( 2 ) ).beforeDeleteAll( Client.class );
		verify( clientInterceptor, times( 2 ) ).afterDeleteAll( Client.class );
	}

	@Test
	public void allInterceptorShouldBeCalledBeforeClientInterceptor() {
		final List<EntityInterceptor<?>> called = new ArrayList<>();

		doAnswer( invocation -> {
			called.add( allInterceptor );
			return null;
		} ).when( allInterceptor ).afterCreate( any() );

		doAnswer( invocation -> {
			called.add( clientInterceptor );
			return null;
		} ).when( clientInterceptor ).afterCreate( any( Client.class ) );

		clientRepository.saveAndFlush( new Client( "another client" ) );

		assertEquals( Arrays.asList( allInterceptor, clientInterceptor ), called );
	}

	@Test
	public void createCustomerBeforeSavingClient() {
		doAnswer( invocation -> {
			Customer customer = new Customer( "nested customer" );
			customerRepository.save( customer );
			return null;
		} ).when( clientInterceptor ).beforeCreate( any( Client.class ) );

		Client client = new Client( "client triggering customer" );
		assertNull( customerRepository.getByName( "nested customer" ) );

		clientRepository.save( client );

		assertNotNull( clientRepository.findById( client.getId() ).orElse( null ) );
		assertNotNull( customerRepository.getByName( "nested customer" ) );
	}

	@Test
	public void nestedCreationHappensInSameTransaction() {
		doAnswer( invocation -> {
			Customer customer = new Customer( "nested customer 2" );
			customerRepository.save( customer );
			return null;
		} ).when( clientInterceptor ).beforeCreate( any( Client.class ) );

		doThrow( new RuntimeException( "after create fail" ) )
				.when( clientInterceptor )
				.afterCreate( any( Client.class ) );

		Client client = new Client( "client triggering customer 2" );
		client.setNewEntityId( -7777L );

		assertNull( customerRepository.getByName( "nested customer 2" ) );

		boolean failed = false;

		try {
			clientRepository.save( client );
		}
		catch ( RuntimeException rte ) {
			failed = true;
		}

		assertTrue( failed );
		assertNull( clientRepository.findById( -7777L ).orElse( null ) );
		assertNull( customerRepository.getByName( "nested customer 2" ) );
	}

	@Test
	public void iterableMethodsJpaRepository() {
		when( allInterceptor.handles( any( Class.class ) ) ).thenReturn( false );

		Client client = new Client( "it-client-1" );
		Client other = new Client( "it-client-2" );

		List<Client> clients = Arrays.asList( client, other );

		verifyNoInteractions( clientInterceptor );

		clientRepository.saveAll( clients );

		verify( clientInterceptor ).handles( Client.class );
		verify( clientInterceptor ).beforeCreate( client );
		verify( clientInterceptor ).beforeCreate( other );
		verify( clientInterceptor ).afterCreate( client );
		verify( clientInterceptor ).afterCreate( other );
		verifyNoMoreInteractions( clientInterceptor );

		reset( clientInterceptor );
		when( clientInterceptor.handles( Client.class ) ).thenReturn( true );

		client.setName( "it-client-1-updated" );

		clientRepository.saveAll( clients );

		verify( clientInterceptor ).handles( Client.class );
		verify( clientInterceptor ).beforeUpdate( client );
		verify( clientInterceptor ).beforeUpdate( other );
		verify( clientInterceptor ).afterUpdate( client );
		verify( clientInterceptor ).afterUpdate( other );
		verifyNoMoreInteractions( clientInterceptor );

		reset( clientInterceptor );
		when( clientInterceptor.handles( Client.class ) ).thenReturn( true );

		clientRepository.deleteAll( clients );

		verify( clientInterceptor ).handles( Client.class );
		verify( clientInterceptor ).beforeDelete( client );
		verify( clientInterceptor ).beforeDelete( other );
		verify( clientInterceptor ).afterDelete( client );
		verify( clientInterceptor ).afterDelete( other );
		verifyNoMoreInteractions( clientInterceptor );

		verify( allInterceptor, times( 3 ) ).handles( Client.class );
		verify( customerInterceptor, times( 3 ) ).handles( Client.class );
		verifyNoMoreInteractions( allInterceptor, customerInterceptor );
	}

	@Configuration
	@SuppressWarnings("unchecked")
	@AcrossTestConfiguration
	protected static class Config implements AcrossContextConfigurer
	{
		@Override
		public void configure( AcrossContext context ) {
			AcrossHibernateJpaModule hibernateModule = new AcrossHibernateJpaModule();
			hibernateModule.setHibernateProperty( "hibernate.hbm2ddl.auto", "create-drop" );
			context.addModule( hibernateModule );

			SimpleJpaModule simpleJpaModule = new SimpleJpaModule();
			simpleJpaModule.addApplicationContextConfigurer( AllInterceptorConfig.class );
			context.addModule( simpleJpaModule );

			SpringDataJpaModule springDataJpaModule = new SpringDataJpaModule();
			springDataJpaModule.addApplicationContextConfigurer( CustomerAndClientInterceptorConfig.class );
			context.addModule( springDataJpaModule );
		}
	}

	@Configuration
	@SuppressWarnings("unchecked")
	protected static class AllInterceptorConfig
	{
		@Bean
		@Exposed
		public EntityInterceptor<Object> allInterceptor() {
			return mock( EntityInterceptor.class );
		}
	}

	@Configuration
	@SuppressWarnings("unchecked")
	protected static class CustomerAndClientInterceptorConfig
	{
		@Bean
		@Exposed
		public EntityInterceptor<Customer> customerInterceptor() {
			return mock( EntityInterceptor.class );
		}

		@Bean
		@Exposed
		public EntityInterceptor<Client> clientInterceptor() {
			return mock( EntityInterceptor.class );
		}
	}
}
