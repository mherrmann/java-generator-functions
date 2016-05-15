java-generator-functions
========================

An implementation of Python-like generator functions in Java. This repository contains a functional interface, `Generator`, which accepts an object with a method, `yield(...)`, that can be used to mimic the behaviour of the `yield` keyword in Python.

Examples
--------
The following is a simple generator that yields `1` and then `2`:

```java
Generator<Integer> simpleGenerator = s -> {
    s.yield(1);
    // Some logic here...
    s.yield(2);
};

for (Integer element : simpleGenerator)
    System.out.println(element);
// Prints "1", then "2".
```

Infinite generators are also possible:

```java
Generator<Integer> infiniteGenerator = s -> {
    while (true)
        s.yield(1);
};
```

Since you can even use a generator to create a (parallel) `Stream`:

```java
// Note that the generic parameter is necessary, or else Java can't determine
// the generator's type.
Generator.<Integer>stream(s -> {
    int i = 0;
    while (true) {
        s.yield(i++);
    }
}).limit(100).parallel() // and so on
```

The `Generator` class lies in package `io.herrmann.generator`. So you need to `import io.herrmann.generator.Generator;` in order for the above examples to work.

Usage
-----

This package is hosted as a Maven repository with the following url:

    http://dl.bintray.com/filipmalczak/maven

To use it from Maven, add the following to your `pom.xml`:

```xml
<project>
    ...
    <repositories>
        ...
        <repository>
            <id>java-generator-functions</id>
            <url>http://dl.bintray.com/filipmalczak/maven</url>
        </repository>
    </repositories>
    ...
    <dependencies>
        <dependency>
            <groupId>io.herrmann</groupId>
            <artifactId>java-generator-functions</artifactId>
            <version>1.0</version>
        </dependency>
    </dependencies>
</project>
```

For Gradle:

```gradle
compile(group: 'io.herrmann', name: 'java-generator-functions', version: '1.0')
```

Caveats and Performance
-----------------------
The `Generator` library internally works with a Thread to produce the items. It does ensure that no Threads stay around if the corresponding Generator is no longer used. However:

**If too many `Generator`s are created before the JVM gets a chance to garbage collect the old ones, you may encounter `OutOfMemoryError`s. This problem most strongly presents itself on OS X where the maximum number of Threads is significantly lower than on other OSs (around 2000).**

The performance is obviously not great but not too shabby either. On my machine with a dual core i5 CPU @ 2.67 GHz, 1000 items can be produced in < 0.03s.

This version requires Java 8, as it takes advantage of functional interfaces in its API.

Contributing
------------
Contributions and pull requests are welcome. Please ensure that `mvn test` still passes and add any unit tests as you see fit. Please also follow the same coding conventions, in particular the line limit of 80 characters and the use of tabs instead of spaces.
