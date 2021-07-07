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

import java.util.HashSet;
import java.util.Set;

import org.junit.platform.engine.TestDescriptor;
import org.testng.ITestClass;
import org.testng.ITestResult;

class DiscoveryListener extends DefaultListener {

	private final TestClassRegistry testClassRegistry = new TestClassRegistry();
	private final TestNGEngineDescriptor engineDescriptor;

	public DiscoveryListener(TestNGEngineDescriptor engineDescriptor) {
		this.engineDescriptor = engineDescriptor;
	}

	public void finalizeDiscovery() {
		Set<ClassDescriptor> classDescriptors = new HashSet<>(engineDescriptor.getClassDescriptors());
		classDescriptors.removeAll(testClassRegistry.getClassDescriptors());
		classDescriptors.forEach(TestDescriptor::removeFromHierarchy);
	}

	@Override
	public void onBeforeClass(ITestClass testClass) {
		testClassRegistry.start(testClass.getRealClass(),
			() -> engineDescriptor.findClassDescriptor(testClass.getRealClass()));
	}

	@Override
	public void onAfterClass(ITestClass testClass) {
		testClassRegistry.finish(testClass.getRealClass(), __ -> {
			// do nothing
		});
	}

	@Override
	public void onTestStart(ITestResult result) {
		addMethodDescriptor(result);
	}

	@Override
	public void onTestSkipped(ITestResult result) {
		addMethodDescriptor(result);
	}

	private void addMethodDescriptor(ITestResult result) {
		ClassDescriptor classDescriptor = testClassRegistry.get(result.getTestClass().getRealClass()) //
				.orElseThrow(() -> new IllegalStateException("Missing class descriptor for " + result.getTestClass()));
		if (!classDescriptor.findMethodDescriptor(result).isPresent()) {
			classDescriptor.addChild(
				engineDescriptor.getTestDescriptorFactory().createMethodDescriptor(classDescriptor, result));
		}
	}

}
