package io.herrmann.generator;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Implementation of {@link GeneratorFunc} as an abstract class. This class
 * mainly exists for backwards compatibility, but it can also cut down some of
 * the boilerplate when using an anonymous inner class instead of a lambda.
 * However, unlike a {@link GeneratorFunc}, this class is not stateless, and
 * cannot be used concurrently.
 */
public abstract class Generator<T> implements GeneratorFunc<T> {

	private GeneratorIterator<T> iter;

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
		iter = new GeneratorIterator<>(this);
		return iter;
	}

	/**
	 * Creates a {@link Stream} from a {@link GeneratorFunc}. For cases where
	 * the generator isn't a lambda passed directly, the instance method {@link
	 * #stream()} is generally more concise.
	 *
	 * @param g The generator
	 * @return An ordered, sequential (non-parallel) stream of elements yielded
	 * by the generator
	 * @see #stream()
	 */
	public static <T> Stream<T> stream(GeneratorFunc<T> g) {
		return StreamSupport.stream(g.spliterator(), false);
	}

}
