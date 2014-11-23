package test.enhancement;

import com.avaje.ebean.enhance.ant.MainTransform;

public abstract class BaseTest {

  static String[] transformArgs = { "target/scala-2.10/sbt-0.13/test-classes", "test/model/**", "debug=9" };

  static {
//    try {
      MainTransform.main(transformArgs);
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }
  }

}
