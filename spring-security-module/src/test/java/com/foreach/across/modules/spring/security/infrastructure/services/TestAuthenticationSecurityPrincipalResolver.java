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
import com.foreach.across.modules.spring.security.infrastructure.business.SecurityPrincipalReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Arne Vandamme
 * @since 4.0.0
 */
@ExtendWith(MockitoExtension.class)
public class TestAuthenticationSecurityPrincipalResolver
{
	@Mock
	private SecurityPrincipalService securityPrincipalService;

	@InjectMocks
	private AuthenticationSecurityPrincipalResolver resolver;

	@Test
	public void principalName() {
		SecurityPrincipal securityPrincipal = mock( SecurityPrincipal.class );
		when( securityPrincipalService.getPrincipalByName( "123" ) ).thenReturn( Optional.of( securityPrincipal ) );
		assertEquals( Optional.of( securityPrincipal ), resolver.resolveSecurityPrincipal( authenticationPrincipal( "123" ) ) );

		verify( securityPrincipalService ).getPrincipalByName( any() );
		verifyNoMoreInteractions( securityPrincipalService );
	}

	@Test
	public void principalId() {
		SecurityPrincipal securityPrincipal = mock( SecurityPrincipal.class );
		when( securityPrincipalService.getPrincipalById( SecurityPrincipalId.of( "456" ) ) ).thenReturn( Optional.of( securityPrincipal ) );
		assertEquals( Optional.of( securityPrincipal ), resolver.resolveSecurityPrincipal( authenticationPrincipal( SecurityPrincipalId.of( "456" ) ) ) );

		verify( securityPrincipalService ).getPrincipalById( any() );
		verifyNoMoreInteractions( securityPrincipalService );
	}

	@Test
	public void securityPrincipalReference() {
		SecurityPrincipalReference securityPrincipalReference = mock( SecurityPrincipalReference.class );
		when( securityPrincipalReference.getSecurityPrincipalId() ).thenReturn( SecurityPrincipalId.of( "789" ) );

		SecurityPrincipal securityPrincipal = mock( SecurityPrincipal.class );
		when( securityPrincipalService.getPrincipalById( SecurityPrincipalId.of( "789" ) ) ).thenReturn( Optional.of( securityPrincipal ) );
		assertEquals( Optional.of( securityPrincipal ), resolver.resolveSecurityPrincipal( securityPrincipalReference ) );

		verify( securityPrincipalService ).getPrincipalById( any() );
		verifyNoMoreInteractions( securityPrincipalService );
	}

	@Test
	public void principalNotLoadedIfOfTypeSecurityPrincipal() {
		SecurityPrincipal principal = mock( SecurityPrincipal.class );
		assertEquals( Optional.of( principal ), resolver.resolveSecurityPrincipal( authenticationPrincipal( principal ) ) );
		verifyNoInteractions( securityPrincipalService );
	}

	private Authentication authenticationPrincipal( Object principal ) {
		Authentication auth = mock( Authentication.class );
		when( auth.getPrincipal() ).thenReturn( principal );
		return auth;
	}

}
