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
package com.foreach.across.modules.hibernate;

import com.foreach.across.config.AcrossContextConfigurer;
import com.foreach.across.core.AcrossContext;
import com.foreach.across.core.annotations.Exposed;
import com.foreach.across.core.context.registry.AcrossContextBeanRegistry;
import com.foreach.across.core.registry.IncrementalRefreshableRegistry;
import com.foreach.across.core.registry.RefreshableRegistry;
import com.foreach.across.modules.hibernate.aop.EntityInterceptor;
import com.foreach.across.modules.hibernate.testmodules.hibernate1.Hibernate1Module;
import com.foreach.across.modules.hibernate.testmodules.hibernate1.Product;
import com.foreach.across.modules.hibernate.testmodules.hibernate1.ProductRepository;
import com.foreach.across.modules.hibernate.testmodules.hibernate2.Hibernate2Module;
import com.foreach.across.modules.hibernate.testmodules.hibernate2.User;
import com.foreach.across.modules.hibernate.testmodules.hibernate2.UserRepository;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Arne Vandamme
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestHibernateInterceptors.Config.class)
@DirtiesContext
public class TestHibernateInterceptors
{
	@Autowired
	private AcrossContextBeanRegistry beanRegistry;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	@Qualifier("userInterceptor")
	private EntityInterceptor<User> userInterceptor;

	@Autowired
	@Qualifier("allInterceptor")
	private EntityInterceptor<Object> allInterceptor;

	@Autowired
	@Qualifier("productInterceptor")
	private EntityInterceptor<Product> productInterceptor;

	@BeforeEach
	public void before() {
		reset( userInterceptor, allInterceptor, productInterceptor );

		when( userInterceptor.handles( User.class ) ).thenReturn( true );
		when( allInterceptor.handles( any( Class.class ) ) ).thenReturn( true );
		when( productInterceptor.handles( Product.class ) ).thenReturn( true );
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
	public void userShouldBeIntercepted() {
		when( allInterceptor.handles( any( Class.class ) ) ).thenReturn( false );

		User user = new User( 10, "test" );

		verifyNoInteractions( userInterceptor );

		userRepository.create( user );

		verify( userInterceptor ).handles( User.class );
		verify( userInterceptor ).beforeCreate( user );
		verify( userInterceptor ).afterCreate( user );
		verifyNoMoreInteractions( userInterceptor );

		reset( userInterceptor );
		when( userInterceptor.handles( User.class ) ).thenReturn( true );

		userRepository.update( user );

		verify( userInterceptor ).handles( User.class );
		verify( userInterceptor ).beforeUpdate( user );
		verify( userInterceptor ).afterUpdate( user );
		verifyNoMoreInteractions( userInterceptor );

		reset( userInterceptor );
		when( userInterceptor.handles( User.class ) ).thenReturn( true );

		userRepository.delete( user );

		verify( userInterceptor ).handles( User.class );
		verify( userInterceptor ).beforeDelete( user );
		verify( userInterceptor ).afterDelete( user );
		verifyNoMoreInteractions( userInterceptor );

		verify( allInterceptor, times( 3 ) ).handles( User.class );
		verify( productInterceptor, times( 3 ) ).handles( User.class );
		verifyNoMoreInteractions( allInterceptor, productInterceptor );
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

		assertEquals( Arrays.asList( allInterceptor, userInterceptor ), called );
	}

	@Test
	public void createProductBeforeSavingUser() {
		doAnswer( invocation -> {
			Product product = new Product( 999, "nested product" );
			productRepository.save( product );
			return null;
		} ).when( userInterceptor ).beforeCreate( any( User.class ) );

		User user = new User( 999, "my user" );
		assertNull( productRepository.getProductWithId( 999 ) );

		userRepository.create( user );

		assertNotNull( userRepository.getById( 999 ) );

		Product createdProduct = productRepository.getProductWithId( 999 );
		assertNotNull( createdProduct );
		assertEquals( "nested product", createdProduct.getName() );
	}

	@Test
	public void nestedCreationHappensInSameTransaction() {
		doAnswer( invocation -> {
			Product product = new Product( 1000, "nested product 2" );
			productRepository.save( product );
			return null;
		} ).when( userInterceptor )
		   .beforeCreate( any( User.class ) );

		doThrow( new RuntimeException( "exception thrown" ) )
				.when( userInterceptor )
				.afterCreate( any( User.class ) );

		User user = new User( 1000, "my user 2" );
		assertNull( productRepository.getProductWithId( 1000 ) );

		boolean failed = false;

		try {
			userRepository.create( user );
		}
		catch ( RuntimeException rte ) {
			failed = true;
		}

		assertTrue( failed );
		assertNull( userRepository.getById( 1000 ) );
		assertNull( productRepository.getProductWithId( 1000 ) );
	}

	@Test
	public void productInterceptorShouldNotBeCalled() {
		Product product = new Product( 10, "some product" );
		productRepository.save( product );

		verifyNoInteractions( productInterceptor );
	}

	@Configuration
	@SuppressWarnings("unchecked")
	@AcrossTestConfiguration
	protected static class Config implements AcrossContextConfigurer
	{
		@Override
		public void configure( AcrossContext context ) {
			AcrossHibernateModule hibernateModule = new AcrossHibernateModule();
			hibernateModule.setProperty( AcrossHibernateModuleSettings.REGISTER_REPOSITORY_INTERCEPTOR, true );
			hibernateModule.setHibernateProperty( "hibernate.hbm2ddl.auto", "create-drop" );
			context.addModule( hibernateModule );

			Hibernate1Module hibernate1Module = new Hibernate1Module();
			hibernate1Module.addApplicationContextConfigurer( AllInterceptorConfig.class );

			Hibernate2Module hibernate2Module = new Hibernate2Module();
			hibernate2Module.addApplicationContextConfigurer( UserAndProductInterceptorConfig.class );

			context.addModule( hibernate1Module );
			context.addModule( hibernate2Module );
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
	protected static class UserAndProductInterceptorConfig
	{
		@Bean
		@Exposed
		public EntityInterceptor<User> userInterceptor() {
			return mock( EntityInterceptor.class );
		}

		@Bean
		@Exposed
		public EntityInterceptor<Product> productInterceptor() {
			return mock( EntityInterceptor.class );
		}
	}
}
