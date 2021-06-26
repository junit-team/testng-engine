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

import static java.util.Collections.emptySet;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.testng.ITestResult;

class ClassDescriptor extends AbstractTestDescriptor {

	static final String SEGMENT_TYPE = "class";

	private final ConcurrentMap<String, MethodDescriptor> methodsById = new ConcurrentHashMap<>();
	private final Class<?> testClass;
	ExecutionStrategy executionStrategy = new IncludeMethodsExecutionStrategy();

	ClassDescriptor(UniqueId uniqueId, Class<?> testClass) {
		super(uniqueId, testClass.getSimpleName(), ClassSource.from(testClass));
		this.testClass = testClass;
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
	public void addChild(TestDescriptor child) {
		MethodDescriptor methodDescriptor = (MethodDescriptor) child;
		methodsById.put(child.getUniqueId().getLastSegment().getValue(), methodDescriptor);
		super.addChild(methodDescriptor);
	}

	public MethodDescriptor findMethodDescriptor(ITestResult result) {
		return methodsById.get(MethodDescriptor.toMethodId(result, MethodSignature.from(result.getMethod())));
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
