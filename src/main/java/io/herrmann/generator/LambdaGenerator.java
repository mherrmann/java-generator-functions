package io.herrmann.generator;

public class LambdaGenerator<T> extends Generator<T> {
	private ConsumingGenerator<T> generator;
	
	public LambdaGenerator(ConsumingGenerator<T> generator) {
		this.generator = generator;
	}
	
	public LambdaGenerator(final ValueGenerator<T> generator) {
		this(g -> generator.generate());
	}
	
	@Override
	protected void run() throws InterruptedException {
		this.generator.generate(this);
	}
}
