/*
 * Copyright 2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.listeners;

import static example.listeners.SystemPropertyProvidingListener.SYSTEM_PROPERTY_KEY;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class SystemPropertyReadingTestCase {

	@Test
	public void test() {
		assertEquals(System.getProperty(SYSTEM_PROPERTY_KEY), SystemPropertyReadingTestCase.class.getName());
	}
}
