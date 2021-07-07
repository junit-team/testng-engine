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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

class TestNGEngineDescriptor extends EngineDescriptor {

	private final TestDescriptorFactory testDescriptorFactory = new TestDescriptorFactory();
	private final Map<Class<?>, ClassDescriptor> classDescriptorsByTestClass = new HashMap<>();

	public TestNGEngineDescriptor(UniqueId uniqueId) {
		super(uniqueId, "TestNG");
	}

	public TestDescriptorFactory getTestDescriptorFactory() {
		return testDescriptorFactory;
	}

	@Override
	public void addChild(TestDescriptor child) {
		ClassDescriptor classDescriptor = (ClassDescriptor) child;
		classDescriptorsByTestClass.put(classDescriptor.getTestClass(), classDescriptor);
		super.addChild(child);
	}

	@Override
	public void removeChild(TestDescriptor child) {
		classDescriptorsByTestClass.remove(((ClassDescriptor) child).getTestClass());
		super.removeChild(child);
	}

	public ClassDescriptor findClassDescriptor(Class<?> testClass) {
		return classDescriptorsByTestClass.get(testClass);
	}

	Set<ClassDescriptor> getClassDescriptors() {
		return classDescriptors().collect(toSet());
	}

	Class<?>[] getTestClasses() {
		return classDescriptors() //
				.map(it -> it.executionStrategy.getTestClass().orElse(null)) //
				.filter(Objects::nonNull).toArray(Class[]::new);
	}

	List<String> getQualifiedMethodNames() {
		return classDescriptors() //
				.flatMap(it -> it.executionStrategy.getTestMethods().stream() //
						.map(methodName -> it.getTestClass().getName() + "." + methodName)) //
				.collect(toList());
	}

	void prepareExecution() {
		classDescriptors().forEach(ClassDescriptor::prepareExecution);
	}

	private Stream<ClassDescriptor> classDescriptors() {
		return getChildren().stream().map(child -> (ClassDescriptor) child);
	}
}
