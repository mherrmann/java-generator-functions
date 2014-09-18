java-generator-functions
========================

An implementation of Python-like generator functions in Java. This repository contains a single class, `Generator` with a method `yield(...)` which can be used to mimic the behaviour of the `yield` keyword in Python.

Examples
--------
The following is a simple generator that yields `1` and then `2`:

    Generator<Integer> simpleGenerator = new Generator<Integer>() {
        public void run() throws InterruptedException {
            yield(1);
            // Some logic here...
            yield(2);
        }
    };
    for (Integer element : simpleGenerator)
        System.out.println(element);
    // Prints "1", then "2".

Infinite generators are also possible:

    Generator<Integer> infiniteGenerator = new Generator<Integer>() {
        public void run() throws InterruptedException {
            while (true)
                yield(1);
        }
    };

The `Generator` class lies in package `io.herrmann.generator`. So you need to `import io.herrmann.generator.Generator;` in order for the above examples to work.

Performance
-----------
The `Generator` class internally works with a Thread to produce the items. By overriding `finalize()`, it ensures that no Threads stay around if the corresponding Generator is no longer used.

The performance is obviously not great but not too shabby either. On my machine with a dual core i5 CPU @ 2.67 GHz, 1000 items can be produced in < 0.03s.

Contributing
------------
Contributions and pull requests are welcome. Please ensure that `mvn test` still passes and add any unit tests as you see fit. Please also follow the same coding conventions, in particular the line limit of 80 characters and the use of tabs instead of spaces.