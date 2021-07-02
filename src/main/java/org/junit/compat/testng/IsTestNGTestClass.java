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

import java.util.Arrays;
import java.util.function.Predicate;

class IsTestNGTestClass implements Predicate<Class<?>> {

	@Override
	public boolean test(Class<?> candidateClass) {
		return TestAnnotationUtils.isAnnotatedInHierarchy(candidateClass)
				|| hasMethodWithTestAnnotation(candidateClass);
	}

	private boolean hasMethodWithTestAnnotation(Class<?> candidateClass) {
		return Arrays.stream(candidateClass.getMethods()).anyMatch(TestAnnotationUtils::isAnnotatedDirectly);
	}
}
