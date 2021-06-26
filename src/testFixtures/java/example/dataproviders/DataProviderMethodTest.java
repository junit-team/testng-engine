/*
 * Copyright 2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.dataproviders;

import static org.testng.Assert.fail;

import org.testng.annotations.Test;

public class DataProviderMethodTest {

	@Test(dataProvider = "strings", dataProviderClass = DataProviders.class)
	public void test(String value) {
		fail(value);
	}

	@Test(dataProvider = "ints", dataProviderClass = DataProviders.class)
	public void test(int value) {
		fail(String.valueOf(value));
	}

	@Test
	public void test() {
		fail("parameterless");
	}
}
