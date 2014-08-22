package com.foreach.across.modules.user.business;

import com.foreach.across.modules.hibernate.id.AcrossSequenceGenerator;
import com.foreach.across.modules.user.config.UserSchemaConfiguration;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.persistence.*;
import java.util.Collection;
import java.util.HashSet;
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
public abstract class AbstractPrincipal
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
		Collection<GrantedAuthority> authorities = new HashSet<>();

		for ( Role role : getRoles() ) {
			authorities.add( new SimpleGrantedAuthority( role.getName() ) );
			for ( Permission permission : role.getPermissions() ) {
				authorities.add( new SimpleGrantedAuthority( permission.getName() ) );
			}
		}

		return authorities;
	}
}