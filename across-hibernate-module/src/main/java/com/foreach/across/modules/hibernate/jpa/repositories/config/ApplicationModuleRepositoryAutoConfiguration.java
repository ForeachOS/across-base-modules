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

package com.foreach.across.modules.hibernate.jpa.repositories.config;

import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.Locale;

import static com.foreach.across.modules.hibernate.jpa.config.HibernateJpaConfiguration.DEFAULT_ACROSS_BOOTSTRAP_MODE;

/**
 * @author Arne Vandamme
 * @since 3.0.0
 */
@Import(ApplicationModuleRepositoryAutoConfiguration.ApplicationModuleRepositoriesRegistrar.class)
public class ApplicationModuleRepositoryAutoConfiguration
{
	static class ApplicationModuleRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport
	{
		private BootstrapMode bootstrapMode = null;

		@Override
		protected Class<? extends Annotation> getAnnotation() {
			return EnableAcrossJpaRepositories.class;
		}

		@Override
		protected Class<?> getConfiguration() {
			return EnableJpaRepositoriesConfiguration.class;
		}

		@Override
		protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
			return new AcrossJpaRepositoryConfigExtension();
		}

		@Override
		protected BootstrapMode getBootstrapMode() {
			return ( this.bootstrapMode == null ) ? DEFAULT_ACROSS_BOOTSTRAP_MODE : this.bootstrapMode;
		}

		@Override
		public void setEnvironment( Environment environment ) {
			super.setEnvironment( environment );
			configureBootstrapMode( environment );
		}

		private void configureBootstrapMode( Environment environment ) {
			String property = environment.getProperty( "spring.data.jpa.repositories.bootstrap-mode" );
			if ( StringUtils.hasText( property ) ) {
				this.bootstrapMode = BootstrapMode.valueOf( property.toUpperCase( Locale.ENGLISH ) );
			}
		}

		@EnableAcrossJpaRepositories
		private static class EnableJpaRepositoriesConfiguration
		{
		}
	}
}
