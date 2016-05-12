package io.herrmann.generator;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * This class represents the state of a {@link Generator}. Its purpose is as a
 * workaround for the statelessness of interfaces.
 *
 * @see Generator
 */
class GeneratorState<T> implements Iterable<T>, Supplier<T> {

	Generator<T> gen;

	GeneratorState(Generator<T> gen) {
		Objects.requireNonNull(gen);
		this.gen = gen;
		getter = iterator();
	}

	// Used to implement the get() method
	private Iterator<T> getter;

	@Override
	public Iterator<T> iterator() {
		return new GeneratorIterator<T>(gen);
	}

	@Override
	public T get() {
		return getter.next();
	}

	public void reset() {
		getter = iterator();
	}

}
