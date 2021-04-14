/**
 * Provides the TestNG {@linkplain org.junit.platform.engine.TestEngine} implementation.
 *
 * @since 1.0
 * @provides org.junit.platform.engine.TestEngine
 * @see org.junit.compat.testng.TestNGTestEngine
 */
module org.junit.compat.testng.engine {
    requires org.junit.platform.engine;
    requires org.testng;
    provides org.junit.platform.engine.TestEngine with org.junit.compat.testng.TestNGTestEngine;
}
