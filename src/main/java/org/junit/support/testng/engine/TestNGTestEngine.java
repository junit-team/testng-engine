/*
 * Copyright 2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.support.testng.engine;

import static org.junit.support.testng.engine.TestNGTestEngine.Configurer.testClasses;
import static org.junit.support.testng.engine.TestNGTestEngine.Configurer.testMethods;
import static org.testng.internal.RuntimeBehavior.TESTNG_MODE_DRYRUN;

import java.util.List;

import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.discovery.EngineDiscoveryRequestResolver;
import org.testng.CommandLineArgs;
import org.testng.ITestNGListener;
import org.testng.TestNG;
import org.testng.annotations.DataProvider;
import org.testng.xml.XmlSuite.ParallelMode;

/**
 * The TestNG {@link TestEngine} for running TestNG tests on the JUnit Platform.
 *
 * @since 1.0
 */
public class TestNGTestEngine implements TestEngine {

	private static final EngineDiscoveryRequestResolver<TestNGEngineDescriptor> DISCOVERY_REQUEST_RESOLVER = EngineDiscoveryRequestResolver.<TestNGEngineDescriptor> builder() //
			.addClassContainerSelectorResolver(new IsTestNGTestClass()) //
			.addSelectorResolver(ctx -> new TestNGSelectorResolver(ctx.getClassNameFilter(),
				ctx.getEngineDescriptor().getTestDescriptorFactory())) //
			.build();

	@Override
	public String getId() {
		return "testng";
	}

	/**
	 * Discover TestNG tests based on the supplied {@linkplain EngineDiscoveryRequest request}.
	 * <p>
	 * Supports the following {@linkplain org.junit.platform.engine.DiscoverySelector selectors}:
	 * <ul>
	 *     <li>{@link org.junit.platform.engine.discovery.ClasspathRootSelector}</li>
	 *     <li>{@link org.junit.platform.engine.discovery.ClassSelector}</li>
	 *     <li>{@link org.junit.platform.engine.discovery.MethodSelector}</li>
	 *     <li>{@link org.junit.platform.engine.discovery.ModuleSelector}</li>
	 *     <li>{@link org.junit.platform.engine.discovery.PackageSelector}</li>
	 *     <li>{@link org.junit.platform.engine.discovery.UniqueIdSelector}</li>
	 * </ul>
	 * <p>
	 * Custom test suites specified via {@code testng.xml} files are not supported.
	 * <p>
	 * Supports the following {@linkplain org.junit.platform.engine.Filter filters}:
	 * <ul>
	 *     <li>{@link org.junit.platform.engine.discovery.ClassNameFilter}</li>
	 *     <li>{@link org.junit.platform.engine.discovery.PackageNameFilter}</li>
	 *     <li>Any post-discovery filter, e.g. for included/excluded tags</li>
	 * </ul>
	 * <p>
	 * The implementation collects a list of potential test classes and method names and uses
	 * TestNG's dry-run mode to determine which of them are TestNG test classes and methods. Since
	 * TestNG can only run either classes or methods, it will be executed twice in the edge case
	 * that class and method selectors are part of the discovery request.
	 */
	@Override
	public TestDescriptor discover(EngineDiscoveryRequest request, UniqueId uniqueId) {
		TestNGEngineDescriptor engineDescriptor = new TestNGEngineDescriptor(uniqueId);

		DISCOVERY_REQUEST_RESOLVER.resolve(request, engineDescriptor);
		Class<?>[] testClasses = engineDescriptor.getTestClasses();
		List<String> methodNames = engineDescriptor.getQualifiedMethodNames();

		ConfigurationParameters configurationParameters = request.getConfigurationParameters();
		DiscoveryListener listener = new DiscoveryListener(request, engineDescriptor);

		if (testClasses.length > 0) {
			withTemporarySystemProperty(TESTNG_MODE_DRYRUN, "true",
				() -> configureAndRun(configurationParameters, listener, testClasses(testClasses), Phase.DISCOVERY));
		}

		if (!methodNames.isEmpty()) {
			withTemporarySystemProperty(TESTNG_MODE_DRYRUN, "true",
				() -> configureAndRun(configurationParameters, listener, testMethods(methodNames), Phase.DISCOVERY));
		}

		listener.finalizeDiscovery();

		return engineDescriptor;
	}

