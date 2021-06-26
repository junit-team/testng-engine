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

import org.testng.ITestNGMethod;

class MethodSignature {

	final String methodName;
	final Class<?>[] parameterTypes;

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
	}

}
