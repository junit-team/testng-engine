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

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static org.junit.platform.engine.TestExecutionResult.Status.SUCCESSFUL;
import static org.junit.platform.engine.TestExecutionResult.aborted;
import static org.junit.platform.engine.TestExecutionResult.failed;
import static org.junit.platform.engine.TestExecutionResult.successful;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.testng.IInvokedMethod;
import org.testng.ITestClass;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.CustomAttribute;

class ExecutionListener extends DefaultListener {

	private final TestClassRegistry testClassRegistry = new TestClassRegistry();
	private final Map<ITestNGMethod, MethodProgress> inProgressTestMethods = new ConcurrentHashMap<>();

	private final Set<ITestResult> engineLevelFailureResults = ConcurrentHashMap.newKeySet();
	private final Map<ClassDescriptor, Set<ITestResult>> classLevelFailureResults = new ConcurrentHashMap<>();

	private final EngineExecutionListener delegate;
	private final BooleanSupplier cancellationToken;
	private final TestNGEngineDescriptor engineDescriptor;

	private volatile SkipException skipException;

	ExecutionListener(EngineExecutionListener delegate, BooleanSupplier cancellationToken,
			TestNGEngineDescriptor engineDescriptor) {
		this.delegate = delegate;
		this.cancellationToken = cancellationToken;
		this.engineDescriptor = engineDescriptor;
	}

