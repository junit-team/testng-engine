/*
 * Copyright 2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example;

import org.testng.SkipException;
import org.testng.annotations.Test;

import static org.testng.Assert.fail;

public class SimpleTest {

    @Test
    public void successful() {
    }

    @Test(dependsOnMethods = "successful")
    public void failing() {
        fail("boom");
    }

    @Test(dependsOnMethods = "failing")
    public void aborted() {
        throw new SkipException("not today");
    }

    @Test(enabled = false)
    public void skipped() {
    }

}
