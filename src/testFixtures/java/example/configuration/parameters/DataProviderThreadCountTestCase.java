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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testng.Assert.assertTrue;
import static org.testng.xml.XmlSuite.DEFAULT_DATA_PROVIDER_THREAD_COUNT;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class DataProviderThreadCountTestCase {

	public static final int NUM_INVOCATIONS = DEFAULT_DATA_PROVIDER_THREAD_COUNT + 1;

	final CountDownLatch latch = new CountDownLatch(NUM_INVOCATIONS);

	@DataProvider(name = "numbers", parallel = true)
	public static Iterator<Object[]> numbers() {
		return IntStream.range(0, NUM_INVOCATIONS) //
				.mapToObj(i -> new Object[] { i }) //
				.iterator();
	}

	@Test(dataProvider = "numbers")
	public void test(Integer number) throws Exception {
		System.out.println(Thread.currentThread().getName() + ": " + number);
		latch.countDown();
		assertTrue(latch.await(1, SECONDS));
	}

}
