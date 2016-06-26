package io.herrmann.generator;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * This is the class that contains most of the logic for the {@link Generator}
 * and {@link GeneratorFunc} classes. It is possible to create several of these
 * for one {@link GeneratorFunc}, but use with caution if the generator function
 * is not stateless.
 */
public final class GeneratorIterator<T> implements Iterator<T> {

	static ThreadGroup THREAD_GROUP;

	GeneratorFunc<T> gen;

	public GeneratorIterator(GeneratorFunc<T> gen) {
		Objects.requireNonNull(gen);
		this.gen = gen;
	}

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

	Thread producer;
	private boolean hasFinished;
	private final Condition itemAvailableOrHasFinished = new Condition();
	private final Condition itemRequested = new Condition();
	private T nextItem;
	private boolean nextItemAvailable;
	private RuntimeException exceptionRaisedByProducer;

	public void yield(T element) throws InterruptedException {
		nextItem = element;
		nextItemAvailable = true;
		itemAvailableOrHasFinished.set();
		itemRequested.await();
	}

	private void startProducer() {
		if (producer != null) {
			throw new IllegalStateException(
				"Can't use the same GeneratorIterator twice!");
		}

		if (THREAD_GROUP == null)
			THREAD_GROUP = new ThreadGroup("generatorfunctions");
		producer = new Thread(THREAD_GROUP, () -> {
			try {
				itemRequested.await();
				gen.run(this);
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
	protected void finalize() throws Throwable {
		producer.interrupt();
		producer.join();
		super.finalize();
	}

}