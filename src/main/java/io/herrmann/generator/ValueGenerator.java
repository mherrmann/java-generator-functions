package io.herrmann.generator;

@FunctionalInterface
public interface ValueGenerator {
	void generate() throws InterruptedException;
}
