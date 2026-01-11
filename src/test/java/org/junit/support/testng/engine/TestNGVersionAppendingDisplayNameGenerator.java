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

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.List;

import org.junit.jupiter.api.DisplayNameGenerator;

class TestNGVersionAppendingDisplayNameGenerator extends DisplayNameGenerator.Standard {

	@Override
	public String generateDisplayNameForMethod(List<Class<?>> enclosingInstanceTypes, Class<?> testClass,
			Method testMethod) {
		var regularDisplayName = super.generateDisplayNameForMethod(enclosingInstanceTypes, testClass, testMethod);
		return MessageFormat.format("{0} [{1}]", regularDisplayName, TestContext.testNGVersion());
	}

}
