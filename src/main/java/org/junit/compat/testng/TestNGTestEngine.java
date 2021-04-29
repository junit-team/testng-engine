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

import static java.util.stream.Collectors.toMap;
import static org.junit.platform.engine.TestExecutionResult.failed;
import static org.junit.platform.engine.TestExecutionResult.successful;
import static org.testng.internal.RuntimeBehavior.TESTNG_MODE_DRYRUN;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.support.config.PrefixedConfigurationParameters;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.testng.TestNG;

/**
 * The TestNG {@link TestEngine}.
 *
 * @since 1.0
 */
public class TestNGTestEngine implements TestEngine {

	@Override
	public String getId() {
		return "testng";
	}

	@Override
	public TestDescriptor discover(EngineDiscoveryRequest request, UniqueId uniqueId) {
		EngineDescriptor engineDescriptor = new EngineDescriptor(uniqueId, "TestNG");
		TestNG testNG = createTestNG(request.getConfigurationParameters());
		Stream<? extends Class<?>> testClasses = request.getSelectorsByType(ClassSelector.class).stream() //
				.map(ClassSelector::getJavaClass);
		setTestClasses(testNG, testClasses);
		testNG.addListener(new DiscoveryListener(engineDescriptor));
		withTemporarySystemProperty(TESTNG_MODE_DRYRUN, "true", testNG::run);
		return engineDescriptor;
	}

	@Override
	public void execute(ExecutionRequest request) {
		EngineExecutionListener listener = request.getEngineExecutionListener();
		listener.executionStarted(request.getRootTestDescriptor());
		try {
			TestNG testNG = createTestNG(request.getConfigurationParameters());
			Map<? extends Class<?>, ClassDescriptor> descriptorsByTestClass = request.getRootTestDescriptor().getChildren().stream() //
					.map(it -> (ClassDescriptor) it) //
					.collect(
						toMap(ClassDescriptor::getTestClass, Function.identity(), (a, b) -> a, LinkedHashMap::new));
			setTestClasses(testNG, descriptorsByTestClass.keySet().stream());
			testNG.addListener(new ExecutionListener(listener, descriptorsByTestClass));
			testNG.run();
			listener.executionFinished(request.getRootTestDescriptor(), successful());
		}
		catch (Exception e) {
			listener.executionFinished(request.getRootTestDescriptor(), failed(e));
		}
	}

	private void setTestClasses(TestNG testNG, Stream<? extends Class<?>> testClasses) {
		testNG.setTestClasses(testClasses.toArray(Class[]::new));
	}

	private TestNG createTestNG(ConfigurationParameters configurationParameters) {
		TestNG testNG = new TestNG();
		testNG.setUseDefaultListeners(false);
		PrefixedConfigurationParameters prefixedConfigurationParameters = new PrefixedConfigurationParameters(
			configurationParameters, "testng.");
		prefixedConfigurationParameters.get("verbose", Integer::valueOf).ifPresent(testNG::setVerbose);
		return testNG;
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
}
