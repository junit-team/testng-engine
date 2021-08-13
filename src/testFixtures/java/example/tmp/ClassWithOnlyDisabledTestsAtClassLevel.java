package example.tmp;

import org.testng.annotations.Test;

@Test(enabled = false)
public class ClassWithOnlyDisabledTestsAtClassLevel {

  public void disabledTest() {}

  public void ignoredTest() {}
}
