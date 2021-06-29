/*
 * Copyright 2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.compat.testng;

import static java.util.Collections.emptyList;
import static org.junit.platform.engine.TestExecutionResult.failed;
import static org.testng.internal.RuntimeBehavior.TESTNG_MODE_DRYRUN;

import java.util.List;

import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.config.PrefixedConfigurationParameters;
import org.junit.platform.engine.support.discovery.EngineDiscoveryRequestResolver;
import org.testng.CommandLineArgs;
import org.testng.TestNG;

/**
 * The TestNG {@link TestEngine}.
 *
 * @since 1.0
 */
public class TestNGTestEngine implements TestEngine {

	private static final EngineDiscoveryRequestResolver<TestNGEngineDescriptor> DISCOVERY_REQUEST_RESOLVER = EngineDiscoveryRequestResolver.<TestNGEngineDescriptor> builder() //
			.addClassContainerSelectorResolver(new IsTestNGTestClass()) //
			.addSelectorResolver(ctx -> new TestNGSelectorResolver(ctx.getClassNameFilter())) //
			.build();

	@Override
	public String getId() {
		return "testng";
	}

	@Override
	public TestDescriptor discover(EngineDiscoveryRequest request, UniqueId uniqueId) {
		TestNGEngineDescriptor engineDescriptor = new TestNGEngineDescriptor(uniqueId);
		DISCOVERY_REQUEST_RESOLVER.resolve(request, engineDescriptor);

		Class<?>[] testClasses = engineDescriptor.getTestClasses();
		List<String> methodNames = engineDescriptor.getQualifiedMethodNames();
		DiscoveryListener listener = new DiscoveryListener(engineDescriptor);

		if (testClasses.length > 0) {
			TestNG testNG = createTestNG(Phase.DISCOVERY, request.getConfigurationParameters(), testClasses,
				emptyList());
			testNG.addListener(listener);

			withTemporarySystemProperty(TESTNG_MODE_DRYRUN, "true", testNG::run);
		}

		if (!methodNames.isEmpty()) {
			TestNG testNG = createTestNG(Phase.DISCOVERY, request.getConfigurationParameters(), null, methodNames);
			testNG.addListener(listener);

			withTemporarySystemProperty(TESTNG_MODE_DRYRUN, "true", testNG::run);
		}

		listener.finalizeDiscovery();

		return engineDescriptor;
	}

	@Override
	public void execute(ExecutionRequest request) {
		EngineExecutionListener listener = request.getEngineExecutionListener();
		TestNGEngineDescriptor engineDescriptor = (TestNGEngineDescriptor) request.getRootTestDescriptor();
		listener.executionStarted(engineDescriptor);
		try {
			engineDescriptor.prepareExecution();
			Class<?>[] testClasses = engineDescriptor.getTestClasses();
			List<String> methodNames = engineDescriptor.getQualifiedMethodNames();
			TestNG testNG = createTestNG(Phase.EXECUTION, request.getConfigurationParameters(), testClasses,
				methodNames);
			ExecutionListener executionListener = createExecutionListener(listener, engineDescriptor);
			testNG.addListener(executionListener);
			testNG.run();
			listener.executionFinished(engineDescriptor, executionListener.toEngineResult());
		}
		catch (Exception e) {
			listener.executionFinished(engineDescriptor, failed(e));
		}
	}

	private ExecutionListener createExecutionListener(EngineExecutionListener listener,
			TestNGEngineDescriptor engineDescriptor) {
		return new ExecutionListener(listener, engineDescriptor);
	}

	private ConfigurableTestNG createTestNG(Phase phase, ConfigurationParameters configurationParameters,
			Class<?>[] testClasses, List<String> methodNames) {
		ConfigurableTestNG testNG = new ConfigurableTestNG();
		if (!methodNames.isEmpty()) {
			testNG.configure(createCommandLineArgs(methodNames));
		}
		testNG.addListener(LoggingListener.INSTANCE);
		phase.configure(testNG, new PrefixedConfigurationParameters(configurationParameters, "testng."));
		testNG.setTestClasses(testClasses);
		return testNG;
	}

	private static CommandLineArgs createCommandLineArgs(List<String> methodNames) {
		CommandLineArgs commandLineArgs = new CommandLineArgs();
		commandLineArgs.useDefaultListeners = String.valueOf(false);
		commandLineArgs.commandLineMethods = methodNames;
		return commandLineArgs;
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

	enum Phase {

		DISCOVERY {
			@Override
			void configure(TestNG testNG, ConfigurationParameters config) {
				testNG.setVerbose(0);
				testNG.setUseDefaultListeners(false);
			}
		},

		EXECUTION {
			@Override
			void configure(TestNG testNG, ConfigurationParameters config) {
				testNG.setVerbose(config.get("verbose", Integer::valueOf).orElse(0));
				testNG.setUseDefaultListeners(config.getBoolean("useDefaultListeners").orElse(false));
				config.get("outputDirectory").ifPresent(testNG::setOutputDirectory);
			}
		};

		abstract void configure(TestNG testNG, ConfigurationParameters config);
	}

	private static class ConfigurableTestNG extends TestNG {
		@Override
		public void configure(CommandLineArgs cla) {
			super.configure(cla);
		}
	}
}
