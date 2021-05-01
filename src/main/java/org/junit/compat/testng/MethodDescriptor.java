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

import static org.junit.platform.commons.support.ClassSupport.nullSafeToString;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

class MethodDescriptor extends AbstractTestDescriptor {

	final MethodSignature methodSignature;

	protected MethodDescriptor(UniqueId uniqueId, String displayName, Class<?> sourceClass,
			MethodSignature methodSignature) {
		super(uniqueId, displayName, toMethodSource(sourceClass, methodSignature));
		this.methodSignature = methodSignature;
	}

	private static MethodSource toMethodSource(Class<?> sourceClass, MethodSignature methodSignature) {
		return MethodSource.from(sourceClass.getName(), methodSignature.methodName,
			nullSafeToString(methodSignature.parameterTypes));
	}

	static String toMethodId(ITestResult result, MethodSignature methodSignature) {
		ITestNGMethod method = result.getMethod();
		if (result.getParameters().length > 0) {
			String paramTypeList = nullSafeToString(methodSignature.parameterTypes);
			int invocationCount = method.getCurrentInvocationCount();
			return String.format("%s(%s)_%d", method.getMethodName(), paramTypeList, invocationCount);
		}
		return method.getMethodName() + "()";
	}

	@Override
	public Type getType() {
		return Type.TEST;
	}
}
