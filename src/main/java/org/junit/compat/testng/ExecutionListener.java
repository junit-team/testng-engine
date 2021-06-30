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

import static org.junit.platform.engine.TestExecutionResult.aborted;
import static org.junit.platform.engine.TestExecutionResult.failed;
import static org.junit.platform.engine.TestExecutionResult.successful;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestExecutionResult;
import org.testng.ITestClass;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

class ExecutionListener extends DefaultListener {

	private final TestClassRegistry testClassRegistry = new TestClassRegistry();
	private final Map<ITestNGMethod, MethodDescriptor> inProgressTestMethods = new ConcurrentHashMap<>();

	private final Set<Throwable> engineFailures = ConcurrentHashMap.newKeySet();
	private final Map<ClassDescriptor, Set<Throwable>> classFailures = new ConcurrentHashMap<>();

	private final EngineExecutionListener delegate;
	private final TestNGEngineDescriptor engineDescriptor;

	ExecutionListener(EngineExecutionListener delegate, TestNGEngineDescriptor engineDescriptor) {
		this.delegate = delegate;
		this.engineDescriptor = engineDescriptor;
	}

	@Override
	public void onBeforeClass(ITestClass testClass) {
		ClassDescriptor classDescriptor = engineDescriptor.findClassDescriptor(testClass.getRealClass());
		if (classDescriptor != null) {
			testClassRegistry.start(testClass, () -> {
				delegate.executionStarted(classDescriptor);
				return classDescriptor;
			});
		}
	}

	@Override
	public void onConfigurationFailure(ITestResult result) {
		Optional<ClassDescriptor> classDescriptor = testClassRegistry.get(result.getTestClass());
		if (classDescriptor.isPresent()) {
			classFailures.computeIfAbsent(classDescriptor.get(), __ -> ConcurrentHashMap.newKeySet()) //
					.add(result.getThrowable());
		}
		else {
			engineFailures.add(result.getThrowable());
		}
	}

	@Override
	public void onAfterClass(ITestClass testClass) {
		testClassRegistry.finish(testClass, classDescriptor -> {
			Set<Throwable> failures = classFailures.remove(classDescriptor);
			delegate.executionFinished(classDescriptor, toTestExecutionResult(failures));
		});
	}

	@Override
	public void onTestStart(ITestResult result) {
		MethodDescriptor methodDescriptor = findOrCreateMethodDescriptor(result);
		inProgressTestMethods.put(result.getMethod(), methodDescriptor);
		delegate.executionStarted(methodDescriptor);
	}

	@Override
	public void onTestSuccess(ITestResult result) {
		MethodDescriptor methodDescriptor = inProgressTestMethods.remove(result.getMethod());
		delegate.executionFinished(methodDescriptor, successful());
	}

	@Override
	public void onTestSkipped(ITestResult result) {
		MethodDescriptor methodDescriptor = inProgressTestMethods.remove(result.getMethod());
		if (methodDescriptor == null) {
			methodDescriptor = findOrCreateMethodDescriptor(result);
			String reason = "<unknown>";
			if (result.getThrowable() != null) {
				reason = result.getThrowable().getMessage();
			}
			delegate.executionSkipped(methodDescriptor, reason);
		}
		else {
			delegate.executionFinished(methodDescriptor, aborted(result.getThrowable()));
		}
	}

	@Override
	public void onTestFailure(ITestResult result) {
		MethodDescriptor methodDescriptor = inProgressTestMethods.remove(result.getMethod());
		delegate.executionFinished(methodDescriptor, failed(result.getThrowable()));
	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
		// TODO
	}

	@Override
	public void onTestFailedWithTimeout(ITestResult result) {
		// TODO
	}

	private MethodDescriptor findOrCreateMethodDescriptor(ITestResult result) {
		ClassDescriptor classDescriptor = testClassRegistry.get(result.getTestClass()) //
				.orElseThrow(() -> new IllegalStateException("Missing class descriptor for " + result.getTestClass()));
		Optional<MethodDescriptor> methodDescriptor = classDescriptor.findMethodDescriptor(result);
		if (methodDescriptor.isPresent()) {
			return methodDescriptor.get();
		}
		MethodDescriptor dynamicMethodDescriptor = engineDescriptor.getTestDescriptorFactory().createMethodDescriptor(
			classDescriptor, result);
		classDescriptor.addChild(dynamicMethodDescriptor);
		delegate.dynamicTestRegistered(dynamicMethodDescriptor);
		return dynamicMethodDescriptor;
	}

	public TestExecutionResult toEngineResult() {
		return toTestExecutionResult(engineFailures);
	}

	private TestExecutionResult toTestExecutionResult(Set<Throwable> failures) {
		return failures == null || failures.isEmpty() ? successful() : failed(chain(failures));
	}

	private Throwable chain(Set<Throwable> failures) {
		Iterator<Throwable> iterator = failures.iterator();
		Throwable throwable = iterator.next();
		iterator.forEachRemaining(throwable::addSuppressed);
		return throwable;
	}
}
