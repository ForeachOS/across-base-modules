package com.foreach.across.modules.user.it;

import com.foreach.across.core.AcrossContext;
import com.foreach.across.modules.hibernate.AcrossHibernateModule;
import com.foreach.across.modules.user.UserModule;
import com.foreach.across.modules.user.business.User;
import com.foreach.across.modules.user.business.UserStatus;
import com.foreach.across.modules.user.dto.UserDto;
import com.foreach.across.modules.user.services.UserService;
import com.foreach.across.test.AcrossTestConfiguration;
import com.foreach.across.test.AcrossTestContextConfigurer;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
@ContextConfiguration(classes = ITUserModule.Config.class)
public class ITUserModule
{
	@Autowired
	private UserService userService;

	@Test
	public void verifyBootstrapped() {
		assertNotNull( userService );
		User admin = userService.getUserByUsername( "admin" );
		assertNotNull( admin );
		assertEquals( "admin", admin.getUsername() );
		assertEquals( UserStatus.DEFAULT_USER_STATUS, admin.getStatus() );
		assertEquals( false, admin.getDeleted() );
		assertEquals( true, admin.getEmailConfirmed() );

		assertEquals( true, admin.isEnabled() );
		assertEquals( true, admin.isAccountNonExpired() );
		assertEquals( true, admin.isAccountNonLocked() );
		assertEquals( true, admin.isCredentialsNonExpired() );
	}

	@Test
	public void newlyCreatedUsersHavePositiveIds() {
		UserDto user = new UserDto();
		user.setUsername( RandomStringUtils.randomAscii( 10 ) );
		user.setPassword( RandomStringUtils.random( 30 ) );
		user.setFirstName( RandomStringUtils.random( 25 ) );
		user.setLastName( RandomStringUtils.random( 25 ) );
		user.setDisplayName( RandomStringUtils.random( 50 ) );

		userService.save( user );

		assertTrue( user.getId() > 0 );

		User existing = userService.getUserById( user.getId() );
		assertEquals( user.getUsername(), existing.getUsername() );
		assertEquals( user.getFirstName(), existing.getFirstName() );
		assertEquals( user.getLastName(), existing.getLastName() );
		assertEquals( user.getDisplayName(), existing.getDisplayName() );
		assertNotEquals( user.getPassword(), existing.getPassword() );
	}

	@Test
	public void usersCanHaveNegativeIds() {
		User existing=null;/* = userService.getUserById( -100 );*/
		assertNull( existing );

		UserDto user = new UserDto();
		user.setNewUser( true );
		user.setId( -100 );
		user.setUsername( "test-user:-100" );
		user.setPassword( "test password" );
		user.setFirstName( "Test" );
		user.setLastName( "User" );
		user.setDisplayName( "Display name for test user" );

		userService.save( user );

		existing = userService.getUserById( -100 );
		assertNotNull( existing );

		assertEquals( "test-user:-100", existing.getUsername() );
		assertEquals( "Test", existing.getFirstName() );
		assertEquals( "User", existing.getLastName() );
		assertEquals( "Display name for test user", existing.getDisplayName() );
	}

	@Configuration
	@AcrossTestConfiguration
	static class Config implements AcrossTestContextConfigurer
	{
		@Override
		public void configure( AcrossContext context ) {
			context.addModule( acrossHibernateModule() );
			context.addModule( userModule() );
		}

		private AcrossHibernateModule acrossHibernateModule() {
			return new AcrossHibernateModule();
		}

		private UserModule userModule() {
			return new UserModule();
		}
	}
}