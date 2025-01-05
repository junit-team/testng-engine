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

public class TimeoutTestCase {

	@Test(timeOut = 1)
	public void timeOut() throws Exception {
		Thread.sleep(1_000);
	}

	@Test(invocationTimeOut = 1)
	public void invocationTimeOut() throws Exception {
		Thread.sleep(1_000);
	}
}
