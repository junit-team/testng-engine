/*
 * Copyright 2021-2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.configuration.parameters;

import static org.testng.Assert.assertTrue;

import org.testng.ITestContext;
import org.testng.annotations.Test;

public class PreserveOrderTestCase {

	@Test
	public void test(ITestContext context) {
		assertTrue(context.getSuite().getXmlSuite().getPreserveOrder());
		assertTrue(context.getCurrentXmlTest().getPreserveOrder());
	}
}
