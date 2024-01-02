/*
 * Copyright 2021-2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.support.testng.engine;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.testng.ITestResult;

class ClassDescriptor extends AbstractTestDescriptor {

	static final String SEGMENT_TYPE = "class";

	private final ConcurrentMap<String, MethodDescriptor> methodsById = new ConcurrentHashMap<>();
	private final Class<?> testClass;
	private final Set<TestTag> tags;
	final AtomicInteger remainingIterations = new AtomicInteger();
	ExecutionStrategy executionStrategy = new IncludeMethodsExecutionStrategy();

	ClassDescriptor(UniqueId uniqueId, Class<?> testClass, Set<TestTag> tags) {
		super(uniqueId, determineDisplayName(testClass), ClassSource.from(testClass));
		this.testClass = testClass;
		this.tags = tags;
	}

	private static String determineDisplayName(Class<?> testClass) {
		String simpleName = testClass.getSimpleName();
		return simpleName.isEmpty() ? testClass.getName() : simpleName;
	}

	@Override
	public String getLegacyReportingName() {
		return testClass.getName();
	}

	Class<?> getTestClass() {
		return testClass;
	}

	@Override
	public Type getType() {
		return Type.CONTAINER;
	}

	@Override
	public Set<TestTag> getTags() {
		return unmodifiableSet(tags);
	}

	@Override
	public void addChild(TestDescriptor child) {
		methodsById.put(toChildKey(child), (MethodDescriptor) child);
		super.addChild(child);
	}

	@Override
	public void removeChild(TestDescriptor child) {
		methodsById.remove(toChildKey(child));
		super.removeChild(child);
	}

	private String toChildKey(TestDescriptor child) {
		return child.getUniqueId().getLastSegment().getValue();
	}

	public Optional<MethodDescriptor> findMethodDescriptor(ITestResult result) {
		return Optional.ofNullable(
			methodsById.get(MethodDescriptor.toMethodId(result, MethodSignature.from(result.getMethod()))));
	}

	public void includeTestMethod(String methodName) {
		executionStrategy = executionStrategy.includeMethod(methodName);
	}

	public void selectEntireClass() {
		executionStrategy = executionStrategy.selectEntireClass();
	}

	public void prepareExecution() {
		executionStrategy = new IncludeMethodsExecutionStrategy(getChildren().stream() //
				.map(child -> (MethodDescriptor) child) //
				.map(MethodDescriptor::getMethodSource) //
				.map(MethodSource::getMethodName));
	}

	interface ExecutionStrategy {
		Optional<Class<?>> getTestClass();

		Set<String> getTestMethods();

		ExecutionStrategy selectEntireClass();

		ExecutionStrategy includeMethod(String methodName);
	}

	class EntireClassExecutionStrategy implements ExecutionStrategy {

		@Override
		public Optional<Class<?>> getTestClass() {
			return Optional.of(testClass);
		}

		@Override
		public Set<String> getTestMethods() {
			return emptySet();
		}

		@Override
		public ExecutionStrategy selectEntireClass() {
			return this;
		}

		@Override
		public ExecutionStrategy includeMethod(String methodName) {
			return this;
		}
	}

	class IncludeMethodsExecutionStrategy implements ExecutionStrategy {

		private final Set<String> testMethods = new LinkedHashSet<>();

		public IncludeMethodsExecutionStrategy() {
		}

		public IncludeMethodsExecutionStrategy(Stream<String> testMethods) {
			testMethods.forEach(this.testMethods::add);
		}

		@Override
		public Optional<Class<?>> getTestClass() {
			return Optional.empty();
		}

		@Override
		public Set<String> getTestMethods() {
			return testMethods;
		}

		@Override
		public ExecutionStrategy selectEntireClass() {
			return new EntireClassExecutionStrategy();
		}

		@Override
		public ExecutionStrategy includeMethod(String methodName) {
			testMethods.add(methodName);
			return this;
		}
	}
}
