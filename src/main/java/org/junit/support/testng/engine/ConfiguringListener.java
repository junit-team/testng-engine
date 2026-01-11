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

import java.util.List;

import org.junit.platform.engine.ConfigurationParameters;
import org.testng.xml.XmlSuite;

class ConfiguringListener extends DefaultListener {

	private final ConfigurationParameters configurationParameters;

	ConfiguringListener(ConfigurationParameters configurationParameters) {
		this.configurationParameters = configurationParameters;
	}

	@Override
	public void alter(List<XmlSuite> suites) {
		configurationParameters.getBoolean("testng.allowReturnValues") //
				.ifPresent(allowReturnValues -> suites.forEach(it -> it.setAllowReturnValues(allowReturnValues)));
	}
}
