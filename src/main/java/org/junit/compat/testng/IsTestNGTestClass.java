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

import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;

import java.util.Arrays;
import java.util.function.Predicate;

import org.testng.annotations.Test;

class IsTestNGTestClass implements Predicate<Class<?>> {

	@Override
	public boolean test(Class<?> candidateClass) {
		return isAnnotated(candidateClass, Test.class) || hasMethodWithTestAnnotation(candidateClass);
	}

	private boolean hasMethodWithTestAnnotation(Class<?> candidateClass) {
		return Arrays.stream(candidateClass.getMethods()) //
				.anyMatch(method -> isAnnotated(method, Test.class));
	}
}
