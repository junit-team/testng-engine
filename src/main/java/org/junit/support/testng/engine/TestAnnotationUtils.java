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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.testng.ITestNGMethod;
import org.testng.annotations.Test;

/**
 * Replicates how TestNG looks up test annotations and their attributes.
 */
class TestAnnotationUtils {

	static boolean isAnnotatedInHierarchy(Class<?> clazz) {
		return findAnnotationInHierarchy(clazz).isPresent();
	}

	static boolean isAnnotatedDirectly(Method method) {
		return method.getAnnotation(Test.class) != null;
	}

	public static Class<?> getRetryAnalyzer(ITestNGMethod method) {
		return getAnnotation(method).retryAnalyzer();
	}

	static Optional<String> getDataProvider(ITestNGMethod method) {
		String dataProvider = getAnnotation(method).dataProvider().trim();
		return dataProvider.isEmpty() ? Optional.empty() : Optional.of(dataProvider);
	}

	static Optional<? extends Class<?>> getDataProviderClass(ITestNGMethod method) {
		return Stream.concat(Stream.of(getAnnotationDirectly(method)), collectTestAnnotations(method.getRealClass())) //
				.filter(Objects::nonNull) //
				.map(Test::dataProviderClass) //
				.filter(clazz -> clazz != Object.class) //
				.findFirst();
	}

	private static Test getAnnotation(ITestNGMethod method) {
		Test result = getAnnotationDirectly(method);
		if (result == null) {
			return findAnnotationInHierarchy(method.getTestClass().getRealClass()) //
					.orElseThrow(IllegalStateException::new);
		}
		return result;
	}

	private static Test getAnnotationDirectly(ITestNGMethod method) {
		return method.getConstructorOrMethod().getMethod().getAnnotation(Test.class);
	}

	private static Optional<Test> findAnnotationInHierarchy(Class<?> clazz) {
		return collectTestAnnotations(clazz).findFirst();
	}

	static Stream<String> collectGroups(Class<?> testClass) {
		return collectTestAnnotations(testClass) //
				.flatMap(annotation -> Arrays.stream(annotation.groups()));
	}

	private static Stream<Test> collectTestAnnotations(Class<?> testClass) {
		return getClassHierarchy(testClass).stream() //
				.map(clazz -> clazz.getAnnotation(Test.class)) //
				.filter(Objects::nonNull);
	}

	private static List<Class<?>> getClassHierarchy(Class<?> testClass) {
		List<Class<?>> result = new ArrayList<>();
		for (Class<?> clazz = testClass; clazz != Object.class; clazz = clazz.getSuperclass()) {
			result.add(clazz);
		}
		return result;
	}
}
