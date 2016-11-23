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

import com.foreach.across.core.annotations.AcrossDepends;
import com.foreach.across.modules.debugweb.DebugWeb;
import com.foreach.across.modules.debugweb.DebugWebModule;
import com.foreach.across.modules.debugweb.DebugWebModuleSettings;
import com.foreach.across.modules.spring.security.configuration.SpringSecurityWebConfigurerAdapter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import java.util.Arrays;

import static com.foreach.across.modules.debugweb.DebugWebModuleSettings.*;

@Configuration
@AcrossDepends(required = { DebugWebModule.NAME})
@ConditionalOnClass(SpringSecurityWebConfigurerAdapter.class)
@ConditionalOnProperty( value = SECURITY_ENABLED, matchIfMissing = true )
public class DebugWebSecurityConfiguration extends SpringSecurityWebConfigurerAdapter {

    @Autowired
    private DebugWebModuleSettings debugWebModuleSettings;

    @Autowired
    private DebugWeb debugWeb;

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        String password = debugWebModuleSettings.getSecurity().getPassword();
        if( StringUtils.isEmpty( password ) ) {
            throw new IllegalArgumentException( "debugWebModule.security.password cannot be empty when debugWebModule.security.enabled=true" );
        }
        auth.inMemoryAuthentication().withUser( debugWebModuleSettings.getSecurity().getUsername() ).password( debugWebModuleSettings.getSecurity().getPassword() ).roles("DEBUG_USER");
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        String ipAddressesExpression = buildIpAddressExpression();
        String accessExpression = StringUtils.length( ipAddressesExpression ) > 0 ?  ipAddressesExpression + " or " : StringUtils.EMPTY;
        http.antMatcher( debugWeb.path( "/**" ) )
                // Allow a set of IPs without a password and allow non-known IPs with a password
                .authorizeRequests().anyRequest().access( accessExpression + "hasRole('ROLE_DEBUG_USER')" )
                .and()
                .formLogin().disable()
                .httpBasic()
                .and()
                .sessionManagement().sessionCreationPolicy( SessionCreationPolicy.STATELESS)
                .and()
                .csrf().disable();
    }

    private String buildIpAddressExpression() {
        String ipAddresses = debugWebModuleSettings.getSecurity().getIpAddresses();
        if( ipAddresses != null ) {
            StringBuilder sb = new StringBuilder();
            for( String ipAddress : StringUtils.split( ipAddresses, ";" ) ) {
                if( sb.length() != 0 ) {
                    sb.append( " or " );
                }
                sb.append( "hasIpAddress('" ).append( StringUtils.trim( ipAddress ) ).append( "')" );
            }
            return sb.toString();
        } else {
            return StringUtils.EMPTY;
        }
    }
}
