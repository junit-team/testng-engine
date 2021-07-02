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

import static org.junit.platform.engine.TestExecutionResult.Status.ABORTED;
import static org.junit.platform.engine.TestExecutionResult.aborted;
import static org.junit.platform.engine.TestExecutionResult.failed;
import static org.junit.platform.engine.TestExecutionResult.successful;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.testng.ITestClass;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

class ExecutionListener extends DefaultListener {

	private final TestClassRegistry testClassRegistry = new TestClassRegistry();
	private final Map<ITestNGMethod, MethodProgress> inProgressTestMethods = new ConcurrentHashMap<>();

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
			finishMethodsNotYetReportedAsFinished(testClass);
			Set<Throwable> failures = classFailures.remove(classDescriptor);
			delegate.executionFinished(classDescriptor, toTestExecutionResult(failures));
		});
	}

	@Override
	public void onTestStart(ITestResult result) {
		MethodDescriptor methodDescriptor = findOrCreateMethodDescriptor(result);
		MethodProgress progress = inProgressTestMethods.computeIfAbsent(result.getMethod(),
			__ -> new MethodProgress(result.getMethod(), methodDescriptor));
		int invocationIndex = result.getMethod().getCurrentInvocationCount();
		if (invocationIndex == 0) {
			delegate.executionStarted(methodDescriptor);
			String description = result.getMethod().getDescription();
			if (description != null && !description.trim().isEmpty()) {
				delegate.reportingEntryPublished(methodDescriptor, ReportEntry.from("description", description.trim()));
			}
		}
		if (methodDescriptor.getType().isContainer()) {
			createInvocationAndReportStarted(progress, invocationIndex, result);
		}
	}

	@Override
	public void onTestSuccess(ITestResult result) {
		reportFinished(result, successful(), false);
	}

	@Override
	public void onTestSkipped(ITestResult result) {
		if (inProgressTestMethods.containsKey(result.getMethod())) {
			reportFinished(result, aborted(result.getThrowable()), willRetry(result));
		}
		else {
			MethodDescriptor methodDescriptor = findOrCreateMethodDescriptor(result);
			String reason = "<unknown>";
			if (result.getThrowable() != null) {
				reason = result.getThrowable().getMessage();
			}
			delegate.executionSkipped(methodDescriptor, reason);
		}
	}

	@Override
	public void onTestFailure(ITestResult result) {
		reportFinished(result, failed(result.getThrowable()), false);
	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
		onTestSuccess(result);
	}

	@Override
	public void onTestFailedWithTimeout(ITestResult result) {
		onTestFailure(result);
	}

	private void finishMethodsNotYetReportedAsFinished(ITestClass testClass) {
		for (ITestNGMethod testMethod : testClass.getTestMethods()) {
			MethodProgress progress = inProgressTestMethods.remove(testMethod);
			if (progress != null) {
				delegate.executionFinished(progress.descriptor, successful());
			}
		}
	}

	private void reportFinished(ITestResult result, TestExecutionResult executionResult, boolean willRetry) {
		MethodProgress progress = inProgressTestMethods.get(result.getMethod());
		if (progress.descriptor.getType().isContainer()) {
			int invocationIndex = result.getMethod().getCurrentInvocationCount() - 1;
			InvocationDescriptor invocationDescriptor = progress.invocations.remove(invocationIndex);
			delegate.executionFinished(invocationDescriptor, executionResult);
			boolean lastInvocation = !willRetry //
					&& (executionResult.getStatus() == ABORTED || !result.getMethod().hasMoreInvocation());
			if (lastInvocation) {
				inProgressTestMethods.remove(result.getMethod());
				delegate.executionFinished(progress.descriptor, successful());
			}
		}
		else {
			inProgressTestMethods.remove(result.getMethod());
			delegate.executionFinished(progress.descriptor, executionResult);
		}
	}

	private MethodDescriptor findOrCreateMethodDescriptor(ITestResult result) {
		ClassDescriptor classDescriptor = testClassRegistry.get(result.getTestClass()) //
				.orElseThrow(() -> new IllegalStateException("Missing class descriptor for " + result.getTestClass()));
		Optional<MethodDescriptor> methodDescriptor = classDescriptor.findMethodDescriptor(result);
		if (methodDescriptor.isPresent()) {
			return methodDescriptor.get();
		}
		MethodDescriptor dynamicMethodDescriptor = getTestDescriptorFactory() //
				.createMethodDescriptor(classDescriptor, result);
		classDescriptor.addChild(dynamicMethodDescriptor);
		delegate.dynamicTestRegistered(dynamicMethodDescriptor);
		return dynamicMethodDescriptor;
	}

	private void createInvocationAndReportStarted(MethodProgress progress, int invocationIndex, ITestResult result) {
		InvocationDescriptor invocationDescriptor = getTestDescriptorFactory().createInvocationDescriptor(
			progress.descriptor, result, invocationIndex);
		progress.invocations.put(invocationIndex, invocationDescriptor);
		progress.descriptor.addChild(invocationDescriptor);
		delegate.dynamicTestRegistered(invocationDescriptor);
		delegate.executionStarted(invocationDescriptor);
	}

	private TestDescriptorFactory getTestDescriptorFactory() {
		return engineDescriptor.getTestDescriptorFactory();
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

	static class MethodProgress {
		final ITestNGMethod method;
		final MethodDescriptor descriptor;
		final ConcurrentMap<Integer, InvocationDescriptor> invocations = new ConcurrentHashMap<>();

		public MethodProgress(ITestNGMethod method, MethodDescriptor descriptor) {
			this.method = method;
			this.descriptor = descriptor;
		}
	}

	private boolean willRetry(ITestResult result) {
		try {
			return result.wasRetried();
		}
		catch (NoSuchMethodError ignore) {
			return true;
		}
	}
}
