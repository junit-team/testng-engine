package example.tmp;

import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

@Test
@Ignore
public class ClassWithOnlyIgnoredTestsAtClassLevel {

  public void disabledTest() {}

  public void ignoredTest() {}
}
