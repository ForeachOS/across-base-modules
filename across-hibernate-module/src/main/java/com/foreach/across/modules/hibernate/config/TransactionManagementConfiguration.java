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

package com.foreach.across.modules.hibernate.config;

import com.foreach.across.core.annotations.Exposed;
import com.foreach.across.modules.hibernate.modules.config.EnableTransactionManagementConfiguration;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Arne Vandamme
 */
@Configuration
@EnableTransactionManagement(order = EnableTransactionManagementConfiguration.INTERCEPT_ORDER)
public class TransactionManagementConfiguration
{
	public static final Logger LOG = LoggerFactory.getLogger( TransactionManagementConfiguration.class );

	@Bean(name = HibernateConfiguration.TRANSACTION_MANAGER)
	@Exposed
	public HibernateTransactionManager transactionManager( SessionFactory sessionFactory ) {
		return new HibernateTransactionManager( sessionFactory );
	}

	@Bean
	@Exposed
	public TransactionTemplate transactionTemplate( PlatformTransactionManager transactionManager ) {
		return new TransactionTemplate( transactionManager );
	}
}
