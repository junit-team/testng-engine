/*
 * Copyright 2021-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.support.testng.engine;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.junit.platform.engine.TestDescriptor.Type.CONTAINER;
import static org.junit.platform.engine.TestDescriptor.Type.TEST;
import static org.junit.support.testng.engine.MethodDescriptor.toMethodId;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestDescriptor.Type;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.internal.IParameterInfo;
import org.testng.internal.annotations.DisabledRetryAnalyzer;

class TestDescriptorFactory {

	// ITestResult.getFactoryInstance() and IFactoryInstance.getIndex() were added in TestNG 7.13
	// (testng-team/testng#3115) as the non-deprecated replacement for getFactoryMethodParamsInfo().
	// They are accessed reflectively because the engine compiles against an older TestNG version.
	private static final Method GET_FACTORY_INSTANCE = ReflectionSupport //
			.findMethod(ITestResult.class, "getFactoryInstance").orElse(null);
	private static final Method GET_INDEX = ReflectionSupport.tryToLoadClass("org.testng.IFactoryInstance") //
			.toOptional() //
			.flatMap(type -> ReflectionSupport.findMethod(type, "getIndex")) //
			.orElse(null);

	private final Map<String, TestTag> testTags = new ConcurrentHashMap<>();

	ClassDescriptor createClassDescriptor(TestDescriptor parent, Class<?> testClass) {
		UniqueId uniqueId = parent.getUniqueId().append(ClassDescriptor.SEGMENT_TYPE, testClass.getName());
		Set<TestTag> tags = TestAnnotationUtils.collectGroups(testClass) //
				.map(this::createTag) //
				.collect(toSet());
		return new ClassDescriptor(uniqueId, testClass, tags);
	}

	MethodDescriptor createMethodDescriptor(ClassDescriptor parent, ITestResult result) {
		ITestNGMethod method = result.getMethod();
		MethodSignature methodSignature = MethodSignature.from(method);
		StringBuilder name = new StringBuilder(methodSignature.parameterTypes.length > 0 //
				? methodSignature.stringRepresentation //
				: result.getName());
		appendInvocationIndex(name, getFactoryMethodInvocationIndex(result));
		appendParameterValues(name, getFactoryParameters(result));
		UniqueId uniqueId = parent.getUniqueId().append(MethodDescriptor.SEGMENT_TYPE,
			toMethodId(result, methodSignature));
		Class<?> sourceClass = method.getTestClass().getRealClass();
		Set<TestTag> tags = Arrays.stream(method.getGroups()).map(this::createTag).collect(toSet());
		Type type = reportsInvocations(method) ? CONTAINER : TEST;
		return new MethodDescriptor(uniqueId, name.toString(), sourceClass, methodSignature, tags, type);
	}

	private static Object[] getFactoryParameters(ITestResult result) {
		try {
			return result.getFactoryParameters();
		}
		catch (NoSuchMethodError ignore) {
			// getFactoryParameters() was added in 7.0
			return null;
		}
	}

	private static Integer getFactoryMethodInvocationIndex(ITestResult result) {
		if (usesFactoryInstanceApi()) {
			return getFactoryMethodInvocationIndexFromFactoryInstance_7_13(result);
		}
		return getFactoryMethodInvocationIndexFromParamsInfo(result);
	}

	private static Integer getFactoryMethodInvocationIndexFromFactoryInstance_7_13(ITestResult result) {
		Optional<?> factoryInstance = (Optional<?>) ReflectionSupport.invokeMethod(requireNonNull(GET_FACTORY_INSTANCE),
			result);
		return factoryInstance.map(o -> (Integer) ReflectionSupport.invokeMethod(requireNonNull(GET_INDEX), o)).orElse(
			null);
	}

	private static Integer getFactoryMethodInvocationIndexFromParamsInfo(ITestResult result) {
		try {
			IParameterInfo parameterInfo = result.getMethod().getFactoryMethodParamsInfo();
			if (parameterInfo == null) {
				return null;
			}
			// getIndex() reports the data provider row index; for a plain factory (without parameters) it always
			// returns 0, so the index is instead derived from the instance hash codes.
			return parameterInfo.getParameters().length == 0 //
					? getFactoryMethodInvocationIndex_6_14(result) //
					: Integer.valueOf(parameterInfo.getIndex());
		}
		catch (NoSuchMethodError ignore) {
			// getIndex() was introduced in 7.5
			return getFactoryMethodInvocationIndex_6_14(result);
		}
	}

	@SuppressWarnings({ "deprecation", "RedundantSuppression" }) // deprecated since 7.10.1
	private static Integer getFactoryMethodInvocationIndex_6_14(ITestResult result) {
		// ITestNGMethod.getFactoryMethodParamsInfo() was added in 7.0 and IParameterInfo.getIndex() in 7.5
		long[] instanceHashCodes = result.getTestClass().getInstanceHashCodes();
		if (instanceHashCodes.length > 1) {
			long hashCode = result.getInstance().hashCode();
			for (int i = 0; i < instanceHashCodes.length; ++i) {
				if (instanceHashCodes[i] == hashCode) {
					return i;
				}
			}
		}

		return null;
	}

	static void appendInvocationIndex(StringBuilder builder, Integer invocationIndex) {
		if (invocationIndex != null) {
			builder.append("[").append(invocationIndex).append("]");
		}
	}

	static void appendParameterValues(StringBuilder builder, Object[] parameters) {
		if (parameters != null && parameters.length > 0) {
			builder.append("(").append(Arrays.stream(parameters).map(String::valueOf).collect(joining(", "))).append(
				")");
		}
	}

	private boolean reportsInvocations(ITestNGMethod method) {
		return isDataDriven(method) //
				|| method.getInvocationCount() > 1 //
				|| method.getThreadPoolSize() > 0 //
				|| getRetryAnalyzerClass(method) != getDefaultRetryAnalyzer();
	}

	private Class<?> getRetryAnalyzerClass(ITestNGMethod method) {
		try {
			return method.getRetryAnalyzerClass();
		}
		catch (NoSuchMethodError ignore) {
			return TestAnnotationUtils.getRetryAnalyzer(method);
		}
	}

	private Class<?> getDefaultRetryAnalyzer() {
		try {
			return DisabledRetryAnalyzer.class;
		}
		catch (NoClassDefFoundError ignore) {
			return Class.class;
		}
	}

	private boolean isDataDriven(ITestNGMethod method) {
		try {
			return method.isDataDriven();
		}
		catch (NoSuchMethodError ignore) {
			return TestAnnotationUtils.getDataProvider(method).isPresent() //
					|| TestAnnotationUtils.getDataProviderClass(method).isPresent();
		}
	}

	InvocationDescriptor createInvocationDescriptor(MethodDescriptor parent, ITestResult result, int invocationIndex) {
		UniqueId uniqueId = parent.getUniqueId().append(InvocationDescriptor.SEGMENT_TYPE,
			String.valueOf(invocationIndex));
		Object[] parameters = result.getParameters();
		String displayName;
		if (parameters.length > 0) {
			String paramList = Arrays.stream(parameters).map(String::valueOf).collect(joining(", "));
			displayName = String.format("[%d] %s", invocationIndex, paramList);
		}
		else {
			displayName = String.format("[%d]", invocationIndex);
		}
		String legacyReportingName = String.format("%s[%d]", parent.getLegacyReportingName(), invocationIndex);
		return new InvocationDescriptor(uniqueId, displayName, legacyReportingName, parent.getMethodSource());
	}

	private TestTag createTag(String value) {
		return testTags.computeIfAbsent(value, TestTag::create);
	}

	private static boolean usesFactoryInstanceApi() {
		return GET_FACTORY_INSTANCE != null && GET_INDEX != null;
	}
}
