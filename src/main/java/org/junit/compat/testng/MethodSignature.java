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

import java.util.Arrays;
import java.util.Objects;

import org.testng.ITestNGMethod;

class MethodSignature {

	final String methodName;
	final Class<?>[] parameterTypes;

	static MethodSignature from(ITestNGMethod method) {
		return new MethodSignature(method.getMethodName(), getParameterTypes(method));
	}

	private static Class<?>[] getParameterTypes(ITestNGMethod method) {
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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MethodSignature that = (MethodSignature) o;
		return methodName.equals(that.methodName) && Arrays.equals(parameterTypes, that.parameterTypes);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(methodName);
		result = 31 * result + Arrays.hashCode(parameterTypes);
		return result;
	}

}
