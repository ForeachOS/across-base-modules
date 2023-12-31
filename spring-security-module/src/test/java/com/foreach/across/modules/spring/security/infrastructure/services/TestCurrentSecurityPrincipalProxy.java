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

package com.foreach.across.modules.spring.security.infrastructure.services;

import com.foreach.across.modules.spring.security.infrastructure.business.SecurityPrincipal;
import com.foreach.across.modules.spring.security.infrastructure.business.SecurityPrincipalId;
import com.foreach.across.modules.spring.security.infrastructure.config.SecurityInfrastructure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Arne Vandamme
 */
@ExtendWith(MockitoExtension.class)
public class TestCurrentSecurityPrincipalProxy
{
	@Mock(lenient = true)
	private AuthenticationSecurityPrincipalResolver authenticationSecurityPrincipalResolver;

	@Mock(lenient = true)
	private SecurityInfrastructure securityInfrastructure;

	@Mock
	private AuthenticationTrustResolver trustResolver;

	@InjectMocks
	private CurrentSecurityPrincipalProxy currentPrincipal = new CurrentSecurityPrincipalProxyImpl();

	@BeforeEach
	public void before() {
		when( securityInfrastructure.authenticationTrustResolver() ).thenReturn( trustResolver );
	}

	@AfterEach
	public void after() {
		SecurityContextHolder.clearContext();
	}

	@Test
	public void principalIdIsNullIfPrincipalNameIsNotPresent() {
		SecurityPrincipal principal = mock( SecurityPrincipal.class );
		doCallRealMethod().when( principal ).getSecurityPrincipalId();

		when( principal.getPrincipalName() ).thenReturn( "" );
		assertNull( principal.getSecurityPrincipalId() );
		when( principal.getPrincipalName() ).thenReturn( null );
		assertNull( principal.getSecurityPrincipalId() );
	}

	@Test
	public void principalLoadedOnlyIfNecessary() {
		SecurityPrincipal principal = mock( SecurityPrincipal.class );

		Authentication auth = mock( Authentication.class );
		when( auth.isAuthenticated() ).thenReturn( true );

		SecurityContextHolder.getContext().setAuthentication( auth );

		when( authenticationSecurityPrincipalResolver.resolveSecurityPrincipal( auth ) ).thenReturn( Optional.of( principal ) );

		assertSame( principal, currentPrincipal.getPrincipal() );
		assertSame( principal, currentPrincipal.getPrincipal() );

		verify( authenticationSecurityPrincipalResolver, times( 2 ) ).resolveSecurityPrincipal( auth );
	}

	@Test
	public void principalLoadedEvenIfNull() {
		Authentication auth = mock( Authentication.class );
		when( auth.isAuthenticated() ).thenReturn( true );

		SecurityContextHolder.getContext().setAuthentication( auth );

		assertNull( currentPrincipal.getPrincipal() );
		assertNull( currentPrincipal.getPrincipal() );

		verify( authenticationSecurityPrincipalResolver, times( 2 ) ).resolveSecurityPrincipal( auth );
	}

	@Test
	public void principalLoadedWithSecurityPrincipalId() {
		Authentication auth = mock( Authentication.class );
		SecurityPrincipalId principalId = SecurityPrincipalId.of( "principal" );
		when( auth.isAuthenticated() ).thenReturn( true );

		SecurityContextHolder.getContext().setAuthentication( auth );
		assertNull( currentPrincipal.getPrincipal() );

		verify( authenticationSecurityPrincipalResolver ).resolveSecurityPrincipal( auth );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void hasAuthority() {
		assertFalse( currentPrincipal.hasAuthority( "some authority" ) );

		Collection authorities = new HashSet<>();
		authorities.add( new SimpleGrantedAuthority( "some authority" ) );

		SecurityPrincipal principal = mock( SecurityPrincipal.class );

		Authentication auth = mock( Authentication.class );
		when( auth.getAuthorities() ).thenReturn( authorities );
		when( auth.isAuthenticated() ).thenReturn( true );

		SecurityContextHolder.getContext().setAuthentication( auth );

		assertTrue( currentPrincipal.hasAuthority( "some authority" ) );
		assertTrue( currentPrincipal.hasAuthority( new SimpleGrantedAuthority( "some authority" ) ) );
		assertTrue( currentPrincipal.hasAuthority( new SimpleGrantedAuthority( "some authority" ) ) );
	}

	@Test
	public void typedPrincipalIsOnlyReturnedIfTypeMatches() {
		SecurityPrincipal principal = mock( SecurityPrincipal.class );

		Authentication auth = mock( Authentication.class );
		when( auth.isAuthenticated() ).thenReturn( true );

		SecurityContextHolder.getContext().setAuthentication( auth );

		when( authenticationSecurityPrincipalResolver.resolveSecurityPrincipal( auth ) ).thenReturn( Optional.of( principal ) );

		assertSame( principal, currentPrincipal.getPrincipal() );
		assertSame( principal, currentPrincipal.getPrincipal( SecurityPrincipal.class ) );
		assertNull( currentPrincipal.getPrincipal( SpecificPrincipal.class ) );
	}

	@Test
	public void notAuthenticatedIfTrustResolverClaimsAnonymous() {
		Authentication auth = new AnonymousAuthenticationToken( "key", "anonymousUser", AuthorityUtils.createAuthorityList( "ROLE_ANONYMOUS" ) );
		when( trustResolver.isAnonymous( auth ) ).thenReturn( true );

		SecurityContextHolder.getContext().setAuthentication( auth );

		assertFalse( currentPrincipal.isAuthenticated() );
		assertEquals( "anonymousUser", currentPrincipal.getPrincipalName() );
		assertEquals( "anonymousUser", currentPrincipal.toString() );
	}

	interface SpecificPrincipal extends SecurityPrincipal
	{

	}
}
