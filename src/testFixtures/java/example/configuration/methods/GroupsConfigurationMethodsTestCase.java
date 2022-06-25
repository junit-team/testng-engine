/*
 * Copyright 2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.configuration.methods;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

public class GroupsConfigurationMethodsTestCase {

	public static List<String> EVENTS = new ArrayList<>();

	@BeforeGroups("group1")
	public void beforeGroup1() {
		EVENTS.add("beforeGroup1");
	}

	@AfterGroups("group1")
	public void afterGroup1() {
		EVENTS.add("afterGroup1");
	}

	@Test(groups = "group1")
	public void testGroup1() {
		EVENTS.add("testGroup1");
	}

	@Test(groups = { "group1", "group2" })
	public void testGroup1And2() {
		EVENTS.add("testGroup1And2");
	}
}
