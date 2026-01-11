/*
 * Copyright 2021-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

/**
 * Provides the TestNG {@linkplain org.junit.platform.engine.TestEngine} implementation.
 *
 * @since 1.0
 * @provides org.junit.platform.engine.TestEngine
 * @see org.junit.support.testng.engine.TestNGTestEngine
 */
module org.junit.support.testng.engine {
    requires org.junit.platform.engine;
    requires org.testng;
    requires java.logging;
    provides org.junit.platform.engine.TestEngine with org.junit.support.testng.engine.TestNGTestEngine;
}
