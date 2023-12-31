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
package com.foreach.across.modules.debugweb.controllers;

import com.foreach.across.modules.debugweb.DebugWeb;
import com.foreach.across.modules.debugweb.DebugWebModuleSettings;
import com.foreach.across.modules.debugweb.mvc.DebugWebController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

@DebugWebController
public class DefaultDebugWebController
{
	@Autowired
	private DebugWeb debugWeb;

	@Autowired
	private DebugWebModuleSettings settings;

	@RequestMapping({ "", "/" })
	public String landingPage() {
		String path = settings.getDashboard();

		if ( !StringUtils.equals( path, DebugWebModuleSettings.DEFAULT_DASHBOARD ) ) {
			return debugWeb.redirect( path );
		}

		return "";
	}
}
