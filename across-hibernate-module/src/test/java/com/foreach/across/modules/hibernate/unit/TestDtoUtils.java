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
package com.foreach.across.modules.hibernate.unit;

import com.foreach.across.modules.hibernate.business.EntityWithDto;
import com.foreach.across.modules.hibernate.util.DtoUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.junit.Test;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.*;

/**
 * @author Arne Vandamme
 */
public class TestDtoUtils
{
	public static class Entity
	{
		private String name;
		private Set<String> values;

		public String getName() {
			return name;
		}

		public void setName( String name ) {
			this.name = name;
		}

		public Set<String> getValues() {
			return values;
		}

		public void setValues( Set<String> values ) {
			this.values = values;
		}
	}

	@Getter
	@Setter
	@EqualsAndHashCode
	public static class HierarchicalEntity implements EntityWithDto<HierarchicalEntity>
	{
		private String name;
		private HierarchicalEntity parent;

		@Override
		public HierarchicalEntity toDto() {
			return DtoUtils.createDto( this );
		}
	}

	@Setter
	@Getter
	@EqualsAndHashCode
	public static class CollectionEntity implements EntityWithDto<CollectionEntity>
	{
		private Collection<HierarchicalEntity> values;

		@Override
		public CollectionEntity toDto() {
			return DtoUtils.createDto( this );
		}
	}

	@Setter
	@Getter
	public static class ArrayEntity implements EntityWithDto<ArrayEntity>
	{
		private HierarchicalEntity[] values;

		@Override
		public ArrayEntity toDto() {
			return DtoUtils.createDto( this );
		}
	}

	@Setter
	@Getter
	@EqualsAndHashCode(of = "id")
	public static class CampaignAsset implements EntityWithDto<CampaignAsset>
	{
		private Long id;
		private Campaign campain;

		@Override
		public CampaignAsset toDto() {
			return DtoUtils.createDto( this );
		}
	}

	@Setter
	@Getter
	@EqualsAndHashCode(of = "id")
	public static class Campaign implements EntityWithDto<Campaign>
	{
		private Long id;
		private List<CampaignAsset> assets;

		@Override
		public Campaign toDto() {
			return DtoUtils.createDto( this );
		}
	}

	@Test
	public void collectionsAreNotCloned() {
		Entity one = new Entity();
		one.setName( "one" );
		one.setValues( Collections.singleton( "test" ) );

		Entity dto = DtoUtils.createDto( one );
		assertNotNull( dto );
		assertNotSame( one, dto );
		assertEquals( one.getName(), dto.getName() );
		assertEquals( one.getValues(), dto.getValues() );
	}

	@Test
	public void nestedEntitiesAreNotCloned() {
		HierarchicalEntity child = new HierarchicalEntity();
		child.setName( "child" );

		HierarchicalEntity parent = new HierarchicalEntity();
		parent.setName( "parent" );
		child.setParent( parent );

		HierarchicalEntity dto = DtoUtils.createDto( child );
		assertNotSame( child, dto );
		assertHierarchicalEntity( child, dto );
	}

	@Test
	public void arraysAreNotDeepCloned() {
		HierarchicalEntity child = new HierarchicalEntity();
		child.setName( "child" );

		HierarchicalEntity parent = new HierarchicalEntity();
		parent.setName( "parent" );
		child.setParent( parent );

		ArrayEntity collection = new ArrayEntity();
		collection.setValues( new HierarchicalEntity[] { child } );

		ArrayEntity dto = DtoUtils.createDto( collection );
		assertNotNull( dto );
		assertNotSame( collection, dto );
		assertEquals( collection.getValues(), dto.getValues() );
		assertEquals( collection.getValues().length, dto.getValues().length );
		assertEquals( 1, collection.getValues().length );

		HierarchicalEntity collectionItem = collection.getValues()[0];
		HierarchicalEntity dtoCollectionItem = dto.getValues()[0];
		assertSame( collectionItem, dtoCollectionItem );
		assertHierarchicalEntity( collectionItem, dtoCollectionItem );
	}

