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

import org.testng.IClassListener;
import org.testng.ITestClass;

public class SystemPropertyProvidingListener implements IClassListener {

	public static final String SYSTEM_PROPERTY_KEY = "test.class";

	@Override
	public void onBeforeClass(ITestClass testClass) {
		System.setProperty(SYSTEM_PROPERTY_KEY, testClass.getName());
	}

	@Override
	public void onAfterClass(ITestClass testClass) {
		System.clearProperty(SYSTEM_PROPERTY_KEY);
	}
}
