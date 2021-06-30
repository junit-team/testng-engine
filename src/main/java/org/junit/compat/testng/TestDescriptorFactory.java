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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.testng.ITestResult;
import org.testng.annotations.Test;

class TestDescriptorFactory {

	private final Map<String, TestTag> testTags = new ConcurrentHashMap<>();

	ClassDescriptor createClassDescriptor(TestDescriptor parent, Class<?> testClass) {
		UniqueId uniqueId = parent.getUniqueId().append(ClassDescriptor.SEGMENT_TYPE, testClass.getName());
		Set<TestTag> tags = collectClassLevelTags(testClass);
		return new ClassDescriptor(uniqueId, testClass, tags);
	}

	MethodDescriptor createMethodDescriptor(ClassDescriptor parent, ITestResult result) {
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
