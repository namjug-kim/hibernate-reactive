/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionException;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceException;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.reactive.provider.Settings;

import org.junit.Test;

import io.vertx.ext.unit.TestContext;


public class StageExceptionsTest extends BaseReactiveTest {

	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( MyPerson.class );
	}

	@Override
	protected Configuration constructConfiguration() {
		Configuration configuration = super.constructConfiguration();
		configuration.setProperty( Settings.NATIVE_EXCEPTION_HANDLING_51_COMPLIANCE, "false" );
		return configuration;
	}

	@Test
	public void testDuplicateKeyException(TestContext context) {
		final Class<PersistenceException> expectedException = PersistenceException.class;

		test( context, openSession()
				.thenCompose( session -> session.persist( new MyPerson( "testFLush1", "unique" ) )
						.thenCompose( v -> session.flush() )
						.thenCompose( v -> session.persist( new MyPerson( "testFlush2", "unique" ) ) )
						.thenCompose( v -> session.flush() )
				)
				.handle( (res, err) -> {
					context.assertNotNull( err );
					context.assertTrue( err.getClass().isAssignableFrom( CompletionException.class ) );
					context.assertTrue(
							expectedException.isAssignableFrom( err.getCause().getClass() ),
							"Expected " + expectedException.getName() + " but was " + err
					);
					return null;
				} )
		);
	}

	@Entity
	public static class MyPerson {

		@Id
		public String name;

		@Column(unique = true)
		public String uniqueName;

		public MyPerson() {
		}

		public MyPerson(String name, String uniqueName) {
			this.name = name;
			this.uniqueName = uniqueName;
		}

		@Override
		public String toString() {
			return name + ", " + uniqueName;
		}
	}

	// I don't think we need to support this case but at the moment it would require more work to
	// disable the behaviour.
	public static class Native51ExceptionHandlingTest extends StageExceptionsTest {
		@Override
		protected Configuration constructConfiguration() {
			Configuration configuration = super.constructConfiguration();
			configuration.setProperty( AvailableSettings.NATIVE_EXCEPTION_HANDLING_51_COMPLIANCE, "true" );
			return configuration;
		}
	}
}
