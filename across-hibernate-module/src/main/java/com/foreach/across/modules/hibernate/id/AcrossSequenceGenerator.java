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

package com.foreach.across.modules.hibernate.id;

import com.foreach.across.core.database.AcrossSchemaConfiguration;
import com.foreach.across.modules.hibernate.business.SettableIdBasedEntity;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.StandardOptimizerDescriptor;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Properties;

/**
 * Custom TableGenerator strategy for generating long ids.
 * Has support for manually defining an id value and inserting an entity with that value instead.
 * Note that the repository code should support this behavior correctly, and special care should be
 * taken to avoid manual ids interfering with the sequence values.
 * <p/>
 * <p/>
 * <pre>
 *     &#064;GeneratedValue(generator = "seq_um_user_id")
 * 	   &#064;GenericGenerator(
 * 			name = "seq_um_user_id",
 * 			strategy = "com.foreach.across.modules.hibernate.id.AcrossSequenceGenerator",
 * 			parameters = {
 * 					&#064;Parameter(name = "sequenceName", value = "seq_um_user_id"),
 * 					&#064;Parameter(name = "allocationSize", value = "10")
 *            }
 * 	    )
 * </pre>
 * The following parameters can be configured:
 * <ul>
 * <li>sequenceName: mandatory, name of the sequence in the sequences table</li>
 * <li>allowPredefinedIds: true if the generator should support manually inserting ids (default: true)</li>
 * <li>allocationSize: sequence allocation size (default: 50)</li>
 * <li>initialValue: initial sequence value (default: 1)</li>
 * </ul>
 *
 * @see com.foreach.across.core.installers.AcrossSequencesInstaller
 * @see com.foreach.across.modules.hibernate.business.SettableIdBasedEntity
 */
public class AcrossSequenceGenerator extends TableGenerator
{
	// Keep this as a constant string!
	public static final String STRATEGY = "com.foreach.across.modules.hibernate.id.AcrossSequenceGenerator";

	private String entityName;

	private boolean supportPredefinedIds = true;

	@Override
	public void configure( Type type, Properties params, ServiceRegistry serviceRegistry ) throws MappingException {
		entityName = params.getProperty( ENTITY_NAME );
		if ( entityName == null ) {
			throw new MappingException( "no entity name" );
		}

		String pkColumnValue = params.getProperty( "sequenceName" );

		if ( StringUtils.isBlank( pkColumnValue ) ) {
			throw new MappingException( "A sequenceName is required for a Across sequence generator" );
		}

		Properties props = new Properties();
		props.putAll( params );
		props.put( SCHEMA, "" );
		props.put( CATALOG, "" );
		props.put( CONFIG_PREFER_SEGMENT_PER_ENTITY, "true" );
		props.put( TABLE_PARAM, AcrossSchemaConfiguration.TABLE_SEQUENCES );
		props.put( SEGMENT_COLUMN_PARAM, AcrossSchemaConfiguration.SEQUENCE_NAME );
		props.put( SEGMENT_VALUE_PARAM, pkColumnValue );
		props.put( VALUE_COLUMN_PARAM, AcrossSchemaConfiguration.SEQUENCE_VALUE );

		// Unless explicitly overruled, we use a pooled optimizer
		props.put( OPT_PARAM, StandardOptimizerDescriptor.POOLED_LO.getExternalName() );

		// Extend with params
		if ( params.containsKey( OPT_PARAM ) ) {
			props.put( OPT_PARAM, params.getProperty( OPT_PARAM ) );
		}

		int allocationSize = determineAllocationSize( params, 50 );
		props.put( INCREMENT_PARAM, String.valueOf( allocationSize ) );
		props.put( INITIAL_PARAM, String.valueOf( determineInitialValue( params, 1 ) ) );

		if ( params.containsKey( "supportPredefinedIds" ) ) {
			supportPredefinedIds = Boolean.valueOf( params.getProperty( "supportPredefinedIds" ) );
		}

		super.configure( type, props, serviceRegistry );
	}

	private int determineAllocationSize( Properties params, int defaultAllocationSize ) {
		if ( params.containsKey( "allocationSize" ) ) {
			return Integer.valueOf( params.getProperty( "allocationSize" ) );
		}

		return defaultAllocationSize;
	}

	private int determineInitialValue( Properties params, int defaultInitialValue ) {
		if ( params.containsKey( "initialValue" ) ) {
			int initialValue = Integer.valueOf( params.getProperty( "initialValue" ) );

			return initialValue < defaultInitialValue ? defaultInitialValue : initialValue;
		}

		return defaultInitialValue;
	}

	@Override
	public Serializable generate( SharedSessionContractImplementor session, Object object ) {
		Serializable id = session.getEntityPersister( entityName, object )
		                         .getClassMetadata().getIdentifier( object, session );

		if ( supportPredefinedIds ) {
			if ( id != null && !Long.valueOf( 0 ).equals( id ) ) {
				return id;
			}
			else if ( object instanceof SettableIdBasedEntity ) {
				id = ( (SettableIdBasedEntity) object ).getNewEntityId();
				if ( id != null && !Long.valueOf( 0 ).equals( id ) ) {
					return id;
				}
			}
		}

		return super.generate( session, object );
	}
}
