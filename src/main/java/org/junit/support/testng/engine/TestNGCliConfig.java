/*
 * Copyright 2021-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.support.testng.engine;

import java.util.ArrayList;
import java.util.List;

public class TestNGCliConfig {
	String listener;
	List<String> commandLineMethods = new ArrayList<>();
	String groups;
	String excludedGroups;
}
