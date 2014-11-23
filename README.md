ebean-jdk8-fix
====================

This is a temporary fix until Play Framework updates its included ebean. Including JDK 8 specific features in a Play Framework project will cause the following compilation error:

	ava.lang.ArrayIndexOutOfBoundsException: 2925
		at com.avaje.ebean.enhance.asm.ClassReader.readClass(ClassReader.java:1976)

This project combines the 3.3 version of avaje-ebeanorm-agent with the 5.0.3 version of ASM that was added to avaje-ebeanorm-agent 4.1.8 in https://github.com/ebean-orm/avaje-ebeanorm/issues/187

To use, build this yourself and publish it to your local repository. Include it in your project's dependencies BEFORE the dependency on ebean.
