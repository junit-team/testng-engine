/*
 * Copyright 2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.basics;

import static org.testng.Assert.fail;

import org.testng.SkipException;
import org.testng.annotations.Test;

@Test(groups = "foo")
public class SimpleTestCase {

	@Test(groups = "bar", description = "a test that passes")
	public void successful() {
	}

	@Test
	public void aborted() {
		throw new SkipException("not today");
	}

	@Test
	public void failing() {
		fail("boom");
	}

	@Test(dependsOnMethods = "failing")
	public void skippedDueToFailingDependency() {
	}

	@Test(enabled = false)
	public void disabled() {
	}

}
