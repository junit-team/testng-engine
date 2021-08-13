package example.tmp;

import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

public class ClassWithOnlyIgnoredTestsAtMethodLevel {

  @Test
  @Ignore
  public void disabledTest() {}

  @Test
  @Ignore
  public void ignoredTest() {}
}