	/**
	 * Execute the previously discovered TestNG tests in the supplied {@linkplain ExecutionRequest request}.
	 * <p>
	 * Supports the following configuration parameters:
	 * <h4>Execution</h4>
	 * <dl>
	 *     <dt>{@code testng.allowReturnValues} (file path)</dt>
	 *     <dd>whether methods annotated with {@code @Test} that have return values should be considered test methods (default: {@code false})</dd>
	 *
	 *     <dt>{@code testng.dataProviderThreadCount} (file path)</dt>
	 *     <dd>maximum number of threads to use for running data providers in parallel, if enabled via {@link DataProvider#parallel()} (default: {@code 10})</dd>
	 *
	 *     <dt>{@code testng.parallel} (methods|tests|classes|instances|none)</dt>
	 *     <dd>TestNG's parallel execution mode for running tests in separate threads (default: {@code "none"})</dd>
	 *
	 *     <dt>{@code testng.preserveOrder} (boolean)</dt>
	 *     <dd>whether classes and methods should be run in a predictable order (default: {@code true})</dd>
	 *
	 *     <dt>{@code testng.threadCount} (boolean)</dt>
	 *     <dd>maximum number of threads for running tests in parallel, if enabled via {@code testng.parallel} (default: {@code 5})</dd>
	 * </dl>
	 * <h4>Reporting</h4>
	 * <dl>
	 *     <dt>{@code testng.listeners} (comma-separated list of fully-qualified class names)</dt>
	 *     <dd>custom listeners that should be registered when executing tests (default: {@code ""})</dd>
	 *
	 *     <dt>{@code testng.outputDirectory} (file path)</dt>
	 *     <dd>the output directory for reports (default: {@code "test-output"})</dd>
	 *
	 *     <dt>{@code testng.useDefaultListeners} (boolean)</dt>
	 *     <dd>whether TestNG's default report generating listeners should be used (default: {@code false})</dd>
	 *
	 *     <dt>{@code testng.verbose} (integer)</dt>
	 *     <dd>TestNG's level of verbosity (default: {@code 0})</dd>
	 * </dl>
	 * <p>
	 * The implementation configures TestNG as if the discovered methods were specified on the
	 * command line.
	 * <p>
	 * Data providers test methods are reported as a nested structure, i.e. individual invocations
	 * are reported underneath the test methods along with their parameters:
	 * <pre><code>
	 * └─ TestNG ✔
	 *    └─ DataProviderMethodTestCase ✔
	 *       └─ test(java.lang.String) ✔
	 *          ├─ [0] a ✔
	 *          └─ [1] b ✔
	 * </code></pre>
	 */
	@Override
	public void execute(ExecutionRequest request) {
		EngineExecutionListener listener = request.getEngineExecutionListener();
		TestNGEngineDescriptor engineDescriptor = (TestNGEngineDescriptor) request.getRootTestDescriptor();
		listener.executionStarted(engineDescriptor);
		engineDescriptor.prepareExecution();
		ExecutionListener executionListener = new ExecutionListener(listener, engineDescriptor);
		List<String> methodNames = engineDescriptor.getQualifiedMethodNames();
		if (!methodNames.isEmpty()) {
			configureAndRun(request.getConfigurationParameters(), executionListener, testMethods(methodNames),
				Phase.EXECUTION);
		}
		listener.executionFinished(engineDescriptor, executionListener.toEngineResult());
	}

	private static void configureAndRun(ConfigurationParameters configurationParameters, ITestNGListener listener,
			Configurer... configurers) {
		CommandLineArgs commandLineArgs = new CommandLineArgs();
		for (Configurer configurer : configurers) {
			configurer.configure(commandLineArgs, configurationParameters);
		}
		ConfigurableTestNG testNG = new ConfigurableTestNG();
		testNG.configure(commandLineArgs);
		for (Configurer configurer : configurers) {
			configurer.configure(testNG, configurationParameters);
		}
		testNG.addListener(LoggingListener.INSTANCE);
		testNG.addListener(new ConfiguringListener(configurationParameters));
		testNG.addListener(listener);
		testNG.run();
	}

	@SuppressWarnings("SameParameterValue")
	private static void withTemporarySystemProperty(String key, String value, Runnable action) {
		String originalValue = System.getProperty(key);
		System.setProperty(key, value);
		try {
			action.run();
		}
		finally {
			if (originalValue == null) {
				System.getProperties().remove(key);
			}
			else {
				System.setProperty(key, originalValue);
			}
		}
	}

	interface Configurer {

		static Configurer testClasses(Class<?>[] testClasses) {
			return new Configurer() {
				@Override
				public void configure(TestNG testNG, ConfigurationParameters config) {
					testNG.setTestClasses(testClasses);
				}
			};
		}

		static Configurer testMethods(List<String> methodNames) {
			return new Configurer() {
				@Override
				public void configure(CommandLineArgs commandLineArgs, ConfigurationParameters config) {
					commandLineArgs.commandLineMethods = methodNames;
				}
			};
		}

		default void configure(TestNG testNG, ConfigurationParameters config) {
		}

		default void configure(CommandLineArgs commandLineArgs, ConfigurationParameters config) {
		}

	}

	enum Phase implements Configurer {

		DISCOVERY {
			@Override
			public void configure(TestNG testNG, ConfigurationParameters config) {
				testNG.setVerbose(0);
				testNG.setUseDefaultListeners(false);
			}
		},

		EXECUTION {
			@Override
			public void configure(TestNG testNG, ConfigurationParameters config) {
				testNG.setVerbose(config.get("testng.verbose", Integer::valueOf).orElse(0));
				testNG.setUseDefaultListeners(config.getBoolean("testng.useDefaultListeners").orElse(false));
				config.get("testng.outputDirectory") //
						.ifPresent(testNG::setOutputDirectory);
				config.getBoolean("testng.preserveOrder") //
						.ifPresent(testNG::setPreserveOrder);
				config.get("testng.parallel", ParallelMode::getValidParallel) //
						.ifPresent(testNG::setParallel);
				config.get("testng.threadCount", Integer::parseInt) //
						.ifPresent(testNG::setThreadCount);
				config.get("testng.dataProviderThreadCount", Integer::parseInt) //
						.ifPresent(testNG::setDataProviderThreadCount);
			}

			@Override
			public void configure(CommandLineArgs commandLineArgs, ConfigurationParameters config) {
				config.get("testng.listeners") //
						.ifPresent(listeners -> commandLineArgs.listener = listeners);
			}
		};
	}

	/**
	 * Needed to make {@link #configure(CommandLineArgs)} accessible.
	 */
	private static class ConfigurableTestNG extends TestNG {
		@Override
		protected void configure(CommandLineArgs cla) {
			super.configure(cla);
		}
	}
}
