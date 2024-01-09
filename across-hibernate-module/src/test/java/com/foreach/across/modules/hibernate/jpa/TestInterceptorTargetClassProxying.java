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
import com.foreach.across.modules.hibernate.aop.EntityInterceptor;
import com.foreach.across.modules.hibernate.testmodules.hibernate2.Hibernate2Module;
import com.foreach.across.modules.hibernate.testmodules.hibernate2.User;
import com.foreach.across.modules.hibernate.testmodules.hibernate2.UserRepository;
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
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * JPA repositories are created using a custom FactoryBean and are by itself already proxies.  AOP interceptors should
 * be applied directly through the {@link org.springframework.data.jpa.repository.support.JpaRepositoryFactory}
 * instead of through regular AOP because extending a Proxy is not possible in case of target class proxying.
 *
 * @author Arne Vandamme
 */
@ExtendWith(SpringExtension.class)
@DirtiesContext
@ContextConfiguration(classes = TestInterceptorTargetClassProxying.Config.class)
public class TestInterceptorTargetClassProxying
{
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	@Qualifier("userInterceptor")
	private EntityInterceptor<User> userInterceptor;

	@Autowired
	@Qualifier("allInterceptor")
	private EntityInterceptor<Object> allInterceptor;

	@Autowired
	@Qualifier("clientInterceptor")
	private EntityInterceptor<Client> clientInterceptor;

	@BeforeEach
	public void before() {
		reset( userInterceptor, allInterceptor, clientInterceptor );

		when( userInterceptor.handles( User.class ) ).thenReturn( true );
		when( allInterceptor.handles( any( Class.class ) ) ).thenReturn( true );
		when( clientInterceptor.handles( Client.class ) ).thenReturn( true );
	}

	@Test
	public void allInterceptorShouldBeCalledBeforeUserInterceptor() {
		final List<EntityInterceptor<?>> called = new ArrayList<>();

		doAnswer( invocation -> {
			called.add( allInterceptor );
			return null;
		} ).when( allInterceptor ).afterCreate( any() );

		doAnswer( invocation -> {
			called.add( userInterceptor );
			return null;
		} ).when( userInterceptor ).afterCreate( any( User.class ) );

		userRepository.create( new User( 1010, "another user" ) );

		assertEquals( Collections.emptyList(), called );
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

		clientRepository.save( new Client( "another client" ) );

		assertEquals( Arrays.asList( allInterceptor, clientInterceptor ), called );
	}

	@Test
	public void iterableMethodsShouldNotRecurseIntoInterceptor() {
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
		verifyNoMoreInteractions( allInterceptor );
	}

	@Configuration
	@SuppressWarnings("all")
	@AcrossTestConfiguration
	protected static class Config implements AcrossContextConfigurer
	{
		@Override
		public void configure( AcrossContext context ) {
			AcrossHibernateJpaModule hibernateModule = new AcrossHibernateJpaModule();
			hibernateModule.setHibernateProperty( "hibernate.hbm2ddl.auto", "create-drop" );
			context.addModule( hibernateModule );

			Hibernate2Module hibernate2Module = new Hibernate2Module();
			hibernate2Module.addApplicationContextConfigurer( AllInterceptorConfig.class );
			context.addModule( hibernate2Module );

			SpringDataJpaModule springDataJpaModule = new SpringDataJpaModule();
			springDataJpaModule.addApplicationContextConfigurer( CustomerAndClientInterceptorConfig.class );
			context.addModule( springDataJpaModule );
		}
	}

	@Configuration
	@EnableAspectJAutoProxy(proxyTargetClass = true)
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
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	@SuppressWarnings("unchecked")
	protected static class CustomerAndClientInterceptorConfig
	{
		@Bean
		@Exposed
		public EntityInterceptor<User> userInterceptor() {
			return mock( EntityInterceptor.class );
		}

		@Bean
		@Exposed
		public EntityInterceptor<Client> clientInterceptor() {
			return mock( EntityInterceptor.class );
		}
	}
}
