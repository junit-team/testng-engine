package example.tmp;

import org.testng.IClassListener;
import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestListener;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ExampleListener implements IClassListener, ITestListener {

  private List<String> methods;

  public List<String> getMethods() {
    return methods;
  }

  @Override
  public void onBeforeClass(ITestClass testClass) {
    methods = Arrays.stream(testClass.getTestMethods())
        .map(each -> each.getConstructorOrMethod().getMethod().getName())
        .collect(Collectors.toList());
  }

  @Override
  public void onFinish(ITestContext context) {
    List<String> excluded = context.getExcludedMethods().stream()
        .map(each -> each.getConstructorOrMethod().getMethod().getName())
        .collect(Collectors.toList());
    if (methods == null || methods.isEmpty()) {
      methods = excluded;
    } else {
      methods.addAll(excluded);
    }
  }
}
