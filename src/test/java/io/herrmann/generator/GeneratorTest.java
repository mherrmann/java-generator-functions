package io.herrmann.generator;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GeneratorTest {
	@Test
	public void testEmptyGenerator() {
		assertEquals(new ArrayList<Object>(), list(new EmptyGenerator()));
	}
	private class EmptyGenerator extends Generator {
		@Override
		protected void run() {
		}
	}
	public static <T> List<T> list(Iterable<T> iterable) {
		List<T> result = new ArrayList<T>();
		for (T item : iterable)
			result.add(item);
		return result;
	}
	@Test
	public void testOneEltGenerator() {
		List<Integer> oneEltList = Arrays.asList(1);
		assertEquals(oneEltList, list(new ListGenerator<Integer>(oneEltList)));
	}
	private class ListGenerator<T> extends Generator<T> {
		private final List<T> elements;
		public ListGenerator(List<T> elements) {
			this.elements = elements;
		}
		protected void run() throws InterruptedException {
			for (T element : elements)
				yield(element);
		}
	}
	@Test
	public void testTwoEltGenerator() {
		List<Integer> twoEltList = Arrays.asList(1, 2);
		assertEquals(twoEltList, list(new ListGenerator<Integer>(twoEltList)));
	}
	@Test
	public void testInfiniteGenerator() {
		InfiniteGenerator generator = new InfiniteGenerator();
		testInfiniteGenerator(generator);
	}
	public void testInfiniteGenerator(InfiniteGenerator generator) {
		int NUM_ELTS_TO_INSPECT = 1000;
		Iterator<Integer> generatorIterator = generator.iterator();
		for (int i=0; i < NUM_ELTS_TO_INSPECT; i++) {
			assertTrue(generatorIterator.hasNext());
			assertEquals(1, (int) generatorIterator.next());
		}
	}
	private class InfiniteGenerator extends Generator<Integer> {
		@Override
		protected void run() throws InterruptedException {
			while (true)
				yield(1);
		}
	}
	@Test
	public void testInfiniteGeneratorLeavesNoRunningThreads() throws Throwable {
		InfiniteGenerator generator = new InfiniteGenerator();
		testInfiniteGenerator(generator);
		generator.finalize();
		assertEquals(Thread.State.TERMINATED, generator.producer.getState());
	}

	private class CustomRuntimeException extends RuntimeException {}

	private class GeneratorRaisingException extends Generator<Integer> {
		@Override
		protected void run() throws InterruptedException {
			throw new CustomRuntimeException();
		}
	}

	@Test(expected = CustomRuntimeException.class)
	public void testGeneratorRaisingExceptionHasNext() {
		GeneratorRaisingException generator = new GeneratorRaisingException();
		Iterator<Integer> iterator = generator.iterator();
		iterator.hasNext();
	}

	@Test(expected = CustomRuntimeException.class)
	public void testGeneratorRaisingExceptionNext() {
		GeneratorRaisingException generator = new GeneratorRaisingException();
		Iterator<Integer> iterator = generator.iterator();
		iterator.next();
	}

	@Test
	public void testUseAsSupplier() {
		List<Integer> nums = Arrays.asList(0, 1, 2, 3, 4, 5);

		int sum = Stream.generate(new Generator<Integer>() {
			@Override
			protected void run() throws InterruptedException {
				for (int n : nums) {
					yield(n);
				}
			}
		}).limit(nums.size()).mapToInt(x -> x).sum();

		assertEquals(sum, nums.stream().mapToInt(x -> x).sum());
	}

	@Test(expected = NoSuchElementException.class)
	public void testNoSuchElementInSupplier() {
		new EmptyGenerator().get();
	}

}
