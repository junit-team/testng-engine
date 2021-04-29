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

import static org.junit.compat.testng.TestContext.testNGVersion;

import java.lang.reflect.Method;
import java.text.MessageFormat;

import org.junit.jupiter.api.DisplayNameGenerator;

public class TestNGVersionAppendingDisplayNameGenerator extends DisplayNameGenerator.Standard {

	@Override
	public String generateDisplayNameForMethod(Class<?> testClass, Method testMethod) {
		var regularDisplayName = super.generateDisplayNameForMethod(testClass, testMethod);
		return MessageFormat.format("{0} [{1}]", regularDisplayName, testNGVersion());
	}

}