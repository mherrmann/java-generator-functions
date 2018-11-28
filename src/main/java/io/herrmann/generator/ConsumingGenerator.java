package io.herrmann.generator;

@FunctionalInterface
public interface ConsumingGenerator<T> {
	void generate(Generator<T> generator) throws InterruptedException;
}
