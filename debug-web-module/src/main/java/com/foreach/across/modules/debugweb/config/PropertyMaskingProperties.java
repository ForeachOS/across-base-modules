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

package com.foreach.across.modules.debugweb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Arne Vandamme
 */
@Component
@ConfigurationProperties(prefix = "debug-web-module.properties")
public class PropertyMaskingProperties
{
	/**
	 * Set regular expressions for all property names that should be masked.
	 */
	private String[] masks = new String[] { ".*password.*", ".*secret.*" };

	/**
	 * Set explicit property names that should be masked.
	 */
	private String[] maskedProperties = new String[0];

	public String[] getMasks() {
		return masks.clone();
	}

	public void setMasks( String[] masks ) {
		this.masks = masks.clone();
	}

	public String[] getMaskedProperties() {
		return maskedProperties.clone();
	}

	public void setMaskedProperties( String[] maskedProperties ) {
		this.maskedProperties = maskedProperties.clone();
	}
}
