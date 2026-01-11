/*
 * Copyright 2021-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.configuration.methods;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class FailingBeforeMethodConfigurationMethodTestCase {

	int calls;

	@BeforeMethod
	public void beforeMethod() {
		calls++;
		if (calls > 1) {
			throw new AssertionError("boom");
		}
	}

	@Test
	public void a() {
		// called
	}

	@Test(dependsOnMethods = "a")
	public void b() {
		// never called
	}
}
