package io.herrmann.generator;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 * This class represents the state of a {@link Generator}. It contains most of
 * the logic for generators. Its purpose is as a workaround for the
 * statelessness of interfaces.
 *
 * @see Generator
 */
class GeneratorState<T> implements Iterable<T>, Supplier<T> {

	Generator<T> gen;

	GeneratorState(Generator<T> g) {
		gen = g;
	}

	private class Condition {
		private boolean isSet;
		public synchronized void set() {
			isSet = true;
			notify();
		}
		public synchronized void await() throws InterruptedException {
			try {
				if (isSet)
					return;
				wait();
			} finally {
				isSet = false;
			}
		}
	}

	static ThreadGroup THREAD_GROUP;

	Thread producer;
	private boolean hasFinished;
	private final Condition itemAvailableOrHasFinished = new Condition();
	private final Condition itemRequested = new Condition();
	private T nextItem;
	private boolean nextItemAvailable;
	private RuntimeException exceptionRaisedByProducer;

	// Used to implement the get() method
	private Iterator<T> getter = this.iterator();

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			@Override
			public boolean hasNext() {
				return waitForNext();
			}
			@Override
			public T next() {
				if (!waitForNext())
					throw new NoSuchElementException();
				nextItemAvailable = false;
				return nextItem;
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			private boolean waitForNext() {
				if (nextItemAvailable)
					return true;
				if (hasFinished)
					return false;
				if (producer == null)
					startProducer();
				itemRequested.set();
				try {
					itemAvailableOrHasFinished.await();
				} catch (InterruptedException e) {
					hasFinished = true;
				}
				if (exceptionRaisedByProducer != null)
					throw exceptionRaisedByProducer;
				return !hasFinished;
			}
		};
	}

	void yield(T element) throws InterruptedException {
		nextItem = element;
		nextItemAvailable = true;
		itemAvailableOrHasFinished.set();
		itemRequested.await();
	}

	private void startProducer() {
		if (producer != null) {
			throw new IllegalStateException(
				"Can't use the same Generator twice!");
		}

		if (THREAD_GROUP == null)
			THREAD_GROUP = new ThreadGroup("generatorfunctions");
		producer = new Thread(THREAD_GROUP, () -> {
			try {
				itemRequested.await();
				gen.run(gen);
			} catch (InterruptedException e) {
				// No need to do anything here; Remaining steps in run()
				// will cleanly shut down the thread.
			} catch (RuntimeException e) {
				exceptionRaisedByProducer = e;
			}
			hasFinished = true;
			itemAvailableOrHasFinished.set();
		});
		producer.setDaemon(true);
		producer.start();
	}

	@Override
	public T get() {
		return getter.next();
	}

	@Override
	protected void finalize() throws Throwable {
		producer.interrupt();
		producer.join();
		super.finalize();
	}

}
