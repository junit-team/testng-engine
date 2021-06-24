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

import java.nio.file.Path;

import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.testkit.engine.EngineTestKit;

abstract class AbstractIntegrationTests {

	@TempDir
	Path tempDir;

	protected EngineTestKit.Builder testNGEngine() {
		return EngineTestKit.engine("testng") //
				.configurationParameter("testng.verbose", "10") //
				.configurationParameter("testng.useDefaultListeners", "false") //
				.configurationParameter("testng.outputDirectory", tempDir.toString());
	}
}
