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

import org.testng.CommandLineArgs;
import org.testng.TestNG;

@SuppressWarnings({ "deprecation", "RedundantSuppression" }) // deprecated since 7.13
class TestNGCliConfigurer_6_14 {

	static TestNG configure(TestNGCliConfig config) {
		CommandLineArgs commandLineArgs = new CommandLineArgs();
		commandLineArgs.listener = config.listener;
		commandLineArgs.commandLineMethods = config.commandLineMethods;
		commandLineArgs.groups = config.groups;
		commandLineArgs.excludedGroups = config.excludedGroups;
		ConfigurableTestNG testNG = new ConfigurableTestNG();
		testNG.configure(commandLineArgs);
		return testNG;
	}

	// Needed to make {@link #configure(CommandLineArgs)} accessible.
	private static class ConfigurableTestNG extends TestNG {
		@Override
		protected void configure(CommandLineArgs cla) {
			super.configure(cla);
		}
	}
}
