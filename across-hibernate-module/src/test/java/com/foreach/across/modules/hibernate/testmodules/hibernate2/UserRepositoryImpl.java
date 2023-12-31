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
package com.foreach.across.modules.hibernate.testmodules.hibernate2;

import com.foreach.across.modules.hibernate.repositories.BasicRepositoryImpl;
import com.foreach.across.modules.hibernate.testmodules.hibernate1.Product;
import com.foreach.across.modules.hibernate.testmodules.hibernate1.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRepositoryImpl extends BasicRepositoryImpl<User> implements UserRepository
{
	@Autowired(required = false)
	private ProductRepository productRepository;

	@Transactional
	public void save( User user, Product product ) {
		productRepository.save( product );

		if ( user != null ) {
			update( user );
		}
		else {
			throw new RuntimeException( "rollback transaction" );
		}
	}
}
