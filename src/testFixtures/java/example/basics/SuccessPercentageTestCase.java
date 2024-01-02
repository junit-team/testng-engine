/*
 * Copyright 2021-2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.basics;

import static org.testng.Assert.fail;

import org.testng.annotations.Test;

public class SuccessPercentageTestCase {

	int testRuns;

	@Test(successPercentage = 75, invocationCount = 4)
	public void test() {
		testRuns++;
		if (testRuns < 3) {
			fail("boom");
		}
	}
}
