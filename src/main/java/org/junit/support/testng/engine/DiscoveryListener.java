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
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.testng.ITestClass;
import org.testng.ITestResult;

class DiscoveryListener extends DefaultListener {

	private final TestClassRegistry testClassRegistry = new TestClassRegistry();
	private final TestNGEngineDescriptor engineDescriptor;
	private final Predicate<String> classNameFilter;

	public DiscoveryListener(EngineDiscoveryRequest request, TestNGEngineDescriptor engineDescriptor) {
		this.classNameFilter = Filter.composeFilters(request.getFiltersByType(ClassNameFilter.class)).toPredicate();
		this.engineDescriptor = engineDescriptor;
	}

	public void finalizeDiscovery() {
		Set<ClassDescriptor> classDescriptors = new HashSet<>(engineDescriptor.getClassDescriptors());
		classDescriptors.removeAll(testClassRegistry.getClassDescriptors());
		classDescriptors.forEach(TestDescriptor::removeFromHierarchy);
	}

	@Override
	public void onBeforeClass(ITestClass testClass) {
		testClassRegistry.start(testClass.getRealClass(), realClass -> {
			ClassDescriptor classDescriptor = engineDescriptor.findClassDescriptor(realClass);
			if (classDescriptor == null && classNameFilter.test(realClass.getName())) {
				classDescriptor = engineDescriptor.getTestDescriptorFactory().createClassDescriptor(engineDescriptor,
					realClass);
				engineDescriptor.addChild(classDescriptor);
			}
			return classDescriptor;
		});
	}

	@Override
	public void onAfterClass(ITestClass testClass) {
		testClassRegistry.finish(testClass.getRealClass(), __ -> true,
			classDescriptor -> classDescriptor.remainingFinishes.incrementAndGet());
	}

	@Override
	public void onTestStart(ITestResult result) {
		addMethodDescriptor(result);
	}

	@Override
	public void onTestSkipped(ITestResult result) {
		addMethodDescriptor(result);
	}

	@Override
	public void onTestFailure(ITestResult result) {
		getClassDescriptor(result).ifPresent(classDescriptor -> addMethodDescriptor(result, classDescriptor));
	}

	private void addMethodDescriptor(ITestResult result) {
		ClassDescriptor classDescriptor = getClassDescriptor(result) //
				.orElseThrow(() -> new IllegalStateException("Missing class descriptor for " + result.getTestClass()));
		addMethodDescriptor(result, classDescriptor);
	}

	private Optional<ClassDescriptor> getClassDescriptor(ITestResult result) {
		return testClassRegistry.get(result.getTestClass().getRealClass());
	}

	private void addMethodDescriptor(ITestResult result, ClassDescriptor classDescriptor) {
		if (!classDescriptor.findMethodDescriptor(result).isPresent()) {
			classDescriptor.addChild(
				engineDescriptor.getTestDescriptorFactory().createMethodDescriptor(classDescriptor, result));
		}
	}

}
