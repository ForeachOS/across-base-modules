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
package com.foreach.across.modules.hibernate.jpa.aop;

import com.foreach.across.modules.hibernate.modules.config.EnableTransactionManagementConfiguration;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;

/**
 * @author Andy Somers
 */
public class JpaRepositoryInterceptorAdvisor extends AbstractBeanFactoryPointcutAdvisor
{
	/**
	 * By default the interceptor should run within the same transaction.
	 */
	@SuppressWarnings("unused")
	public static final int INTERCEPT_ORDER = EnableTransactionManagementConfiguration.INTERCEPT_ORDER + 1;
	private static final long serialVersionUID = 2973014201873864201L;

	private final transient Pointcut pointcut = new JpaRepositoryPointcut();

	@Override
	public Pointcut getPointcut() {
		return pointcut;
	}
}
