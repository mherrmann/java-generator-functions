package io.herrmann.generator;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * This functional interface allows specifying Python generator-like sequences.
 * For examples, see the JUnit test case.
 *
 * The implementation uses a separate Thread to produce the sequence items. This
 * is certainly not as fast as eg. a for-loop, but not horribly slow either. On
 * a machine with a dual core i5 CPU @ 2.67 GHz, 1000 items can be produced in
 * &lt; 0.03s.
 *
 * By overriding finalize(), the underlying class takes care not to leave any
 * Threads running longer than necessary.
 */
@FunctionalInterface
public interface Generator<T> extends Iterable<T>, Supplier<T> {

	// Workaround for the fact that interfaces can't have state.
	// I don't know how to make this type-safe without doing something really
	// hacky or re-implementing WeakHashMap, but at least it's package-private.
	static Map<Generator<?>, GeneratorState<?>> states = new WeakHashMap<>();

	@SuppressWarnings("unchecked")
	default GeneratorState<T> getState() {
		synchronized (states) {
			if (!states.containsKey(this)) {
				states.put(this, new GeneratorState<T>(this));
			}

			return (GeneratorState<T>) states.get(this);
		}
	}

	@Override
	public default Iterator<T> iterator() {
		return getState().iterator();
	}

	public void run(GeneratorIterator<T> gen) throws InterruptedException;

	@Override
	public default T get() {
		return getState().get();
	}

	/**
	 * Reset a <i>stateless</i> generator so that its get() method behaves as
	 * though it has never been called. A generator is stateless if its {@link
	 * #run(GeneratorIterator)} method has no sideffects. This is useful when
	 * creating several streams from the same generator.
	 */
	public default void reset() {
		getState().reset();
	}

}
