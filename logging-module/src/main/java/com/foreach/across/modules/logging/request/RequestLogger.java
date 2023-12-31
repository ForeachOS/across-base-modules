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
package com.foreach.across.modules.logging.request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Andy Somers
 */
public enum RequestLogger
{
	// Do not log any requests
	NONE,
	// Log requests through the RequestLoggerFilter
	FILTER,
	// Log requests through the RequestLogInterceptor
	INTERCEPTOR;

	public static final Logger LOG = LoggerFactory.getLogger( RequestLogger.class );
}
