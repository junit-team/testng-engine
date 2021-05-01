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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.testng.ITestResult;

class ClassDescriptor extends AbstractTestDescriptor {

	private final ConcurrentMap<String, MethodDescriptor> methodsById = new ConcurrentHashMap<>();
	private final Class<?> testClass;

	ClassDescriptor(UniqueId uniqueId, Class<?> testClass) {
		super(uniqueId, testClass.getSimpleName(), ClassSource.from(testClass));
		this.testClass = testClass;
	}

	@Override
	public String getLegacyReportingName() {
		return testClass.getName();
	}

	Class<?> getTestClass() {
		return testClass;
	}

	@Override
	public Type getType() {
		return Type.CONTAINER;
	}

	@Override
	public void addChild(TestDescriptor child) {
		if (child instanceof MethodDescriptor) {
			MethodDescriptor methodDescriptor = (MethodDescriptor) child;
			methodsById.put(child.getUniqueId().getLastSegment().getValue(), methodDescriptor);
		}
		super.addChild(child);
	}

	public MethodDescriptor findMethodDescriptor(ITestResult result) {
		return methodsById.get(MethodDescriptor.toMethodId(result, MethodSignature.from(result.getMethod())));
	}
}
