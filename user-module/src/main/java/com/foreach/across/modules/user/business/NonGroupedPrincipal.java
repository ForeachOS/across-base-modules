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
package com.foreach.across.modules.user.business;

import com.foreach.across.modules.hibernate.business.IdBasedEntity;
import com.foreach.across.modules.hibernate.id.AcrossSequenceGenerator;
import com.foreach.across.modules.spring.security.infrastructure.business.AbstractSecurityPrincipal;
import com.foreach.across.modules.user.config.UserSchemaConfiguration;
import com.foreach.across.modules.user.converters.FieldUtils;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.security.core.GrantedAuthority;

import javax.persistence.*;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents a security principal that can be assigned one or more roles.
 *
 * @author Arne Vandamme
 */
@Entity
@Table(name = UserSchemaConfiguration.TABLE_PRINCIPAL)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(
		name = "principal_type",
		discriminatorType = DiscriminatorType.STRING
)
public abstract class NonGroupedPrincipal extends AbstractSecurityPrincipal implements IdBasedSecurityPrincipal
{
	@Id
	@GeneratedValue(generator = "seq_um_principal_id")
	@GenericGenerator(
			name = "seq_um_principal_id",
			strategy = AcrossSequenceGenerator.STRATEGY,
			parameters = {
					@org.hibernate.annotations.Parameter(name = "sequenceName", value = "seq_um_principal_id"),
					@org.hibernate.annotations.Parameter(name = "allocationSize", value = "10")
			}
	)
	private long id;

	@ManyToMany(fetch = FetchType.EAGER)
	@BatchSize(size = 50)
	@JoinTable(
			name = UserSchemaConfiguration.TABLE_PRINCIPAL_ROLE,
			joinColumns = @JoinColumn(name = "principal_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new TreeSet<>();

	@Column(name = "principal_name")
	private String principalName;

	@Override
	public String getPrincipalName() {
		return principalName;
	}

	protected final void setPrincipalName( String principalName ) {
		this.principalName = FieldUtils.lowerCase( principalName );
	}

	@Override
	public long getId() {
		return id;
	}

	public void setId( long id ) {
		this.id = id;
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles( Set<Role> roles ) {
		this.roles = roles;
	}

	public boolean hasRole( String name ) {
		return hasRole( new Role( name ) );
	}

	public boolean hasRole( Role role ) {
		return getRoles().contains( role );
	}

	public boolean hasPermission( String name ) {
		return hasPermission( new Permission( name ) );
	}

	public boolean hasPermission( Permission permission ) {
		for ( Role role : getRoles() ) {
			if ( role.hasPermission( permission ) ) {
				return true;
			}
		}

		return false;
	}

	public Collection<GrantedAuthority> getAuthorities() {
		Collection<GrantedAuthority> authorities = new LinkedHashSet<>();

		for ( Role role : getRoles() ) {
			authorities.add( role );
			for ( Permission permission : role.getPermissions() ) {
				authorities.add( permission );
			}
		}

		return authorities;
	}

	@Override
	public boolean equals( Object o ) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof NonGroupedPrincipal ) ) {
			return false;
		}

		NonGroupedPrincipal that = (NonGroupedPrincipal) o;

		if ( id != that.id ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return (int) ( id ^ ( id >>> 32 ) );
	}
}