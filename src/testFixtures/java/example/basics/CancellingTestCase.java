/*
 * Copyright 2021-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.basics;

import org.junit.platform.engine.CancellationToken;
import org.testng.annotations.Test;

@Test(groups = "cancellation")
public class CancellingTestCase {

	public static CancellationToken cancellationToken;

	@Test
	public void first() {
		cancellationToken.cancel();
	}

	@Test
	public void second() {
		cancellationToken.cancel();
	}
}
