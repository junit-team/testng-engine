/*
 * Copyright 2021-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.support.testng.engine;

import static java.util.Collections.unmodifiableSet;
import static org.junit.platform.commons.support.ClassSupport.nullSafeToString;

import java.util.Set;

import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.testng.ITestResult;
import org.testng.internal.IParameterInfo;

class MethodDescriptor extends AbstractTestDescriptor {

	static final String SEGMENT_TYPE = "method";

	final MethodSignature methodSignature;
	private final Set<TestTag> tags;
	private final Type type;

	protected MethodDescriptor(UniqueId uniqueId, String displayName, Class<?> sourceClass,
			MethodSignature methodSignature, Set<TestTag> tags, Type type) {
		super(uniqueId, displayName, toMethodSource(sourceClass, methodSignature));
		this.methodSignature = methodSignature;
		this.tags = tags;
		this.type = type;
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
		String id = methodSignature.stringRepresentation;
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
		return type;
	}

	@Override
	public boolean mayRegisterTests() {
		return type == Type.CONTAINER;
	}

	@SuppressWarnings("OptionalGetWithoutIsPresent")
	MethodSource getMethodSource() {
		return getSource().map(it -> (MethodSource) it).get();
	}
}
