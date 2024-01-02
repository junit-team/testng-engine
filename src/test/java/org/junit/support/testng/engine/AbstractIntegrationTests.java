/*
 * Copyright 2021-2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.support.testng.engine;

import static java.util.function.Predicate.isEqual;
import static org.junit.platform.commons.util.FunctionUtils.where;
import static org.junit.platform.testkit.engine.Event.byTestDescriptor;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.displayName;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.uniqueIdSubstring;

import java.nio.file.Path;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;

abstract class AbstractIntegrationTests {

	@TempDir
	Path tempDir;

	EngineTestKit.Builder testNGEngine() {
		return EngineTestKit.engine("testng") //
				.configurationParameter("testng.verbose", "10") //
				.configurationParameter("testng.useDefaultListeners", "false") //
				.configurationParameter("testng.outputDirectory", tempDir.toString());
	}

	static Condition<Event> testClass(Class<?> testClass) {
		return container(event(displayName(testClass.getSimpleName()), uniqueIdSubstring(testClass.getName()),
			legacyReportingName(testClass.getName())));
	}

	static Condition<Event> legacyReportingName(String legacyReportingName) {
		return new Condition<>(
			byTestDescriptor(where(TestDescriptor::getLegacyReportingName, isEqual(legacyReportingName))),
			"descriptor with legacy reporting name '%s'", legacyReportingName);
	}
}
