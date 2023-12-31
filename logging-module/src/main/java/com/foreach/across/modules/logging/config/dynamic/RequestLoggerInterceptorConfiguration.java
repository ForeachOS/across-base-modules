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

import com.foreach.across.core.annotations.ConditionalOnAcrossModule;
import com.foreach.across.modules.web.config.support.PrefixingHandlerMappingConfigurer;
import com.foreach.across.modules.web.mvc.InterceptorRegistry;
import com.foreach.common.web.logging.RequestLogInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

/**
 * @author Andy Somers
 */
@ConditionalOnAcrossModule("AcrossWebModule")
public class RequestLoggerInterceptorConfiguration implements PrefixingHandlerMappingConfigurer
{
	@Autowired
	private RequestLogInterceptor requestLogInterceptor;

	@Override
	public boolean supports( String mapperName ) {
		return true;
	}

	@Override
	public void addInterceptors( InterceptorRegistry interceptorRegistry ) {
		interceptorRegistry.addFirst( requestLogInterceptor );
	}

	@Bean
	public RequestLogInterceptor requestLogInterceptor() {
		return new RequestLogInterceptor();
	}
}
