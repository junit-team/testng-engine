/*
 * Copyright 2021-2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.dataproviders;

import static java.util.Collections.singletonList;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.testng.annotations.DataProvider;

public class DataProviders {

	@DataProvider
	public static Iterator<Object[]> strings() {
		return Stream.of(singletonList("a"), singletonList("b")).map(List::toArray).iterator();
	}

	@DataProvider
	public static Iterator<Object[]> ints() {
		return Stream.of(singletonList(1), singletonList(2)).map(List::toArray).iterator();
	}

	@DataProvider
	public static Iterator<Object[]> exception() {
		throw new RuntimeException("exception in data provider");
	}

	@DataProvider
	public static Object[][] empty() {
		return new Object[0][];
	}
}
