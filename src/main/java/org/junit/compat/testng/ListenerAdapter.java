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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.platform.commons.support.ClassSupport;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.testng.IClass;
import org.testng.IClassListener;
import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

class ListenerAdapter implements IClassListener, ITestListener {
	private final EngineExecutionListener delegate;
	private final Map<IClass, ClassDescriptor> inProgressTestClasses = new ConcurrentHashMap<>();
	private final Map<ITestNGMethod, MethodDescriptor> inProgressTestMethods = new ConcurrentHashMap<>();
	private final Map<? extends Class<?>, ClassDescriptor> descriptorsByTestClass;

	ListenerAdapter(EngineExecutionListener delegate, Map<? extends Class<?>, ClassDescriptor> descriptorsByTestClass) {
		this.delegate = delegate;
		this.descriptorsByTestClass = descriptorsByTestClass;
	}

	@Override
	public void onStart(ITestContext context) {
	}

	@Override
	public void onFinish(ITestContext context) {
	}

	@Override
	public void onBeforeClass(ITestClass testClass) {
		ClassDescriptor classDescriptor = descriptorsByTestClass.get(testClass.getRealClass());
		if (classDescriptor != null) {
			inProgressTestClasses.put(testClass, classDescriptor);
			delegate.executionStarted(classDescriptor);
		}
	}

	@Override
	public void onAfterClass(ITestClass testClass) {
		ClassDescriptor classDescriptor = inProgressTestClasses.remove(testClass);
		if (classDescriptor != null) {
			delegate.executionFinished(classDescriptor, successful());
		}
	}

	@Override
	public void onTestStart(ITestResult result) {
		MethodDescriptor methodDescriptor = createMethodDescriptor(result);
		inProgressTestMethods.put(result.getMethod(), methodDescriptor);
		delegate.dynamicTestRegistered(methodDescriptor);
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
			methodDescriptor = createMethodDescriptor(result);
			delegate.dynamicTestRegistered(methodDescriptor);
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

	private MethodDescriptor createMethodDescriptor(ITestResult result) {
		ClassDescriptor classDescriptor = inProgressTestClasses.get(result.getTestClass());
		ITestNGMethod method = result.getMethod();
		Class<?>[] parameterTypes = getParameterTypes(method);
		MethodSource source = MethodSource.from(method.getTestClass().getRealClass().getName(), method.getMethodName(),
			parameterTypes);
		UniqueId uniqueId = classDescriptor.getUniqueId().append("method",
			String.format("%s(%s)", method.getMethodName(), ClassSupport.nullSafeToString(parameterTypes)));
		return new MethodDescriptor(uniqueId, result.getName(), source);
	}

	private Class<?>[] getParameterTypes(ITestNGMethod method) {
		try {
			return method.getParameterTypes();
		}
		catch (NoSuchMethodError e) {
			return method.getConstructorOrMethod().getParameterTypes();
		}
	}
}