	@Test
	public void collectionsOfEntitiesAreNotDeepCloned() {
		HierarchicalEntity child = new HierarchicalEntity();
		child.setName( "child" );

		HierarchicalEntity child2 = new HierarchicalEntity();
		child2.setName( "child2" );

		HierarchicalEntity parent = new HierarchicalEntity();
		parent.setName( "parent" );
		child.setParent( parent );

		CollectionEntity collection = new CollectionEntity();
		collection.setValues( new ArrayList<>( Collections.singletonList( child ) ) );
		assertCollectionToDto( collection );

		collection.setValues( new HashSet<>( Collections.singletonList( child ) ) );
		assertCollectionToDto( collection );

		collection.setValues( new HashSet<>( Arrays.asList( child, child2 ) ) );
		assertCollectionToDto( collection );

		collection.setValues( Collections.singletonList( child ) );
		assertCollectionToDto( collection );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> collection.getValues().add( child2 ) );

		collection.setValues( Arrays.asList( child, child2 ) );
		assertCollectionToDto( collection );
	}

	@Test
	public void cglibEnhancedType() {
		HierarchicalEntity child = new HierarchicalEntity();
		child.setName( "child" );

		HierarchicalEntity parent = new HierarchicalEntity();
		parent.setName( "parent" );
		child.setParent( parent );

		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass( HierarchicalEntity.class );
		enhancer.setCallback( (MethodInterceptor) ( obj, method, args, proxy ) -> method.invoke( child, args ) );

		HierarchicalEntity dto = (HierarchicalEntity) DtoUtils.createDto( enhancer.create() );
		assertThat( dto.getClass() ).isEqualTo( HierarchicalEntity.class );
		assertHierarchicalEntity( child, dto );
	}

	@Test
	public void bidirectionalRelationship() {
		Campaign campaign = new Campaign();
		campaign.setId( 1L );

		CampaignAsset asset1 = new CampaignAsset();
		asset1.setId( 1L );
		asset1.setCampain( campaign );

		CampaignAsset asset2 = new CampaignAsset();
		asset2.setId( 2L );
		asset2.setCampain( campaign );

		campaign.setAssets( Arrays.asList( asset1, asset2 ) );

		CampaignAsset dto = asset1.toDto();
		assertThat( asset1 ).isEqualTo( dto )
		                    .isNotSameAs( dto );
		assertThat( asset1.getCampain() ).isEqualTo( dto.getCampain() )
		                                 .isSameAs( dto.getCampain() );

		List<CampaignAsset> originalAssets = asset1.getCampain().getAssets();
		List<CampaignAsset> clonedAssets = dto.getCampain().getAssets();
		assertThat( originalAssets )
				.isEqualTo( clonedAssets )
				.isSameAs( clonedAssets )
				.containsExactly( clonedAssets.toArray( new CampaignAsset[0] ) );

		assertThat( clonedAssets.get( 0 ) )
				.isEqualTo( asset1 )
				.isSameAs( asset1 )
				.isEqualTo( dto )
				.isNotSameAs( dto );

		assertThat( clonedAssets.get( 1 ) )
				.isEqualTo( asset2 )
				.isSameAs( asset2 );
	}

	private void assertCollectionToDto( CollectionEntity collection ) {
		CollectionEntity dto = DtoUtils.createDto( collection );
		assertNotNull( dto );
		assertNotSame( collection, dto );
		assertEquals( collection.getValues().size(), dto.getValues().size() );
		assertEquals( collection.getValues(), dto.getValues() );

		HierarchicalEntity[] originalValues = collection.getValues().toArray( new HierarchicalEntity[0] );
		HierarchicalEntity[] clonedValues = dto.getValues().toArray( new HierarchicalEntity[0] );
		for ( int i = 0; i < collection.getValues().size(); i++ ) {
			assertSame( originalValues[i], clonedValues[i] );
			assertHierarchicalEntity( originalValues[i], clonedValues[i] );
		}
	}

	private void assertHierarchicalEntity( HierarchicalEntity original, HierarchicalEntity deepCopy ) {
		if ( original.getParent() == null ) {
			assertNull( deepCopy.getParent() );
		}
		else {
			assertSame( original.getParent(), deepCopy.getParent() );
			assertEquals( original.getParent().getName(), deepCopy.getParent().getName() );
			assertNull( original.getParent().getParent() );
			assertNull( deepCopy.getParent().getParent() );

		}
		assertEquals( original.getParent(), deepCopy.getParent() );
	}
}
