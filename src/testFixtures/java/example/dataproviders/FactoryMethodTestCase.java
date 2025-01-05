/*
 * Copyright 2021-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.dataproviders;

import static org.testng.Assert.assertNotEquals;

import org.testng.annotations.Factory;
import org.testng.annotations.Test;

public class FactoryMethodTestCase {

	private final String a;
	private final String b;

	@Factory
	public static Object[] factoryData() {
		return new Object[] { new FactoryMethodTestCase("a", "b"), new FactoryMethodTestCase("c", "d") };
	}

	public FactoryMethodTestCase(String a, String b) {
		this.a = a;
		this.b = b;
	}

	@Test
	public void test() {
		assertNotEquals(a, b);
	}
}
