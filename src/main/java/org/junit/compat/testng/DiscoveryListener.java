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

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.junit.compat.testng.MethodDescriptor.toMethodId;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.testng.ITestClass;
import org.testng.ITestResult;

class DiscoveryListener extends DefaultListener {

	private final Map<String, TestTag> testTags = new ConcurrentHashMap<>();
	private final TestClassRegistry testClassRegistry = new TestClassRegistry();
	private final TestNGEngineDescriptor engineDescriptor;

	public DiscoveryListener(TestNGEngineDescriptor engineDescriptor) {
		this.engineDescriptor = engineDescriptor;
	}

	public void finalizeDiscovery() {
		Set<ClassDescriptor> classDescriptors = new HashSet<>(engineDescriptor.getClassDescriptors());
		classDescriptors.removeAll(testClassRegistry.getClassDescriptors());
		classDescriptors.forEach(TestDescriptor::removeFromHierarchy);
		engineDescriptor.finalizeDiscovery();
	}

	@Override
	public void onBeforeClass(ITestClass testClass) {
		testClassRegistry.start(testClass, () -> engineDescriptor.findClassDescriptor(testClass.getRealClass()));
	}

	@Override
	public void onAfterClass(ITestClass testClass) {
		testClassRegistry.finish(testClass, __ -> {
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
		ClassDescriptor classDescriptor = testClassRegistry.get(result.getTestClass());
		classDescriptor.addChild(createMethodDescriptor(classDescriptor, result));
	}

	private MethodDescriptor createMethodDescriptor(ClassDescriptor parent, ITestResult result) {
		MethodSignature methodSignature = MethodSignature.from(result.getMethod());
		String name = result.getName();
		if (result.getParameters().length > 0) {
			int invocationCount = result.getMethod().getCurrentInvocationCount();
			String paramList = Arrays.stream(result.getParameters()).map(String::valueOf).collect(joining(", "));
			name = String.format("%s[%d](%s)", name, invocationCount, paramList);
		}
		UniqueId uniqueId = parent.getUniqueId().append(MethodDescriptor.SEGMENT_TYPE,
			toMethodId(result, methodSignature));
		Class<?> sourceClass = result.getMethod().getTestClass().getRealClass();
		Set<TestTag> tags = Arrays.stream(result.getMethod().getGroups()).map(this::createTag).collect(toSet());
		return new MethodDescriptor(uniqueId, name, sourceClass, methodSignature, tags);
	}

	private TestTag createTag(String value) {
		return testTags.computeIfAbsent(value, TestTag::create);
	}

}
