/*
 * Copyright 2021-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.support.testng.engine;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.junit.platform.engine.TestDescriptor.Type.CONTAINER;
import static org.junit.platform.engine.TestDescriptor.Type.TEST;
import static org.junit.support.testng.engine.MethodDescriptor.toMethodId;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestDescriptor.Type;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.internal.annotations.DisabledRetryAnalyzer;

class TestDescriptorFactory {

	private final Map<String, TestTag> testTags = new ConcurrentHashMap<>();

	ClassDescriptor createClassDescriptor(TestDescriptor parent, Class<?> testClass) {
		UniqueId uniqueId = parent.getUniqueId().append(ClassDescriptor.SEGMENT_TYPE, testClass.getName());
		Set<TestTag> tags = TestAnnotationUtils.collectGroups(testClass) //
				.map(this::createTag) //
				.collect(toSet());
		return new ClassDescriptor(uniqueId, testClass, tags);
	}

	MethodDescriptor createMethodDescriptor(ClassDescriptor parent, ITestResult result) {
		ITestNGMethod method = result.getMethod();
		MethodSignature methodSignature = MethodSignature.from(method);
		StringBuilder name = new StringBuilder(methodSignature.parameterTypes.length > 0 //
				? methodSignature.stringRepresentation //
				: result.getName());
		appendInvocationIndex(name, getFactoryMethodInvocationIndex(result));
		appendParameterValues(name, getFactoryParameters(result));
		UniqueId uniqueId = parent.getUniqueId().append(MethodDescriptor.SEGMENT_TYPE,
			toMethodId(result, methodSignature));
		Class<?> sourceClass = method.getTestClass().getRealClass();
		Set<TestTag> tags = Arrays.stream(method.getGroups()).map(this::createTag).collect(toSet());
		Type type = reportsInvocations(method) ? CONTAINER : TEST;
		return new MethodDescriptor(uniqueId, name.toString(), sourceClass, methodSignature, tags, type);
	}

	private static Object[] getFactoryParameters(ITestResult result) {
		try {
			return result.getFactoryParameters();
		}
		catch (NoSuchMethodError ignore) {
			// getFactoryParameters() was added in 7.0
			return null;
		}
	}

	private static Integer getFactoryMethodInvocationIndex(ITestResult result) {
		long[] instanceHashCodes = result.getTestClass().getInstanceHashCodes();
		if (instanceHashCodes.length > 1) {
			long hashCode = result.getInstance().hashCode();
			for (int i = 0; i < instanceHashCodes.length; ++i) {
				if (instanceHashCodes[i] == hashCode) {
					return i;
				}
			}
		}

		return null;
	}

	static void appendInvocationIndex(StringBuilder builder, Integer invocationIndex) {
		if (invocationIndex != null) {
			builder.append("[").append(invocationIndex).append("]");
		}
	}

	static void appendParameterValues(StringBuilder builder, Object[] parameters) {
		if (parameters != null && parameters.length > 0) {
			builder.append("(").append(Arrays.stream(parameters).map(String::valueOf).collect(joining(", "))).append(
				")");
		}
	}

	private boolean reportsInvocations(ITestNGMethod method) {
		return isDataDriven(method) //
				|| method.getInvocationCount() > 1 //
				|| method.getThreadPoolSize() > 0 //
				|| getRetryAnalyzerClass(method) != getDefaultRetryAnalyzer();
	}

	private Class<?> getRetryAnalyzerClass(ITestNGMethod method) {
		try {
			return method.getRetryAnalyzerClass();
		}
		catch (NoSuchMethodError ignore) {
			return TestAnnotationUtils.getRetryAnalyzer(method);
		}
	}

	private Class<?> getDefaultRetryAnalyzer() {
		try {
			return DisabledRetryAnalyzer.class;
		}
		catch (NoClassDefFoundError ignore) {
			return Class.class;
		}
	}

	private boolean isDataDriven(ITestNGMethod method) {
		try {
			return method.isDataDriven();
		}
		catch (NoSuchMethodError ignore) {
			return TestAnnotationUtils.getDataProvider(method).isPresent() //
					|| TestAnnotationUtils.getDataProviderClass(method).isPresent();
		}
	}

	InvocationDescriptor createInvocationDescriptor(MethodDescriptor parent, ITestResult result, int invocationIndex) {
		UniqueId uniqueId = parent.getUniqueId().append(InvocationDescriptor.SEGMENT_TYPE,
			String.valueOf(invocationIndex));
		Object[] parameters = result.getParameters();
		String displayName;
		if (parameters.length > 0) {
			String paramList = Arrays.stream(parameters).map(String::valueOf).collect(joining(", "));
			displayName = String.format("[%d] %s", invocationIndex, paramList);
		}
		else {
			displayName = String.format("[%d]", invocationIndex);
		}
		String legacyReportingName = String.format("%s[%d]", parent.getLegacyReportingName(), invocationIndex);
		return new InvocationDescriptor(uniqueId, displayName, legacyReportingName, parent.getMethodSource());
	}

	private TestTag createTag(String value) {
		return testTags.computeIfAbsent(value, TestTag::create);
	}
}
