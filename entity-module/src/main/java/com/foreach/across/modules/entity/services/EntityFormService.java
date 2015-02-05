package com.foreach.across.modules.entity.services;

import com.foreach.across.core.annotations.RefreshableCollection;
import com.foreach.across.modules.entity.business.EntityForm;
import com.foreach.across.modules.entity.business.FormPropertyDescriptor;
import com.foreach.across.modules.entity.registry.EntityConfiguration;
import com.foreach.across.modules.entity.registry.properties.EntityPropertyDescriptor;
import com.foreach.across.modules.entity.registry.properties.EntityPropertyRegistry;
import com.foreach.across.modules.entity.registry.properties.MergingEntityPropertyRegistry;
import com.foreach.across.modules.entity.support.EntityMessageCodeResolver;
import com.foreach.across.modules.entity.views.ConfigurablePropertiesEntityViewFactorySupport;
import com.foreach.across.modules.entity.views.EntityFormViewFactory;
import com.foreach.across.modules.entity.views.EntityListViewFactory;
import com.foreach.across.modules.entity.views.elements.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;

/**
 *
 */
@Service
public class EntityFormService
{
	private static final Logger LOG = LoggerFactory.getLogger( EntityFormService.class );

	@RefreshableCollection(incremental = true, includeModuleInternals = true)
	private Collection<ViewElementTypeLookupStrategy> elementTypeLookupStrategies;

	@RefreshableCollection(incremental = true, includeModuleInternals = true)
	private Collection<ViewElementBuilderFactoryAssembler> builderFactoryAssemblers;

	@Autowired(required = false)
	private ConversionService conversionService;

	@PostConstruct
	protected void createDefaultConversionService() {
		if ( conversionService == null ) {
			LOG.info(
					"No ConversionService found for the EntityModule - creating default conversion service for views." );
			conversionService = new DefaultFormattingConversionService();
		}
	}

	public ViewElement createFormElement( EntityConfiguration entityConfiguration,
	                                      EntityPropertyRegistry entityPropertyRegistry,
	                                      EntityPropertyDescriptor descriptor,
	                                      EntityMessageCodeResolver messageCodeResolver ) {
		ViewElementBuilderFactory builderFactory
				= getOrCreateBuilderFactory( entityConfiguration, entityPropertyRegistry, descriptor );

		if ( builderFactory != null ) {
			ViewElementBuilder builder = builderFactory.createBuilder();

			if ( builder != null ) {
				builder.setMessageCodeResolver( messageCodeResolver );

				return builder.createViewElement();
			}
		}

		return null;
	}

	public ViewElement createViewElement( EntityConfiguration entityConfiguration,
	                                      EntityPropertyDescriptor descriptor,
	                                      EntityMessageCodeResolver messageCodeResolver,
	                                      ConfigurablePropertiesEntityViewFactorySupport factory ) {
		if ( factory instanceof EntityListViewFactory ) {
			return new ConversionServiceViewElement( messageCodeResolver, conversionService, descriptor );
		}
		else if ( factory instanceof EntityFormViewFactory ) {

			EntityPropertyRegistry registry = new MergingEntityPropertyRegistry(
					entityConfiguration.getPropertyRegistry()
			);

			return createFormElement( entityConfiguration, registry, descriptor,
			                          messageCodeResolver );
		}
		return null;
	}

	public ViewElementBuilderFactory getOrCreateBuilderFactory(
			EntityConfiguration entityConfiguration,
			EntityPropertyRegistry entityPropertyRegistry,
			EntityPropertyDescriptor propertyDescriptor
	) {
		// Get builder factory
		ViewElementBuilderFactory builderFactory = propertyDescriptor.getAttribute( ViewElementBuilderFactory.class );

		if ( builderFactory == null ) {
			builderFactory = createBuilderFactory( entityConfiguration, entityPropertyRegistry, propertyDescriptor );
		}

		return builderFactory;
	}

	public ViewElementBuilderFactory createBuilderFactory(
			EntityConfiguration entityConfiguration,
			EntityPropertyRegistry entityPropertyRegistry,
			EntityPropertyDescriptor propertyDescriptor
	) {
		String elementType = findElementType( entityConfiguration, propertyDescriptor );

		if ( elementType == null ) {
			return null;
		}

		ViewElementBuilderFactoryAssembler builderFactoryAssembler = findAssemblerForType( elementType );

		if ( builderFactoryAssembler == null ) {
			return null;
		}

		return builderFactoryAssembler
				.createBuilderFactory( entityConfiguration, entityPropertyRegistry, propertyDescriptor );
	}

	private String findElementType( EntityConfiguration entityConfiguration, EntityPropertyDescriptor descriptor ) {
		for ( ViewElementTypeLookupStrategy lookupStrategy : elementTypeLookupStrategies ) {
			String elementType = lookupStrategy.findElementType( entityConfiguration, descriptor );
			if ( elementType != null ) {
				return elementType;
			}
		}

		return null;
	}

