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

package com.foreach.across.modules.logging.config.dynamic;

import com.foreach.across.core.AcrossException;
import com.foreach.across.core.annotations.AcrossDepends;
import com.foreach.across.core.annotations.Exposed;
import com.foreach.across.core.annotations.Module;
import com.foreach.across.modules.logging.LoggingModuleSettings;
import com.foreach.across.modules.logging.request.LogHandlerAndViewNameInterceptor;
import com.foreach.across.modules.logging.request.RequestLoggerConfiguration;
import com.foreach.across.modules.logging.request.RequestLoggerFilter;
import com.foreach.across.modules.web.servlet.AcrossWebDynamicServletConfigurer;
import com.foreach.common.spring.context.ApplicationInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.handler.MappedInterceptor;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Collection;
import java.util.EnumSet;

/**
 * Configures the RequestLoggerFilter to be first in the filter chain
 */
@AcrossDepends(required = "AcrossWebModule")
public class RequestLoggerFilterConfiguration extends AcrossWebDynamicServletConfigurer implements EnvironmentAware
{
	private Environment environment;

	private RequestLoggerFilter requestLogFilter;

	@Override
	public void setEnvironment( Environment environment ) {
		this.environment = environment;
	}

	@Bean
	protected RequestLoggerConfiguration requestLoggerConfiguration() {
		return environment.getProperty( LoggingModuleSettings.REQUEST_LOGGER_CONFIGURATION,
		                                RequestLoggerConfiguration.class,
		                                RequestLoggerConfiguration.allRequests() );
	}

	@Bean
	public RequestLoggerFilter requestLogFilter() {
		RequestLoggerFilter filter = new RequestLoggerFilter();

		if ( requestLoggerConfiguration().getIncludedPathPatterns() != null ) {
			filter.setIncludedPathPatterns( requestLoggerConfiguration().getIncludedPathPatterns() );
		}

		if ( requestLoggerConfiguration().getExcludedPathPatterns() != null ) {
			filter.setExcludedPathPatterns( requestLoggerConfiguration().getExcludedPathPatterns() );
		}

		if ( requestLoggerConfiguration().getLoggerLevelThreshold() != null ) {
			filter.setLoggerLevelThreshold( requestLoggerConfiguration().getLoggerLevelThreshold() );
		}

		this.requestLogFilter = filter;

		return filter;
	}

	@Bean
	@Exposed
	public MappedInterceptor logHandlerAndViewNameMappedInterceptor() {
		return new MappedInterceptor( new String[] { "/**" }, new LogHandlerAndViewNameInterceptor() );
	}

	@Override
	protected void dynamicConfigurationAllowed( ServletContext servletContext ) throws ServletException {
		FilterRegistration.Dynamic filter = servletContext.addFilter( "requestLoggerFilter", requestLogFilter );

		Collection<String> urlFilterMappings = requestLoggerConfiguration().getUrlFilterMappings();
		Collection<String> servletNameFilterMappings = requestLoggerConfiguration().getServletNameFilterMappings();

		if ( urlFilterMappings.isEmpty() && servletNameFilterMappings.isEmpty() ) {
			throw new AcrossException(
					"At least one filter mapping must be specified when enabling request logging" );
		}

		if ( !urlFilterMappings.isEmpty() ) {
			filter.addMappingForUrlPatterns( EnumSet.allOf( DispatcherType.class ), false,
			                                 urlFilterMappings.toArray( new String[urlFilterMappings.size()] ) );
		}
		if ( !servletNameFilterMappings.isEmpty() ) {
			filter.addMappingForServletNames( EnumSet.allOf( DispatcherType.class ), false,
			                                  servletNameFilterMappings.toArray(
					                                  new String[servletNameFilterMappings.size()] ) );
		}
	}

	@Override
	protected void dynamicConfigurationDenied( ServletContext servletContext ) throws ServletException {
	}

	/**
	 * Register the {@link ApplicationInfo#getInstanceId()} as the request log instance id.
	 */
	@Configuration
	@SuppressWarnings("all")
	@AcrossDepends(required = "ApplicationInfoModule")
	public static class ApplicationInstanceLogConfiguration
	{
		@Autowired
		public void registerApplicationInfo( RequestLoggerFilter requestLoggerFilter,
		                                     @Module("ApplicationInfoModule") ApplicationInfo applicationInfo ) {
			requestLoggerFilter.setInstanceId( applicationInfo.getInstanceId() );
		}
	}
}