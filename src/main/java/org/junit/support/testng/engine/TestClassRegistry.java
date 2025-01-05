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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

class TestClassRegistry {

	private final Set<ClassDescriptor> classDescriptors = ConcurrentHashMap.newKeySet();
	private final Map<Class<?>, Entry> testClasses = new ConcurrentHashMap<>();

	void start(Class<?> testClass, Function<Class<?>, ClassDescriptor> onFirst) {
		Entry entry = testClasses.computeIfAbsent(testClass, __ -> {
			ClassDescriptor classDescriptor = onFirst.apply(testClass);
			if (classDescriptor != null) {
				classDescriptors.add(classDescriptor);
				return new Entry(classDescriptor);
			}
			return null;
		});
		if (entry != null) {
			entry.inProgress.incrementAndGet();
		}
	}

	Optional<ClassDescriptor> get(Class<?> testClass) {
		Entry entry = testClasses.get(testClass);
		return Optional.ofNullable(entry).map(it -> it.descriptor);
	}

	void finish(Class<?> testClass, Predicate<ClassDescriptor> last, Consumer<ClassDescriptor> onLast) {
		testClasses.compute(testClass, (__, value) -> {
			if (value == null) {
				return null;
			}
			int inProgress = value.inProgress.decrementAndGet();
			if (inProgress == 0 && last.test(value.descriptor)) {
				onLast.accept(value.descriptor);
				return null;
			}
			return value;
		});
	}

	Set<ClassDescriptor> getClassDescriptors() {
		return classDescriptors;
	}

	private static class Entry {

		final ClassDescriptor descriptor;
		final AtomicInteger inProgress = new AtomicInteger(0);

		Entry(ClassDescriptor descriptor) {
			this.descriptor = descriptor;
		}
	}
}
