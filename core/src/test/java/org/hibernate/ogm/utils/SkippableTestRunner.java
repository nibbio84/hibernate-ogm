/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import org.hibernate.ogm.datastore.impl.AvailableDatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

/**
 * A JUnit 4 test runner for which allows to skip tests for single grid dialects or datastore providers by annotating
 * them with {@link SkipByGridDialect} or {@link SkipByDatastoreProvider}, respectively.
 *
 * @author Gunnar Morling
 */
public class SkippableTestRunner extends BlockJUnit4ClassRunner {

	public SkippableTestRunner(Class<?> klass) throws InitializationError {
		super( klass );
	}

	@Override
	public void run(RunNotifier notifier) {
		if ( isTestClassSkipped() || areAllTestMethodsSkipped() ) {
			skipTest( notifier );
		}
		else {
			super.run( notifier );
		}
	}

	@Override
	protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
		if ( isTestMethodSkipped( method ) ) {
			skipTest( method, notifier );
		}
		else {
			super.runChild( method, notifier );
		}
	}

	/**
	 * Skip a whole test class.
	 */
	protected void skipTest(RunNotifier notifier) {
		notifier.fireTestIgnored( Description.createSuiteDescription( super.getTestClass().getJavaClass() ) );
	}

	/**
	 * Skip a test method.
	 */
	protected void skipTest(FrameworkMethod method, RunNotifier notifier) {
		notifier.fireTestIgnored( describeChild( method ) );
	}

	/**
	 * Whether the test class is skipped by {@link SkipByDatastoreProvider}.
	 */
	protected boolean isTestClassSkipped() {
		return isTestClassSkippedByDatastoreProvider() || isTestClassSkippedByGridDialect();
	}

	private boolean isTestClassSkippedByDatastoreProvider() {
		SkipByDatastoreProvider skipByDatastoreProvider = getTestClass().getJavaClass().getAnnotation( SkipByDatastoreProvider.class );
		return skipByDatastoreProvider != null ? isSkipped( skipByDatastoreProvider ) : false;
	}

	private boolean isTestClassSkippedByGridDialect() {
		SkipByGridDialect skipByGridDialect = getTestClass().getJavaClass().getAnnotation( SkipByGridDialect.class );

		return skipByGridDialect != null ? isSkipped( skipByGridDialect ) : false;
	}


	/**
	 * Whether the given method is to be skipped or not, by means of the {@link SkipByDatastoreProvider} or {@link SkipByGridDialect} either given on the
	 * method itself or on the class of the test.
	 */
	protected boolean isTestMethodSkipped(final FrameworkMethod method) {
		if ( isTestMethodSkippedByDatastoreProvider( method ) || isTestMethodSkippedByGridDialect( method ) ) {
			return true;
		}

		return isTestClassSkipped();
	}

	private boolean isTestMethodSkippedByDatastoreProvider(FrameworkMethod method) {
		SkipByDatastoreProvider skipByDatastoreProvider = method.getAnnotation( SkipByDatastoreProvider.class );

		return skipByDatastoreProvider != null ? isSkipped( skipByDatastoreProvider ) : false;
	}

	private boolean isTestMethodSkippedByGridDialect(FrameworkMethod method) {
		SkipByGridDialect skipByGridDialect = method.getAnnotation( SkipByGridDialect.class );

		return skipByGridDialect != null ? isSkipped( skipByGridDialect ) : false;
	}

	protected boolean areAllTestMethodsSkipped() {
		for ( FrameworkMethod method : getChildren() ) {
			if ( !isTestMethodSkipped( method ) ) {
				return false;
			}
		}

		return true;
	}

	private boolean isSkipped(SkipByDatastoreProvider skipByDatastoreProvider) {
		for ( AvailableDatastoreProvider datastoreProvider : skipByDatastoreProvider.value() ) {
			if ( datastoreProvider == TestHelper.getCurrentDatastoreProvider() ) {
				return true;
			}
		}

		return false;
	}

	private boolean isSkipped(SkipByGridDialect skipByGridDialect) {
		Class<? extends GridDialect> actualGridDialectClass = TestHelper.getCurrentGridDialect();

		for ( GridDialectType gridDialectType : skipByGridDialect.value() ) {
			Class<TestableGridDialect> gridDialectClass = gridDialectType.loadGridDialectClass();
			if ( gridDialectClass == null ) {
				continue;
			}

			if ( gridDialectClass.isAssignableFrom( actualGridDialectClass ) ) {
				return true;
			}
		}
		return false;
	}

}
