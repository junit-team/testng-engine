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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.platform.commons.support.ClassSupport;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.testng.IClass;
import org.testng.ITestClass;
import org.testng.ITestNGMethod;
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
		ITestNGMethod method = result.getMethod();
		Class<?> sourceClass = method.getTestClass().getRealClass();
		MethodSignature methodSignature = MethodSignature.from(method);
		UniqueId uniqueId = parent.getUniqueId().append("method", String.format("%s(%s)", method.getMethodName(),
			ClassSupport.nullSafeToString(methodSignature.parameterTypes)));
		return new MethodDescriptor(uniqueId, result.getName(), sourceClass, methodSignature);
	}

}
