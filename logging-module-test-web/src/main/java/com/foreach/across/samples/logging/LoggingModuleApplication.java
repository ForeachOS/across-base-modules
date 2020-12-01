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

package com.foreach.across.samples.logging;

import com.foreach.across.AcrossApplicationRunner;
import com.foreach.across.config.AcrossApplication;
import com.foreach.across.modules.debugweb.DebugWebModule;
import com.foreach.across.modules.logging.LoggingModule;
import com.foreach.across.modules.logging.LoggingModuleSettings;
import com.foreach.across.modules.logging.method.MethodLogConfiguration;
import com.foreach.across.modules.logging.request.RequestLoggerConfiguration;
import com.foreach.across.modules.web.AcrossWebModule;
import org.springframework.context.annotation.Bean;

/**
 * @author Arne Vandamme
 * @since 2.0.0
 */
@AcrossApplication(modules = {
		DebugWebModule.NAME,
		AcrossWebModule.NAME
})
public class LoggingModuleApplication
{
	public final static String NAME = "LoggingModuleApplication";

	public static void main( String[] args ) {
		AcrossApplicationRunner.run( LoggingModuleApplication.class, args );
	}

	@Bean
	public LoggingModule loggingModule() {
		LoggingModule loggingModule = new LoggingModule();

		// Enable agressive method logging
		loggingModule.setProperty( LoggingModuleSettings.METHOD_LOG_ENABLED, true );
		MethodLogConfiguration methodLogConfiguration = MethodLogConfiguration.all( 1 );
		loggingModule.setProperty( LoggingModuleSettings.METHOD_LOG_CONFIGURATION, methodLogConfiguration );

		RequestLoggerConfiguration requestLoggerConfiguration = RequestLoggerConfiguration.allRequests();
		loggingModule.setProperty( LoggingModuleSettings.REQUEST_LOGGER_CONFIGURATION, requestLoggerConfiguration );

		return loggingModule;
	}
}
