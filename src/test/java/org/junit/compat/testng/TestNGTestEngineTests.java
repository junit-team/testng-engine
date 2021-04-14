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

import static org.junit.platform.testkit.engine.EngineTestKit.engine;

import org.junit.jupiter.api.Test;

public class TestNGTestEngineTests {

	@Test
	void test() {
		engine("testng") //
				.execute() //
				.allEvents() //
				.assertStatistics(stats -> stats.skipped(1));
	}
}
