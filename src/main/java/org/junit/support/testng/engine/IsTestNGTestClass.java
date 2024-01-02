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
import java.util.List;
import java.util.function.Predicate;

import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;

class IsTestNGTestClass implements Predicate<Class<?>> {

	@Override
	public boolean test(Class<?> candidateClass) {
		return TestAnnotationUtils.isAnnotatedInHierarchy(candidateClass)
				|| hasMethodWithTestAnnotation(candidateClass);
	}

	private boolean hasMethodWithTestAnnotation(Class<?> candidateClass) {
		List<Method> testMethods = ReflectionSupport.findMethods(candidateClass,
			TestAnnotationUtils::isAnnotatedDirectly, HierarchyTraversalMode.BOTTOM_UP);
		return !testMethods.isEmpty();
	}
}
