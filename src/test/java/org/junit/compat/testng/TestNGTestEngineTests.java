/*
 * Copyright 2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.compat.testng;

import example.SimpleTest;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EventConditions;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EngineTestKit.engine;
import static org.junit.platform.testkit.engine.EventConditions.abortedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

public class TestNGTestEngineTests {

	@Test
	void executesSimpleTestClass() {
		var results = engine("testng").selectors(selectClass(SimpleTest.class)).execute();

		results.allEvents().debug().assertEventsMatchLooselyInOrder( //
				event(EventConditions.engine(), started()), //
				event(container(SimpleTest.class), started()), //
				event(test("method:successful()"), started()), //
				event(test("method:successful()"), finishedSuccessfully()), //
				event(test("method:failing()"), started()), //
				event(test("method:failing()"), finishedWithFailure(message("boom"))), //
				event(test("method:aborted()"), started()), //
				event(test("method:aborted()"), abortedWithReason()), //
				event(container(SimpleTest.class), finishedSuccessfully()), //
				event(EventConditions.engine(), finishedSuccessfully()));
	}
}
