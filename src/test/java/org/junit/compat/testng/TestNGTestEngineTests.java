package org.junit.compat.testng;

import org.junit.jupiter.api.Test;

import static org.junit.platform.testkit.engine.EngineTestKit.engine;

public class TestNGTestEngineTests {

    @Test
    void test() {
        engine("testng")
                .execute()
                .allEvents()
                .assertStatistics(stats -> stats.skipped(1));
    }
}
