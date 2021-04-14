package org.junit.compat.testng;

import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

/**
 * The TestNG {@link TestEngine}.
 *
 * @since 1.0
 */
public class TestNGTestEngine implements TestEngine {

    @Override
    public String getId() {
        return "testng";
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        return new EngineDescriptor(uniqueId, "TestNG");
    }

    @Override
    public void execute(ExecutionRequest request) {
        request.getEngineExecutionListener().executionSkipped(request.getRootTestDescriptor(), "Not implemented");
    }
}
