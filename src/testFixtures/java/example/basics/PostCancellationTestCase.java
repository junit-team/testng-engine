/*
 * Copyright 2021-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.basics;

import static org.junit.Assert.fail;

import org.testng.annotations.Test;

public class PostCancellationTestCase {

	@Test(dependsOnGroups = "cancellation")
	public void test() {
		fail();
	}
}
