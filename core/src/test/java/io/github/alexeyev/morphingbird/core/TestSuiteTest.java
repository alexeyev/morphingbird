package io.github.alexeyev.morphingbird.core;
import java.nio.file.*; import java.util.*;
public class TestSuiteTest {
  public static void main(String[] a) throws Exception {
    for (String repo : new String[]{"/tmp/apertium-zab","/tmp/apertium-skr"}) {
      String json = new String(Files.readAllBytes(Path.of(repo+"/test/tests.json")));
      TestSuiteModel m = TestSuiteModel.parse(json);
      System.out.println("=== " + repo.replaceAll(".*/","") + " ===");
      System.out.println("  tests: " + m.tests.size());
      for (var t : m.tests) System.out.println("    " + t.name + " -> mode=" + t.mode + ", input=" + t.inputFile);
      System.out.println("  by mode: " + m.byMode());
      System.out.println();
    }
    // Validation: zab should have 4 tests, skr 2; all should have mode+input
    String zj = new String(Files.readAllBytes(Path.of("/tmp/apertium-zab/test/tests.json")));
    String sj = new String(Files.readAllBytes(Path.of("/tmp/apertium-skr/test/tests.json")));
    TestSuiteModel zm = TestSuiteModel.parse(zj), sm = TestSuiteModel.parse(sj);
    boolean ok = zm.tests.size()==4 && sm.tests.size()==2
      && zm.tests.stream().allMatch(t->t.mode!=null && t.inputFile!=null)
      && sm.tests.stream().allMatch(t->t.mode!=null && t.inputFile!=null);
    System.out.println(ok ? "TEST SUITE MODEL OK" : "PROBLEM");
  }
}
