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
package com.foreach.across.modules.hibernate.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.io.Serializable;

/**
 * Shortcut interface combining the default {@link org.springframework.data.jpa.repository.JpaRepository} with a
 * {@link org.springframework.data.jpa.repository.JpaSpecificationExecutor}.
 *
 * @author Arne Vandamme
 */
public interface CommonJpaRepository<T, U extends Serializable> extends JpaRepository<T, U>, JpaSpecificationExecutor<T>
{
}
