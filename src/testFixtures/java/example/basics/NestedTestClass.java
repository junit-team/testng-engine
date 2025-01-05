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

import org.testng.annotations.Test;

public class NestedTestClass {

	public static class A {
		@Test
		public void test() {
		}
	}

	public static class B {
		@Test
		public void test() {
		}
	}
}
