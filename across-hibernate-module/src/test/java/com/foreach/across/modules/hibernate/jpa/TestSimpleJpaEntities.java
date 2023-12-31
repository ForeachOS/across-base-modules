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
import com.foreach.across.core.filters.ClassBeanFilter;
import com.foreach.across.modules.hibernate.testmodules.jpa.Customer;
import com.foreach.across.modules.hibernate.testmodules.jpa.CustomerRepository;
import com.foreach.across.modules.hibernate.testmodules.jpa.SimpleJpaModule;
import com.foreach.across.modules.hibernate.testmodules.springdata.Client;
import com.foreach.across.modules.hibernate.testmodules.springdata.ClientRepository;
import com.foreach.across.modules.hibernate.testmodules.springdata.SpringDataJpaModule;
import com.foreach.across.test.AcrossTestConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DirtiesContext
@ContextConfiguration
public class TestSimpleJpaEntities
{
	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private ClientRepository clientRepository;

	@Test
	public void crudCustomer() {
		List<Customer> customers = customerRepository.getAll();
		assertTrue( customers.isEmpty() );

		Customer customer = new Customer();
		customer.setName( UUID.randomUUID().toString() );

		customerRepository.save( customer );
		assertNotNull( customer.getId() );

		customers = customerRepository.getAll();
		assertEquals( 1, customers.size() );
		assertTrue( customers.contains( customer ) );
	}

	@Test
	public void crudClientWithFixedId() {
		Iterable<Client> clients = clientRepository.findAll();
		assertFalse( clients.iterator().hasNext() );

		Client autoId = new Client( "one" );
		assertTrue( autoId.isNew() );

		Client fixedId = new Client( "two" );
		fixedId.setNewEntityId( -10L );
		assertTrue( fixedId.isNew() );

		Client savedAuto = clientRepository.save( autoId );
		assertFalse( savedAuto.isNew() );
		assertTrue( savedAuto.getId() > 0 );

		Client savedFixed = clientRepository.save( fixedId );
		assertFalse( savedFixed.isNew() );
		assertEquals( Long.valueOf( -10L ), savedFixed.getId() );

		assertEquals( fixedId, clientRepository.findById( -10L ).orElse( null ) );
	}

	@Configuration
	@AcrossTestConfiguration
	protected static class Config implements AcrossContextConfigurer
	{
		@Override
		public void configure( AcrossContext context ) {
			AcrossHibernateJpaModule hibernateModule = new AcrossHibernateJpaModule();
			hibernateModule.setHibernateProperty( "hibernate.hbm2ddl.auto", "create-drop" );
			context.addModule( hibernateModule );

			context.addModule( new SimpleJpaModule() );

			SpringDataJpaModule springDataJpaModule = new SpringDataJpaModule();
			springDataJpaModule.setExposeFilter( new ClassBeanFilter( ClientRepository.class ) );
			context.addModule( springDataJpaModule );
		}
	}
}
