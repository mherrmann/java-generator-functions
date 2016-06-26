package io.herrmann.generator;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;

/**
 * This functional interface allows specifying Python generator-like sequences.
 * For examples, see the JUnit test case.
 *
 * The implementation uses a separate Thread to produce the sequence items. This
 * is certainly not as fast as eg. a for-loop, but not horribly slow either. On
 * a machine with a dual core i5 CPU @ 2.67 GHz, 1000 items can be produced in
 * &lt; 0.03s.
 *
 * By overriding finalize(), the underlying iterator takes care not to leave any
 * Threads running longer than necessary.
 *
 * @see Generator
 */
@FunctionalInterface
public interface GeneratorFunc<T> extends Iterable<T> {

	@Override
	public default Iterator<T> iterator() {
		return new GeneratorIterator<>(this);
	}

	public void run(GeneratorIterator<T> gen) throws InterruptedException;

	/**
	 * Returns an ordered {@link Spliterator} consisting of elements yielded by
	 * this {@link GeneratorFunc}.
	 */
	@Override
	default Spliterator<T> spliterator() {
		return Spliterators.spliteratorUnknownSize(iterator(),
				Spliterator.ORDERED);
	}

	/**
	 * Creates a {@link Stream} from a {@link GeneratorFunc}. If you are trying
	 * to call this on a lambda, you should either use the static method {@link
	 * Generator#stream()} or assign it to a variable first.
	 *
	 * @param g The generator
	 * @return An ordered, sequential (non-parallel) stream of elements yielded
	 * by the generator
	 */
	public default Stream<T> stream() {
		return Generator.stream(this);
	}

}
