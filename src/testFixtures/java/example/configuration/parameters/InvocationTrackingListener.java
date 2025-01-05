/*
 * Copyright 2021-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.configuration.parameters;

import org.testng.IClassListener;
import org.testng.ITestClass;

public class InvocationTrackingListener implements IClassListener {

	public static boolean invoked;

	@Override
	public void onBeforeClass(ITestClass testClass) {
		invoked = true;
	}

	@Override
	public void onAfterClass(ITestClass testClass) {
		// do nothing
	}
}
