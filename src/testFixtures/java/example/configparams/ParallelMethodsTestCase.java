/*
 * Copyright 2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.configparams;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testng.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;

import org.testng.annotations.Test;

public class ParallelMethodsTestCase {

	final CountDownLatch latch = new CountDownLatch(2);

	@Test
	public void a() throws Exception {
		countDownAndAwait();
	}

	@Test
	public void b() throws Exception {
		countDownAndAwait();
	}

	private void countDownAndAwait() throws InterruptedException {
		latch.countDown();
		assertTrue(latch.await(1, SECONDS));
	}
}
