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
import static org.junit.platform.engine.TestDescriptor.Type.CONTAINER;
import static org.junit.platform.engine.TestDescriptor.Type.TEST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestDescriptor.Type;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.Test;
import org.testng.internal.annotations.DisabledRetryAnalyzer;

class TestDescriptorFactory {

	private final Map<String, TestTag> testTags = new ConcurrentHashMap<>();

	ClassDescriptor createClassDescriptor(TestDescriptor parent, Class<?> testClass) {
		UniqueId uniqueId = parent.getUniqueId().append(ClassDescriptor.SEGMENT_TYPE, testClass.getName());
		Set<TestTag> tags = collectClassLevelTags(testClass);
		return new ClassDescriptor(uniqueId, testClass, tags);
	}

	MethodDescriptor createMethodDescriptor(ClassDescriptor parent, ITestResult result) {
		ITestNGMethod method = result.getMethod();
		MethodSignature methodSignature = MethodSignature.from(method);
		String name = methodSignature.parameterTypes.length > 0 //
				? methodSignature.stringRepresentation //
				: result.getName();
		UniqueId uniqueId = parent.getUniqueId().append(MethodDescriptor.SEGMENT_TYPE,
			toMethodId(result, methodSignature));
		Class<?> sourceClass = method.getTestClass().getRealClass();
		Set<TestTag> tags = Arrays.stream(method.getGroups()).map(this::createTag).collect(toSet());
		Type type = reportsInvocations(method) ? CONTAINER : TEST;
		return new MethodDescriptor(uniqueId, name, sourceClass, methodSignature, tags, type);
	}

	private boolean reportsInvocations(ITestNGMethod method) {
		return isDataDriven(method) //
				|| method.getInvocationCount() > 1 //
				|| getRetryAnalyzerClass(method) != getDefaultRetryAnalyzer();
	}

	private Class<?> getRetryAnalyzerClass(ITestNGMethod method) {
		try {
			return method.getRetryAnalyzerClass();
		}
		catch (NoSuchMethodError ignore) {
			Test annotation = getTestAnnotation(method);
			return annotation == null ? getDefaultRetryAnalyzer() : annotation.retryAnalyzer();
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
			Test annotation = getTestAnnotation(method);
			return annotation != null
					&& (!annotation.dataProvider().trim().isEmpty() || annotation.dataProviderClass() != Object.class);
		}
	}

	private Test getTestAnnotation(ITestNGMethod method) {
		return method.getConstructorOrMethod().getMethod().getAnnotation(Test.class);
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
		return new InvocationDescriptor(uniqueId, displayName, parent.getMethodSource(), invocationIndex);
	}

	private Set<TestTag> collectClassLevelTags(Class<?> testClass) {
		return getClassHierarchy(testClass).stream() //
				.map(clazz -> clazz.getAnnotation(Test.class)) //
				.filter(Objects::nonNull) //
				.flatMap(annotation -> Arrays.stream(annotation.groups())) //
				.map(this::createTag) //
				.collect(toSet());
	}

	private static List<Class<?>> getClassHierarchy(Class<?> testClass) {
		List<Class<?>> result = new ArrayList<>();
		for (Class<?> clazz = testClass; clazz != Object.class; clazz = clazz.getSuperclass()) {
			result.add(clazz);
		}
		return result;
	}

	private TestTag createTag(String value) {
		return testTags.computeIfAbsent(value, TestTag::create);
	}
}