	private ViewElementBuilderFactoryAssembler findAssemblerForType( String elementType ) {
		for ( ViewElementBuilderFactoryAssembler assembler : builderFactoryAssemblers ) {
			if ( assembler.supports( elementType ) ) {
				return assembler;
			}
		}

		return null;
	}

	@Deprecated
	public EntityForm create( Collection<FormPropertyDescriptor> descriptors ) {
		EntityForm form = new EntityForm();

//		for ( FormPropertyDescriptor descriptor : descriptors ) {
//			descriptor.setDisplayName( StringUtils.capitalize( descriptor.getName() ) );
//
//			if ( descriptor.isReadable() && descriptor.isWritable() ) {
//				if ( descriptor.getPropertyType().equals( boolean.class )
//						|| descriptor.getPropertyType().equals( Boolean.class ) ) {
//					form.addElement( new CheckboxFormElement( descriptor ) );
//				}
//				else if ( Collection.class.isAssignableFrom( descriptor.getPropertyType() ) ) {
//					ResolvableType type = descriptor.getPropertyResolvableType();
//
//					if ( type.hasGenerics() ) {
//						Class itemType = type.getGeneric( 0 ).resolve();
//
//						Collection<?> possibleValues = Collections.emptyList();
//
//						if ( itemType.isEnum() ) {
//							possibleValues = Arrays.asList( itemType.getEnumConstants() );
//						}
//						else {
//							EntityConfiguration itemEntityType = entityRegistry.getEntityByClass( itemType );
//
//							if ( itemEntityType != null ) {
//								possibleValues = itemEntityType.getRepository().findAll();
//							}
//						}
//
//						form.addElement( new MultiCheckboxFormElement( entityRegistry, descriptor, possibleValues ) );
//
//					}
//				}
//				else if ( entityRegistry.getEntityByClass( descriptor.getPropertyType() ) != null ) {
//					EntityConfiguration itemEntityType = entityRegistry.getEntityByClass(
//							descriptor.getPropertyType() );
//
//					form.addElement(
//							new SelectFormElement( entityRegistry, descriptor, itemEntityType.getRepository().findAll() )
//					);
//				}
//				else {
//					form.addElement( new TextboxFormElement( descriptor ) );
//				}
//			}
//		}

		return form;
	}

	public EntityForm create( EntityConfiguration entityConfiguration ) {
		Class entityType = entityConfiguration.getEntityType();

		List<EntityPropertyDescriptor> descriptors = entityConfiguration.getPropertyRegistry().getProperties();

		EntityForm form = new EntityForm();

//		for ( EntityPropertyDescriptor descriptor : descriptors ) {
//			if ( descriptor.isHidden() ) {
//				form.addElement( new HiddenFormElement( descriptor ) );
//			}
//			else if ( descriptor.isReadable() && !descriptor.isWritable() ) {
//				form.addElement( new TextFormElement( descriptor ) );
//			}
//			else if ( descriptor.isWritable() && descriptor.isReadable() ) {
//				if ( descriptor.getPropertyType().equals( boolean.class ) || descriptor.getPropertyType().equals(
//						Boolean.class ) ) {
//					form.addElement( new CheckboxFormElement( descriptor ) );
//				}
//				else if ( Collection.class.isAssignableFrom( descriptor.getPropertyType() ) ) {
//					/*ResolvableType type = ResolvableType.forMethodParameter( descriptor.getWriteMethod(), 0 );
//
//					if ( type.hasGenerics() ) {
//						Class itemType = type.getGeneric( 0 ).resolve();
//
//						Collection<?> possibleValues = Collections.emptyList();
//
//						if ( itemType.isEnum() ) {
//							possibleValues = Arrays.asList( itemType.getEnumConstants() );
//						}
//						else {
//							EntityConfiguration itemEntityType = entityRegistry.getEntityByClass( itemType );
//
//							if ( itemEntityType != null ) {
//								possibleValues = itemEntityType.getRepository().findAll();
//							}
//						}
//
//						form.addElement( new MultiCheckboxFormElement( entityRegistry, descriptor, possibleValues ) );
//
//					}*/
//				}
//				else if ( entityRegistry.contains( descriptor.getPropertyType() ) ) {
//					/*EntityConfiguration itemEntityType = entityRegistry.getEntityByClass(
//							descriptor.getPropertyType() );
//
//					form.addElement(
//							new SelectFormElement( entityRegistry, descriptor, itemEntityType.getRepository().findAll() )
//					);*/
//				}
//				else {
//					form.addElement( new TextboxFormElement( descriptor ) );
//				}
//			}
//		}

		return form;
	}
}
