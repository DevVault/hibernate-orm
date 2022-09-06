/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.mapping.basic.bitset;

import java.sql.Types;
import java.util.BitSet;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import org.assertj.core.api.Assertions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isOneOf;

/**
 * Test using a converter to map the BitSet
 */
@DomainModel(annotatedClasses = BitSetConverterImmutableTests.Product.class)
@SessionFactory
public class BitSetConverterImmutableTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final MappingMetamodelImplementor mappingMetamodel = sessionFactory.getRuntimeMetamodels()
				.getMappingMetamodel();

		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor(Product.class);
		final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("bitSet");
		final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();

		assertThat( attributeMapping.getJavaType().getJavaTypeClass(), equalTo( BitSet.class ) );

		assertThat( jdbcMapping, instanceOf( ConvertedBasicTypeImpl.class ) );
		final BasicValueConverter<?, ?> converter = jdbcMapping.getValueConverter();
		assertThat(
				converter,
				instanceOf( JpaAttributeConverter.class )
		);
		assertThat(
				( (JpaAttributeConverter<?, ?>) converter ).getConverterBean().getBeanClass(),
				equalTo( BitSetConverter.class )
		);

		Assertions.assertThat(attributeMapping.getExposedMutabilityPlan()).isNotInstanceOf(BitSetMutabilityPlan.class);
		Assertions.assertThat(attributeMapping.getExposedMutabilityPlan()).isInstanceOf(ImmutableMutabilityPlan.class);
		Assertions.assertThat(attributeMapping.getExposedMutabilityPlan().isMutable()).isFalse();

		final BitSet sample = new BitSet();
		Assertions.assertThat(((MutabilityPlan) attributeMapping.getExposedMutabilityPlan()).deepCopy(sample)).isSameAs(sample);

		assertThat(
				jdbcMapping.getJdbcType().getJdbcTypeCode(),
				isOneOf(Types.VARCHAR, Types.NVARCHAR)
		);

		assertThat(converter.getRelationalJavaType().getJavaTypeClass(), equalTo(String.class));
	}

	@Table(name = "products")
	//tag::basic-bitset-example-convert[]
	@Entity(name = "Product")
	public static class Product {
		@Id
		private Integer id;

		@Convert(converter = BitSetConverter.class)
		private BitSet bitSet;

		//Getters and setters are omitted for brevity
		//end::basic-bitset-example-convert[]

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public BitSet getBitSet() {
			return bitSet;
		}

		public void setBitSet(BitSet bitSet) {
			this.bitSet = bitSet;
		}
		//tag::basic-bitset-example-convert[]
	}
	//end::basic-bitset-example-convert[]


	//tag::basic-bitset-example-converter[]
	@Immutable
	@Converter(autoApply = true)
	public static class BitSetConverter implements AttributeConverter<BitSet,String> {
		@Override
		public String convertToDatabaseColumn(BitSet attribute) {
			return BitSetHelper.bitSetToString(attribute);
		}

		@Override
		public BitSet convertToEntityAttribute(String dbData) {
			return BitSetHelper.stringToBitSet(dbData);
		}
	}
	//end::basic-bitset-example-converter[]
}