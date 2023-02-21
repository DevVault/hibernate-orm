/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.orm.test.query.sql;

import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		NativeQueryEagerAssociationTest.Building.class,
		NativeQueryEagerAssociationTest.Classroom.class
})
@JiraKey("HHH-16191")
public class NativeQueryEagerAssociationTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Building building1 = new Building( 1L, "building_1" );
			final Building building2 = new Building( 2L, "building_2" );
			final Building building3 = new Building( 3L, "building_3" );
			session.persist( building1 );
			session.persist( building2 );
			session.persist( building3 );
			session.persist( new Classroom( 1L, "classroom_1", building1, List.of( building2, building3 ) ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Classroom" ).executeUpdate();
			session.createMutationQuery( "delete from Building" ).executeUpdate();
		} );
	}

	@Test
	public void testNativeQuery(SessionFactoryScope scope) {
		final Classroom result = scope.fromTransaction(
				session -> session.createNativeQuery( "select id, description, building_id from classroom", Classroom.class ).getSingleResult()
		);
		assertEquals( 1L, result.getId() );
		assertTrue( Hibernate.isInitialized( result.getBuilding() ) );
		assertTrue( Hibernate.isInitialized( result.getAdjacentBuildings() ) );
		assertEquals( 1L, result.getBuilding().getId() );
		assertEquals( 2, result.getAdjacentBuildings().size() );
	}

	@Entity(name = "Building")
	public static class Building {
		@Id
		private Long id;

		private String description;

		public Building() {
		}

		public Building(Long id, String description) {
			this.id = id;
			this.description = description;
		}

		public Long getId() {
			return id;
		}

		public String getDescription() {
			return description;
		}
	}

	@Entity(name = "Classroom")
	public static class Classroom {
		@Id
		private Long id;

		private String description;

		@ManyToOne(fetch = FetchType.EAGER)
		private Building building;

		@OneToMany(fetch = FetchType.EAGER)
		private List<Building> adjacentBuildings;

		public Classroom() {
		}

		public Classroom(Long id, String description, Building building, List<Building> adjacentBuildings) {
			this.id = id;
			this.description = description;
			this.building = building;
			this.adjacentBuildings = adjacentBuildings;
		}

		public Long getId() {
			return id;
		}

		public String getDescription() {
			return description;
		}

		public Building getBuilding() {
			return building;
		}

		public List<Building> getAdjacentBuildings() {
			return adjacentBuildings;
		}
	}
}