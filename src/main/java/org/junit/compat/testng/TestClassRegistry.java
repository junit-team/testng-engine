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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.testng.IClass;

class TestClassRegistry {

	private final Set<ClassDescriptor> classDescriptors = ConcurrentHashMap.newKeySet();
	private final Map<IClass, Entry> testClasses = new ConcurrentHashMap<>();

	void start(IClass testClass, Supplier<ClassDescriptor> onFirst) {
		Entry entry = testClasses.computeIfAbsent(testClass, __ -> {
			ClassDescriptor classDescriptor = onFirst.get();
			classDescriptors.add(classDescriptor);
			return new Entry(classDescriptor);
		});
		entry.inProgress.incrementAndGet();
	}

	ClassDescriptor get(IClass testClass) {
		return testClasses.get(testClass).descriptor;
	}

	void finish(IClass testClass, Consumer<ClassDescriptor> onLast) {
		testClasses.compute(testClass, (__, value) -> {
			if (value == null) {
				return null;
			}
			int inProgress = value.inProgress.decrementAndGet();
			if (inProgress == 0) {
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
