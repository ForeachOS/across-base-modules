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
package com.foreach.across.modules.hibernate.jpa.config;

import com.foreach.across.core.annotations.Exposed;
import com.foreach.across.core.registry.RefreshableRegistry;
import com.foreach.across.modules.hibernate.aop.EntityInterceptor;
import com.foreach.across.modules.hibernate.config.BasicRepositoryInterceptorConfiguration;
import com.foreach.across.modules.hibernate.jpa.aop.JpaRepositoryInterceptor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.*;

/**
 * Configures intercepting the {@link org.springframework.data.repository.CrudRepository} methods when an entity gets inserted/updated/deleted.
 *
 * @author Andy Somers
 */
@Configuration
@Import(BasicRepositoryInterceptorConfiguration.class)
public class JpaRepositoryInterceptorConfiguration
{
	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	@Exposed
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public JpaRepositoryInterceptor jpaRepositoryInterceptor( RefreshableRegistry<EntityInterceptor> entityInterceptors ) {
		return new JpaRepositoryInterceptor( entityInterceptors );
	}
}
