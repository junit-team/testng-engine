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

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.testng.annotations.DataProvider;

public class DataProviders {

	@DataProvider
	public static Iterator<Object[]> strings() {
		return Stream.of(List.of("a"), List.of("b")).map(List::toArray).iterator();
	}

	@DataProvider
	public static Iterator<Object[]> ints() {
		return Stream.of(List.of(1), List.of(2)).map(List::toArray).iterator();
	}
}
