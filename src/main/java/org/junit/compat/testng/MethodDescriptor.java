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

import static java.util.Collections.unmodifiableSet;
import static org.junit.platform.commons.support.ClassSupport.nullSafeToString;

import java.util.Set;

import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.internal.IParameterInfo;

class MethodDescriptor extends AbstractTestDescriptor {

	static final String SEGMENT_TYPE = "method";

	private final Set<TestTag> tags;

	protected MethodDescriptor(UniqueId uniqueId, String displayName, Class<?> sourceClass,
			MethodSignature methodSignature, Set<TestTag> tags) {
		super(uniqueId, displayName, toMethodSource(sourceClass, methodSignature));
		this.tags = tags;
	}

	@Override
	public Set<TestTag> getTags() {
		return unmodifiableSet(tags);
	}

	private static MethodSource toMethodSource(Class<?> sourceClass, MethodSignature methodSignature) {
		return MethodSource.from(sourceClass.getName(), methodSignature.methodName,
			nullSafeToString(methodSignature.parameterTypes));
	}

	static String toMethodId(ITestResult result, MethodSignature methodSignature) {
		ITestNGMethod method = result.getMethod();
		String id;
		if (result.getParameters().length > 0) {
			String paramTypeList = nullSafeToString(methodSignature.parameterTypes);
			int invocationCount = method.getCurrentInvocationCount();
			id = String.format("%s(%s)_%d", method.getMethodName(), paramTypeList, invocationCount);
		}
		else {
			id = method.getMethodName() + "()";
		}
		Object[] instances = result.getTestClass().getInstances(true);
		if (instances.length > 1) {
			Object instance = result.getInstance();
			int instanceIndex = 0;
			for (int i = 0; i < instances.length; i++) {
				if (unwrap(instances[i]) == instance) {
					instanceIndex = i;
					break;
				}
			}
			id = id + "@" + instanceIndex;
		}
		return id;
	}

	private static Object unwrap(Object instance) {
		try {
			return IParameterInfo.embeddedInstance(instance);
		}
		catch (NoClassDefFoundError ignored) {
			return instance;
		}
	}

	@Override
	public Type getType() {
		return Type.TEST;
	}

	@SuppressWarnings("OptionalGetWithoutIsPresent")
	MethodSource getMethodSource() {
		return getSource().map(it -> (MethodSource) it).get();
	}
}
