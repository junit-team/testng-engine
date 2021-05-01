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
import static org.junit.compat.testng.MethodDescriptor.toMethodId;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.testng.IClass;
import org.testng.ITestClass;
import org.testng.ITestResult;

class DiscoveryListener extends DefaultListener {

	private final EngineDescriptor engineDescriptor;
	private final Map<IClass, ClassDescriptor> classDescriptors = new ConcurrentHashMap<>();

	public DiscoveryListener(EngineDescriptor engineDescriptor) {
		this.engineDescriptor = engineDescriptor;
	}

	@Override
	public void onBeforeClass(ITestClass testClass) {
		classDescriptors.computeIfAbsent(testClass, __ -> {
			ClassDescriptor classDescriptor = createClassDescriptor(testClass);
			engineDescriptor.addChild(classDescriptor);
			return classDescriptor;
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
		ClassDescriptor classDescriptor = classDescriptors.get(result.getTestClass());
		classDescriptor.addChild(createMethodDescriptor(classDescriptor, result));
	}

	private ClassDescriptor createClassDescriptor(ITestClass testClass) {
		return new ClassDescriptor(engineDescriptor.getUniqueId().append("class", testClass.getRealClass().getName()),
			testClass.getRealClass());
	}

	private MethodDescriptor createMethodDescriptor(ClassDescriptor parent, ITestResult result) {
		MethodSignature methodSignature = MethodSignature.from(result.getMethod());
		String name = result.getName();
		if (result.getParameters().length > 0) {
			int invocationCount = result.getMethod().getCurrentInvocationCount();
			String paramList = Arrays.stream(result.getParameters()).map(String::valueOf).collect(joining(", "));
			name = String.format("%s[%d](%s)", name, invocationCount, paramList);
		}
		UniqueId uniqueId = parent.getUniqueId().append("method", toMethodId(result, methodSignature));
		Class<?> sourceClass = result.getMethod().getTestClass().getRealClass();
		return new MethodDescriptor(uniqueId, name, sourceClass, methodSignature);
	}

}
