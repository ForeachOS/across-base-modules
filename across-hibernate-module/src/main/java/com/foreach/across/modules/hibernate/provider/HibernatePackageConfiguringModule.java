package com.foreach.across.modules.hibernate.provider;

/**
 * Interface to be implemented by all AcrossModules that provide their own Hibernate packages.
 * A HibernatePackageRegistry is identified by its name, usually the same as the module the
 * package belongs to.
 *
 * @deprecated use {@link HibernatePackageConfigurer} instead
 */
@Deprecated
public interface HibernatePackageConfiguringModule extends HibernatePackageConfigurer
{
	void configureHibernatePackage( HibernatePackageRegistry hibernatePackage );
}
