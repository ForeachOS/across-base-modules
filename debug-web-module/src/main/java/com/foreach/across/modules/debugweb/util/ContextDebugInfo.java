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
package com.foreach.across.modules.debugweb.util;

import com.foreach.across.core.context.info.AcrossContextInfo;
import com.foreach.across.core.context.info.AcrossModuleInfo;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.util.LinkedList;
import java.util.List;

public class ContextDebugInfo
{
	private final String name;
	private final ApplicationContext applicationContext;
	private boolean enabled;

	private AcrossModuleInfo moduleInfo;
	private AcrossContextInfo contextInfo;

	public ContextDebugInfo( String name, ApplicationContext applicationContext ) {
		this.name = name;
		this.applicationContext = applicationContext;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public Environment getEnvironment() {
		return applicationContext != null ? applicationContext.getEnvironment() : null;
	}

	public String getName() {
		return name;
	}

	public boolean isAcrossContext() {
		return getContextInfo() != null;
	}

	public boolean isModule() {
		return getModuleInfo() != null;
	}

	public AcrossModuleInfo getModuleInfo() {
		return moduleInfo;
	}

	public void setModuleInfo( AcrossModuleInfo moduleInfo ) {
		this.moduleInfo = moduleInfo;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled( boolean enabled ) {
		this.enabled = enabled;
	}

	public AcrossContextInfo getContextInfo() {
		return contextInfo;
	}

	public void setContextInfo( AcrossContextInfo contextInfo ) {
		this.contextInfo = contextInfo;
	}

	public String getLabelColor() {
		if ( moduleInfo == null ) {
			return "info";
		}

		switch ( moduleInfo.getBootstrapStatus() ) {
			case Skipped:
				return "warning";
			case Bootstrapped:
				return "success";
			default:
				return "danger";
		}
	}

	/**
	 * Gathers all debug info for an entire AcrossContext.
	 */
	public static List<ContextDebugInfo> create( AcrossContextInfo context ) {
		LinkedList<ContextDebugInfo> list = new LinkedList<>();

		for ( AcrossModuleInfo moduleInfo : context.getConfiguredModules() ) {
			list.add( createForModule( moduleInfo ) );
		}

		list.addFirst( createForContext( context ) );

		ApplicationContext parent = context.getContext().getParentApplicationContext();

		while ( parent != null ) {
			list.addFirst( createForApplicationContext( parent ) );

			parent = parent.getParent();
		}

		return list;
	}

	private static ContextDebugInfo createForModule( AcrossModuleInfo moduleInfo ) {
		ApplicationContext applicationContext = moduleInfo.isBootstrapped() ? moduleInfo.getApplicationContext() : null;

		ContextDebugInfo debugInfo = new ContextDebugInfo( moduleInfo.getName(), applicationContext );
		debugInfo.setModuleInfo( moduleInfo );
		debugInfo.setEnabled( moduleInfo.isBootstrapped() );

		return debugInfo;
	}

	private static ContextDebugInfo createForContext( AcrossContextInfo context ) {
		ApplicationContext applicationContext = context.getApplicationContext();
		ContextDebugInfo debugInfo = new ContextDebugInfo( applicationContext.getDisplayName(),
		                                                   context.getApplicationContext() );
		debugInfo.setEnabled( true );
		debugInfo.setContextInfo( context );

		return debugInfo;
	}

	private static ContextDebugInfo createForApplicationContext( ApplicationContext applicationContext ) {
		ContextDebugInfo debugInfo = new ContextDebugInfo( applicationContext.getDisplayName(), applicationContext );
		debugInfo.setEnabled( true );

		return debugInfo;
	}
}
