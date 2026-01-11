/*
 * Copyright 2021-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.configuration.parameters;

import org.testng.annotations.Test;

public class ReturnValuesTestCase {

	@Test
	public String test() {
		return "some bogus return value";
	}
}
