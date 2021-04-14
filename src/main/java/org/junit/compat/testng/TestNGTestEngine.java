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

import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

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
		return new EngineDescriptor(uniqueId, "TestNG");
	}

	@Override
	public void execute(ExecutionRequest request) {
		request.getEngineExecutionListener().executionSkipped(request.getRootTestDescriptor(), "Not implemented");
	}
}
