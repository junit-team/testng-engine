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

import org.junit.platform.commons.support.ReflectionSupport;
import org.testng.TestNG;

class TestNGCliConfigurer_7_13 {

	static TestNG configure(TestNGCliConfig config) throws Exception {
		Class<?> cliOptionsClass = ReflectionSupport.tryToLoadClass("org.testng.cli.CliOptions") //
				.toOptional().get();

		Object cliOptions = ReflectionSupport.newInstance(cliOptionsClass);
		setField(cliOptions, "listener", config.listener);
		setField(cliOptions, "commandLineMethods", config.commandLineMethods);
		setField(cliOptions, "groups", config.groups);
		setField(cliOptions, "excludedGroups", config.excludedGroups);

		Class<?> cliConfigurerClass = ReflectionSupport.tryToLoadClass("org.testng.cli.CliConfigurer") //
				.toOptional().get();
		Method configure = ReflectionSupport.findMethod(cliConfigurerClass, "configure", TestNG.class,
			cliOptionsClass).get();
		TestNG testng = new TestNG();
		ReflectionSupport.invokeMethod(configure, null, testng, cliOptions);
		return testng;
	}

	private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
		target.getClass().getField(name).set(target, value);
	}
}
