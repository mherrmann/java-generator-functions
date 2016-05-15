package io.herrmann.generator;

import org.junit.Test;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
	public void testUseInStream() {
		List<Integer> nums = Arrays.asList(0, 1, 2, 3, 4, 5);

		// Note that the generic parameter is necessary, or else Java can't
		// determine the generator's type.
		int sum = Generator.<Integer>stream(s -> {
			for (int n : nums) {
				s.yield(n);
			}
		}).limit(nums.size()).mapToInt(x -> x).sum();

		assertEquals(sum, nums.stream().mapToInt(x -> x).sum());
	}

	@Test
	public void testUseInParallelStream() {
		// A slightly more realistic usage example: generate a set of lattice
		// points in a given rectangle
		Rectangle r = new Rectangle(2, 3, 2, 4);

		Set<Point> ps = Generator.<Point>stream(s -> {
			for (int x = 0; x < 10; x++) {
				for (int y = 0; y < 10; y ++) {
					s.yield(new Point(x, y));
				}
			}
		}).parallel()
				.filter(r::contains)
				.collect(Collectors.toSet());

		// For comparison, here's what you might have to do to get a parallel
		// stream without this library. More concise? Yes. More
		// intuitive/readable? Probably not.
		Set<Point> ps1 = IntStream.range(0, 100)
				.mapToObj(i -> new Point(i % 10, i / 10))
				.parallel()
				.filter(r::contains)
				.collect(Collectors.toSet());

		// Generate it the old fashioned way for comparison
		Set<Point> ps2 = new HashSet<>();
		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 10; y ++) {
				if (r.contains(x, y)) {
					ps2.add(new Point(x, y));
				}
			}
		}

		assertEquals(ps2, ps);
		assertEquals(ps2, ps1);

		// An infinite generator for fibonacci numbers!
		Generator<Integer> fibs = s -> {
			int a = 0, b = 1;
			while (true) {
				s.yield(a);
				int next = a + b;
				a = b;
				b = next;
			}
		};

		int sum1 = Generator.stream(fibs).limit(45).mapToInt(x -> x).sum();
		assertEquals(1836311902, sum1);

		// An old-fashioned Stream of fibonacci numbers
		int sum2 = Stream.iterate(new int[]{ 0, 1 },
				c -> new int[]{ c[1], c[0] + c[1] })
				.limit(45).parallel().mapToInt(a -> a[0]).sum();
		assertEquals(1836311902, sum2);
	}

}
