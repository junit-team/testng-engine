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

import org.testng.annotations.Test;

public class ParallelExecutionTestCase {

	@Test(threadPoolSize = 10, invocationCount = 10)
	public void test() throws InterruptedException {
		Thread.sleep(100);
	}
}
