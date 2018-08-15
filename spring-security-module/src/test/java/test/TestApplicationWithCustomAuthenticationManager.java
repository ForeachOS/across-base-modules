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

package test;

import com.foreach.across.test.support.config.MockAcrossServletContextInitializer;
import com.foreach.across.test.support.config.MockMvcConfiguration;
import lombok.SneakyThrows;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import test.app.SpringSecurityTestApplication;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Arne Vandamme
 * @since 3.0.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@DirtiesContext
@SpringBootTest(classes = { SpringSecurityTestApplication.class, MockMvcConfiguration.class })
@ActiveProfiles("custom-auth")
@ContextConfiguration(initializers = MockAcrossServletContextInitializer.class)
public class TestApplicationWithCustomAuthenticationManager
{
	@Autowired
	private MockMvc mockMvc;

	@Test
	@Ignore("Default configuration only - exception for blocked not added")
	@SneakyThrows
	public void blockedShouldNotBeAllowed() {
		mockMvc.perform( get( "/blocked" ) )
		       .andExpect( status().isUnauthorized() );
		mockMvc.perform( get( "/blocked" ).with( httpBasic( "dashboard", "dashboard" ) ) )
		       .andExpect( status().isForbidden() );
	}

	@Test
	@SneakyThrows
	public void helloShouldBeSecured() {
		mockMvc.perform( get( "/hello" ) )
		       .andExpect( status().isUnauthorized() );
	}

	@Test
	@SneakyThrows
	public void helloCanBeCalledWithCustomUser() {
		mockMvc.perform( get( "/hello" ).with( httpBasic( "dashboard", "dashboard" ) ) )
		       .andExpect( status().isOk() )
		       .andExpect( content().string( "hello" ) );
	}

	@Test
	@Ignore("As of Boot 2.0 everything is secured by default")
	@SneakyThrows
	public void helloPublicShouldNotBeSecured() {
		mockMvc.perform( get( "/hello-public" ) )
		       .andExpect( status().isOk() )
		       .andExpect( content().string( "hello-public" ) );
	}

	@Test
	@Ignore("As of Boot 2.0 everything is secured by default")
	@SneakyThrows
	public void errorPageShouldNotBeSecured() {
		mockMvc.perform( get( "/error" ) )
		       .andExpect( status().isInternalServerError() )
		       .andExpect( content().string( containsString( "No message available" ) ) );
	}
}
