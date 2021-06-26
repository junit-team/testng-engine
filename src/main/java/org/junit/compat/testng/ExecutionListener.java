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

import static java.util.Collections.synchronizedSet;
import static org.junit.platform.engine.TestExecutionResult.aborted;
import static org.junit.platform.engine.TestExecutionResult.failed;
import static org.junit.platform.engine.TestExecutionResult.successful;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
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
		ClassDescriptor classDescriptor = testClassRegistry.get(result.getTestClass());
		classFailures.computeIfAbsent(classDescriptor, __ -> synchronizedSet(new LinkedHashSet<>())) //
				.add(result.getThrowable());
	}

	@Override
	public void onConfigurationFailure(ITestResult result, ITestNGMethod method) {
		onConfigurationFailure(result);
	}

	@Override
	public void onAfterClass(ITestClass testClass) {
		testClassRegistry.finish(testClass, classDescriptor -> {
			Set<Throwable> failures = classFailures.remove(classDescriptor);
			TestExecutionResult result;
			if (failures == null) {
				result = successful();
			}
			else {
				Iterator<Throwable> iterator = failures.iterator();
				Throwable throwable = iterator.next();
				iterator.forEachRemaining(throwable::addSuppressed);
				result = TestExecutionResult.failed(throwable);
			}
			delegate.executionFinished(classDescriptor, result);
		});
	}

	@Override
	public void onTestStart(ITestResult result) {
		MethodDescriptor methodDescriptor = findMethodDescriptor(result);
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
			methodDescriptor = findMethodDescriptor(result);
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

	private MethodDescriptor findMethodDescriptor(ITestResult result) {
		ClassDescriptor classDescriptor = testClassRegistry.get(result.getTestClass());
		return classDescriptor.findMethodDescriptor(result);
	}
}
