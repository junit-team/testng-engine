package example.tmp;

import org.testng.annotations.Test;

public class ClassWithOnlyDisabledTestsAtMethodLevel {

  @Test(enabled = false)
  public void disabledTest() {}

  @Test(enabled = false)
  public void ignoredTest() {}
}
