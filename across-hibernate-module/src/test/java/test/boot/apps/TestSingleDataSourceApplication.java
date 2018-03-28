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

package test.boot.apps;

import com.foreach.across.core.context.registry.AcrossContextBeanRegistry;
import com.foreach.across.modules.hibernate.jpa.AcrossHibernateJpaModule;
import com.foreach.across.modules.hibernate.jpa.AcrossHibernateJpaModuleSettings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import test.boot.apps.single.SingleDataSourceApplication;
import test.boot.apps.single.application.Book;
import test.boot.apps.single.application.BookRepository;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Arne Vandamme
 * @since 3.0.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		classes = SingleDataSourceApplication.class,
		properties = {
				"spring.datasource.url=jdbc:hsqldb:mem:single-db",
				"spring.jpa.show-sql=true",
				"spring.transaction.default-timeout=25",
				"acrossHibernate.hibernateProperties[hibernate.hbm2ddl.auto]=create-drop"
		}
)
public class TestSingleDataSourceApplication
{
	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private AcrossContextBeanRegistry beanRegistry;

	@Autowired
	private SingleDataSourceApplication.BookInterceptor bookInterceptor;

	@Test
	public void moduleSettingsShouldBeBound() {
		AcrossHibernateJpaModuleSettings moduleSettings
				= beanRegistry.getBeanOfTypeFromModule( AcrossHibernateJpaModule.NAME, AcrossHibernateJpaModuleSettings.class );

		assertTrue( moduleSettings.isShowSql() );
		assertEquals( Integer.valueOf( 25 ), moduleSettings.getTransactionProperties().getDefaultTimeout() );
	}

	@Test
	public void transactionManagerCreatedAndPropertiesSet() {
		Optional<AbstractPlatformTransactionManager> tm = beanRegistry.findBeanOfTypeFromModule( AcrossHibernateJpaModule.NAME,
		                                                                                         AbstractPlatformTransactionManager.class );

		assertTrue( tm.isPresent() );
		assertEquals( 25, tm.get().getDefaultTimeout() );

		assertEquals( Collections.singletonList( tm.get() ), beanRegistry.getBeansOfType( PlatformTransactionManager.class ) );
	}

	@Test
	public void savingAndFetchingBookIsPossible() {
		Book book = new Book();
		book.setAuthor( "John Doe" );
		book.setName( "John Doe's Biography" );
		book.setPrice( 100 );

		bookRepository.save( book );

		assertThat( bookInterceptor.getCreatedBook() ).isSameAs( book );

		Book fetched = bookRepository.findOneByNameAndAuthor( "John Doe's Biography", "John Doe" );
		assertEquals( book, fetched );
	}
}
