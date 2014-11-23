package com.avaje.ebean.enhance.agent;

import com.avaje.ebean.enhance.asm.*;

import java.io.PrintStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.security.ProtectionDomain;

/**
 * A Class file Transformer that enhances entity beans.
 * <p>
 * This is used as both a javaagent or via an ANT task (or other off line
 * approach).
 * </p>
 */
public class Transformer implements ClassFileTransformer {

  public static void premain(String agentArgs, Instrumentation inst) {

    Transformer t = new Transformer("", agentArgs);
    inst.addTransformer(t);

    if (t.getLogLevel() > 0) {
      System.out.println("premain loading Transformer with args:" + agentArgs);
    }
  }

  public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
    Transformer t = new Transformer("", agentArgs);
    inst.addTransformer(t);

    if (t.getLogLevel() > 0) {
      System.out.println("agentmain loading Transformer with args:" + agentArgs);
    }
  }

  private static final int CLASS_WRITER_COMPUTEFLAGS = ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS;

  private final EnhanceContext enhanceContext;

  private boolean performDetect;
  private boolean transformTransactional;
  private boolean transformEntityBeans;

  public Transformer(String extraClassPath, String agentArgs) {
    this(parseClassPaths(extraClassPath), agentArgs);
  }

  public Transformer(URL[] extraClassPath, String agentArgs) {
    this(new ClassPathClassBytesReader(extraClassPath), agentArgs);
  }

  public Transformer(ClassBytesReader r, String agentArgs) {
    this.enhanceContext = new EnhanceContext(r, agentArgs);
    this.performDetect = enhanceContext.getPropertyBoolean("detect", true);
    this.transformTransactional = enhanceContext.getPropertyBoolean("transactional", true);
    this.transformEntityBeans = enhanceContext.getPropertyBoolean("entity", true);
  }

  /**
   * Change the logout to something other than system out.
   */
  public void setLogout(PrintStream logout) {
    this.enhanceContext.setLogout(logout);
  }

  public void log(int level, String msg) {
    log(level, null, msg);
  }
  
  private void log(int level, String className, String msg) {
    enhanceContext.log(level, className, msg);
  }

  public int getLogLevel() {
    return enhanceContext.getLogLevel();
  }

  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

    try {

      // ignore JDK and JDBC classes etc
      if (enhanceContext.isIgnoreClass(className)) {
        log(9, className, "ignore class");
        return null;
      }

      ClassAdapterDetectEnhancement detect = null;

      if (performDetect) {
        log(5, className, "performing detection");
        detect = detect(loader, classfileBuffer);
      }

      if (detect == null) {
        // default only looks entity beans to enhance
        log(1, className, "no detection so enhancing entity");
        return entityEnhancement(loader, classfileBuffer);
      }

      if (transformEntityBeans && detect.isEntity()) {

        if (detect.isEnhancedEntity()) {
          detect.log(1, "already enhanced entity");

        } else {
          detect.log(2, "performing entity transform");
          return entityEnhancement(loader, classfileBuffer);
        }
      }

      if (transformTransactional && detect.isTransactional()) {
        if (detect.isEnhancedTransactional()) {
          detect.log(1, "already enhanced transactional");

        } else {
          detect.log(2, "performing transactional transform");
          return transactionalEnhancement(loader, classfileBuffer);
        }
      }
      log(9, className, "no enhancement on class");
      return null;

    } catch (NoEnhancementRequiredException e) {
      // the class is an interface
      log(8, className, "No Enhancement required " + e.getMessage());
      return null;

    } catch (Exception e) {
      // a safety net for unexpected errors
      // in the transformation
      enhanceContext.log(e);
      return null;
    }
  }

  /**
   * Perform entity bean enhancement.
   */
  private byte[] entityEnhancement(ClassLoader loader, byte[] classfileBuffer) {

    ClassReader cr = new ClassReader(classfileBuffer);
    ClassWriter cw = new CLAwareClassWriter(CLASS_WRITER_COMPUTEFLAGS, loader);
    ClassAdapterEntity ca = new ClassAdapterEntity(cw, loader, enhanceContext);
    try {

      cr.accept(ca, 0);

      if (ca.isLog(1)) {
        ca.logEnhanced();
      }

      if (enhanceContext.isReadOnly()) {
        return null;

      } else {
        return cw.toByteArray();
      }

    } catch (AlreadyEnhancedException e) {
      if (ca.isLog(1)) {
        ca.log("already enhanced entity");
      }
      return null;

    } catch (NoEnhancementRequiredException e) {
      if (ca.isLog(2)) {
        ca.log("skipping... no enhancement required");
      }
      return null;
    }
  }

  /**
   * Perform transactional enhancement.
   */
  private byte[] transactionalEnhancement(ClassLoader loader, byte[] classfileBuffer) {

    ClassReader cr = new ClassReader(classfileBuffer);
    ClassWriter cw = new CLAwareClassWriter(CLASS_WRITER_COMPUTEFLAGS, loader);
    ClassAdapterTransactional ca = new ClassAdapterTransactional(cw, loader, enhanceContext);

    try {

      cr.accept(ca, ClassReader.EXPAND_FRAMES);

      if (ca.isLog(1)) {
        ca.log("enhanced");
      }

      if (enhanceContext.isReadOnly()) {
        return null;

      } else {
        return cw.toByteArray();
      }

    } catch (AlreadyEnhancedException e) {
      if (ca.isLog(1)) {
        ca.log("already enhanced");
      }
      return null;

    } catch (NoEnhancementRequiredException e) {
      if (ca.isLog(0)) {
        ca.log("skipping... no enhancement required");
      }
      return null;
    }
  }

  /**
   * Helper method to split semi-colon separated class paths into a URL array.
   */
  public static URL[] parseClassPaths(String extraClassPath) {

    if (extraClassPath == null) {
      return new URL[0];
    }

    String[] stringPaths = extraClassPath.split(";");
    return UrlPathHelper.convertToUrl(stringPaths);
  }

  /**
   * Read the bytes quickly trying to detect if it needs entity or transactional
   * enhancement.
   */
  private ClassAdapterDetectEnhancement detect(ClassLoader classLoader, byte[] classfileBuffer) {

    ClassAdapterDetectEnhancement detect = new ClassAdapterDetectEnhancement(classLoader,
        enhanceContext);

    // skip what we can...
    ClassReader cr = new ClassReader(classfileBuffer);
    cr.accept(detect, ClassReader.SKIP_CODE + ClassReader.SKIP_DEBUG + ClassReader.SKIP_FRAMES);

    return detect;
  }
}
