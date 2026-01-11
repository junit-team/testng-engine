/*
 * Copyright 2021-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.dataproviders;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.IntStream;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ParallelDataProviderTestCase {

	@DataProvider(name = "numbers", parallel = true)
	public static Iterator<Object[]> numbers() {
		return IntStream.of(1, 3, 5, 7, 9, 11, 13, -5, -3, 15, Integer.MAX_VALUE) //
				.mapToObj(Arrays::asList) //
				.map(Collection::toArray) //
				.iterator();
	}

	@Test(dataProvider = "numbers")
	public void test(Integer number) {
	}

}