	@Override
	public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
		if (cancellationToken.getAsBoolean()) {
			SkipException exception = skipException;
			if (exception == null) {
				exception = new SkipException("Execution cancelled");
				skipException = exception;
			}
			throw exception;
		}
	}

	@Override
	public void onBeforeClass(ITestClass testClass) {
		ClassDescriptor classDescriptor = requireNonNull(engineDescriptor.findClassDescriptor(testClass.getRealClass()),
			"Missing class descriptor");
		testClassRegistry.start(testClass.getRealClass(), __ -> {
			delegate.executionStarted(classDescriptor);
			return classDescriptor;
		});
	}

	@Override
	public void onConfigurationFailure(ITestResult result) {
		handleConfigurationResult(result);
	}

	@Override
	public void onConfigurationSkip(ITestResult result) {
		handleConfigurationResult(result);
	}

	private void handleConfigurationResult(ITestResult result) {
		Optional<ClassDescriptor> classDescriptor = testClassRegistry.get(result.getTestClass().getRealClass());
		if (classDescriptor.isPresent()) {
			classLevelFailureResults.computeIfAbsent(classDescriptor.get(), __ -> ConcurrentHashMap.newKeySet()) //
					.add(result);
		}
		else {
			engineLevelFailureResults.add(result);
		}
	}

	@Override
	public void onAfterClass(ITestClass testClass) {
		testClassRegistry.finish(testClass.getRealClass(),
			classDescriptor -> classDescriptor.remainingIterations.decrementAndGet() == 0, classDescriptor -> {
				finishMethodsNotYetReportedAsFinished(testClass);
				Set<ITestResult> results = classLevelFailureResults.remove(classDescriptor);
				delegate.executionFinished(classDescriptor, toTestExecutionResult(results));
			});
	}

	@Override
	public void onTestStart(ITestResult result) {
		MethodProgress progress = startMethodProgress(result);
		int invocationIndex = progress.invocationIndex.getAndIncrement();
		if (invocationIndex == 0) {
			reportStarted(result, progress);
		}
		if (progress.descriptor.getType().isContainer()) {
			try {
				progress.reportedAsStarted.await();
			}
			catch (InterruptedException e) {
				throw new RuntimeException("Interrupted while waiting for test method to be reported as started");
			}
			createInvocationAndReportStarted(progress, invocationIndex, result);
		}
	}

	@Override
	public void onTestSuccess(ITestResult result) {
		reportFinished(result, successful());
	}

	@Override
	public void onTestSkipped(ITestResult result) {
		MethodProgress progress = inProgressTestMethods.get(result.getMethod());
		if (progress != null || result.getThrowable() != null) {
			if (progress == null) {
				reportStarted(result, startMethodProgress(result));
			}
			reportFinished(result, aborted(result.getThrowable()));
		}
		else {
			MethodDescriptor methodDescriptor = findOrCreateMethodDescriptor(result);
			delegate.executionSkipped(methodDescriptor, "<unknown>");
		}
	}

	@Override
	public void onTestFailure(ITestResult result) {
		if (!inProgressTestMethods.containsKey(result.getMethod())) {
			reportStarted(result, startMethodProgress(result));
		}
		reportFinished(result, failed(result.getThrowable()));
	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
		onTestSuccess(result);
	}

	@Override
	public void onTestFailedWithTimeout(ITestResult result) {
		onTestFailure(result);
	}

	private MethodProgress startMethodProgress(ITestResult result) {
		MethodDescriptor methodDescriptor = findOrCreateMethodDescriptor(result);
		return inProgressTestMethods.computeIfAbsent(result.getMethod(),
			__ -> new MethodProgress(result.getMethod(), methodDescriptor));
	}

	private void finishMethodsNotYetReportedAsFinished(ITestClass testClass) {
		for (ITestNGMethod testMethod : testClass.getTestMethods()) {
			MethodProgress progress = inProgressTestMethods.remove(testMethod);
			if (progress != null) {
				delegate.executionFinished(progress.descriptor, successful());
			}
		}
	}

	private void reportStarted(ITestResult result, MethodProgress progress) {
		delegate.executionStarted(progress.descriptor);
		progress.reportedAsStarted.countDown();
		String description = result.getMethod().getDescription();
		if (description != null && !description.trim().isEmpty()) {
			delegate.reportingEntryPublished(progress.descriptor, ReportEntry.from("description", description.trim()));
		}
		Map<String, String> attributes = getAttributes(result);
		if (!attributes.isEmpty()) {
			delegate.reportingEntryPublished(progress.descriptor, ReportEntry.from(attributes));
		}
	}

	private void reportFinished(ITestResult result, TestExecutionResult executionResult) {
		MethodProgress progress = inProgressTestMethods.get(result.getMethod());
		if (progress.descriptor.getType().isContainer() && progress.invocations.containsKey(result)) {
			InvocationDescriptor invocationDescriptor = progress.invocations.remove(result);
			delegate.executionFinished(invocationDescriptor, executionResult);
		}
		else {
			inProgressTestMethods.remove(result.getMethod());
			delegate.executionFinished(progress.descriptor, executionResult);
		}
	}

	private MethodDescriptor findOrCreateMethodDescriptor(ITestResult result) {
		ClassDescriptor classDescriptor = testClassRegistry.get(result.getTestClass().getRealClass()) //
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
		progress.invocations.put(result, invocationDescriptor);
		progress.descriptor.addChild(invocationDescriptor);
		delegate.dynamicTestRegistered(invocationDescriptor);
		delegate.executionStarted(invocationDescriptor);
	}

	private TestDescriptorFactory getTestDescriptorFactory() {
		return engineDescriptor.getTestDescriptorFactory();
	}

	public TestExecutionResult toEngineResult() {
		TestExecutionResult testExecutionResult = toTestExecutionResult(engineLevelFailureResults);
		if (testExecutionResult.getStatus() == SUCCESSFUL && skipException != null) {
			return aborted(skipException);
		}
		return testExecutionResult;
	}

	private TestExecutionResult toTestExecutionResult(Set<ITestResult> results) {
		return results == null || results.isEmpty() ? successful() : abortedOrFailed(results);
	}

	private static TestExecutionResult abortedOrFailed(Set<ITestResult> results) {
		return results.stream().allMatch(it -> it.getStatus() == ITestResult.SKIP) //
				? aborted(chain(throwables(results))) //
				: failed(chain(throwables(results)));
	}

	private static Stream<Throwable> throwables(Set<ITestResult> results) {
		return results.stream().map(ITestResult::getThrowable).filter(Objects::nonNull);
	}

	private static Throwable chain(Stream<Throwable> failures) {
		Iterator<Throwable> iterator = failures.iterator();
		Throwable throwable = null;
		if (iterator.hasNext()) {
			throwable = iterator.next();
			iterator.forEachRemaining(throwable::addSuppressed);
		}
		return throwable;
	}

	static class MethodProgress {
		final ITestNGMethod method;
		final MethodDescriptor descriptor;
		final ConcurrentMap<ITestResult, InvocationDescriptor> invocations = new ConcurrentHashMap<>();
		final AtomicInteger invocationIndex = new AtomicInteger();
		final CountDownLatch reportedAsStarted = new CountDownLatch(1);

		public MethodProgress(ITestNGMethod method, MethodDescriptor descriptor) {
			this.method = method;
			this.descriptor = descriptor;
		}
	}

	private Map<String, String> getAttributes(ITestResult result) {
		try {
			CustomAttribute[] attributes = result.getMethod().getAttributes();
			if (attributes.length > 0) {
				return Arrays.stream(attributes) //
						.collect(toMap(CustomAttribute::name, attr -> String.join(", ", attr.values())));
			}
		}
		catch (NoSuchMethodError ignore) {
		}
		return emptyMap();
	}
}
