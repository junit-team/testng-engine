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

import static org.junit.platform.commons.support.ClassSupport.nullSafeToString;

import org.testng.ITestNGMethod;

class MethodSignature {

	final String methodName;
	final Class<?>[] parameterTypes;
	final String stringRepresentation;

	static MethodSignature from(ITestNGMethod method) {
		return new MethodSignature(method.getMethodName(), getParameterTypes(method));
	}

	public static Class<?>[] getParameterTypes(ITestNGMethod method) {
		try {
			return method.getParameterTypes();
		}
		catch (NoSuchMethodError e) {
			return method.getConstructorOrMethod().getParameterTypes();
		}
	}

	private MethodSignature(String methodName, Class<?>[] parameterTypes) {
		this.methodName = methodName;
		this.parameterTypes = parameterTypes;
		this.stringRepresentation = String.format("%s(%s)", methodName, nullSafeToString(parameterTypes));
	}

	@Override
	public String toString() {
		return stringRepresentation;
	}
}
