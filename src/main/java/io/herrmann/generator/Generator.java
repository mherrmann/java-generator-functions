package io.herrmann.generator;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Implementation of {@link GeneratorFunc} as an abstract class. This class
 * mainly exists for backwards compatibility, but it can also cut down some of
 * the boilerplate when using an anonymous inner class instead of a lambda.
 */
public abstract class Generator<T> implements GeneratorFunc<T> {

	private GeneratorIterator<T> iter = new GeneratorIterator<>(this);

	@Override
	public void run(GeneratorIterator<T> gen) throws InterruptedException {
		run();
	}

	protected abstract void run() throws InterruptedException;

	protected void yield(T element) throws InterruptedException {
		iter.yield(element);
	}

	@Override
	public Iterator<T> iterator() {
		return iter;
	}

	/**
	 * Creates a {@link Stream} from a {@link GeneratorFunc}.
	 * @param g The generator
	 * @return An ordered, sequential (non-parallel) stream of elements yielded
	 * by the generator
	 */
	public static <T> Stream<T> stream(GeneratorFunc<T> g) {
		return StreamSupport.stream(g.spliterator(), false);
	}

}
