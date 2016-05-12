package io.herrmann.generator;

import static org.hamcrest.core.Is.*;
import org.junit.Test;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GeneratorTest {

	private static Generator<Void> emptyGenerator = s -> {};

	private static Generator<Integer> infiniteGenerator = s -> {
		while (true) {
			s.yield(1);
		}
	};

	@Test
	public void testEmptyGenerator() {
		assertEquals(new ArrayList<Object>(), list(emptyGenerator));
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

	private class ListGenerator<T> implements Generator<T> {
		private final List<T> elements;
		public ListGenerator(List<T> elements) {
			this.elements = elements;
		}

		public void run(GeneratorIterator<T> self) throws InterruptedException {
			for (T element : elements)
				self.yield(element);
		}
	}

	@Test
	public void testTwoEltGenerator() {
		List<Integer> twoEltList = Arrays.asList(1, 2);
		assertEquals(twoEltList, list(new ListGenerator<Integer>(twoEltList)));
	}

	@Test
	public void testInfiniteGenerator() {
		Generator<Integer> generator = infiniteGenerator;
		testInfiniteGenerator(generator.iterator());
	}

	public void testInfiniteGenerator(Iterator<Integer> generatorIterator) {
		int NUM_ELTS_TO_INSPECT = 1000;
		for (int i=0; i < NUM_ELTS_TO_INSPECT; i++) {
			assertTrue(generatorIterator.hasNext());
			assertEquals(1, (int) generatorIterator.next());
		}
	}

	@Test
	public void testInfiniteGeneratorLeavesNoRunningThreads() throws Throwable {
		Generator<Integer> generator = infiniteGenerator;
		GeneratorIterator<Integer> iterator =
				(GeneratorIterator<Integer>) generator.iterator();
		testInfiniteGenerator(iterator);
		iterator.finalize();
		assertEquals(Thread.State.TERMINATED,
				iterator.producer.getState());
	}

	private class CustomRuntimeException extends RuntimeException {}

	@Test(expected = CustomRuntimeException.class)
	public void testGeneratorRaisingExceptionHasNext() {
		Generator<Integer> generator = s -> { throw new CustomRuntimeException(); };
		Iterator<Integer> iterator = generator.iterator();
		iterator.hasNext();
	}

	@Test(expected = CustomRuntimeException.class)
	public void testGeneratorRaisingExceptionNext() {
		Generator<Integer> generator = s -> { throw new CustomRuntimeException(); };
		Iterator<Integer> iterator = generator.iterator();
		iterator.next();
	}

	@Test
	public void testUseAsSupplier() {
		List<Integer> nums = Arrays.asList(0, 1, 2, 3, 4, 5);

		// Note that the cast is necessary, or else Java can't determine the
		// lambda's type.
		int sum = Stream.generate((Generator<Integer>) self -> {
			for (int n : nums) {
				self.yield(n);
			}
		}).limit(nums.size()).mapToInt(x -> x).sum();

		assertEquals(sum, nums.stream().mapToInt(x -> x).sum());

		// A slightly more realistic usage example: generate a list of lattice
		// points in a given rectangle
		Rectangle r = new Rectangle(2, 3, 2, 1);
		List<Point> ps = Stream.generate((Generator<Point>) s -> {
			for (int x = 0; x < 10; x++) {
				for (int y = 0; y < 10; y ++) {
					s.yield(new Point(x, y));
				}
			}
		}).limit(100)//.parallel() -- currently not thread safe
				.filter(r::contains)
				.collect(Collectors.toList());

		// Generate it the old fashioned way for comparison
		List<Point> ps2 = new ArrayList<>();
		for (int x = r.x; x < r.x + r.width; x++) {
			for (int y = r.y; y < r.y + r.height; y++) {
				ps2.add(new Point(x, y));
			}
		}

		assertEquals(ps2, ps);
	}

	@Test(expected = NoSuchElementException.class)
	public void testNoSuchElementInSupplier() {
		emptyGenerator.get();
	}

	@Test
	public void testReset() {
		Generator<Integer> naturalNumbers = s -> {
			int i = 0;
			while (true) {
				s.yield(i++);
			}
		};

		// sum of 0-4
		int sum = Stream.generate(naturalNumbers)
				.limit(5).mapToInt(x -> x).sum();

		assertEquals(sum, 10);

		naturalNumbers.reset();

		// sum of 0-9
		sum = Stream.generate(naturalNumbers)
				.limit(10).mapToInt(x -> x).sum();

		assertEquals(sum, 45);
	}

}
