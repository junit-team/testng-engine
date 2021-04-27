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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

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
	public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
		EngineDescriptor engineDescriptor = new EngineDescriptor(uniqueId, "TestNG");
		discoveryRequest.getSelectorsByType(ClassSelector.class).stream() //
				.map(ClassSelector::getJavaClass).map(
					testClass -> new ClassDescriptor(uniqueId.append("class", testClass.getName()), testClass)).forEach(
						engineDescriptor::addChild);
		return engineDescriptor;
	}

	@Override
	public void execute(ExecutionRequest request) {
		EngineExecutionListener listener = request.getEngineExecutionListener();
		listener.executionStarted(request.getRootTestDescriptor());
		try {
			TestNG testNG = new TestNG();
			Map<? extends Class<?>, ClassDescriptor> descriptorsByTestClass = request.getRootTestDescriptor().getChildren().stream() //
					.map(it -> (ClassDescriptor) it) //
					.collect(
						toMap(ClassDescriptor::getTestClass, Function.identity(), (a, b) -> a, LinkedHashMap::new));
			testNG.setTestClasses(descriptorsByTestClass.keySet().toArray(new Class<?>[0]));
			testNG.setUseDefaultListeners(false);
			PrefixedConfigurationParameters configurationParameters = new PrefixedConfigurationParameters(
				request.getConfigurationParameters(), "testng.");
			configurationParameters.get("verbose", Integer::valueOf).ifPresent(testNG::setVerbose);
			testNG.addListener(new ListenerAdapter(listener, descriptorsByTestClass));
			testNG.run();
			listener.executionFinished(request.getRootTestDescriptor(), successful());
		}
		catch (Exception e) {
			listener.executionFinished(request.getRootTestDescriptor(), failed(e));
		}
	}
}
