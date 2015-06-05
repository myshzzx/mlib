package mysh.util;

import java.util.Objects;

/**
 * utils for test.
 */
public class Tests {
	/**
	 * execution timer. call nip funcs to get time costs.<br/>
	 * WARNING: should NOT implements Serializable.
	 */
	public static class Tick {
		enum Unit {
			// doesn't include nano-sec here because its accuracy can't reach that level.
			MicroSec("μs", 1000L),
			MilliSec("ms", 1000_000L),
			Sec("s", 1000_000_000L),
			Minute("min", 60_000_000_000L);

			private final String desc;
			private final long fact;

			Unit(String desc, long fact) {
				this.desc = desc;
				this.fact = fact;
			}
		}

		private final String name;
		private final Unit unit;
		private final long from;

		public Tick(String name, Unit unit) {
			this.name = name == null ? "tick" : name;
			this.unit = Objects.requireNonNull(unit, "tick unit can't be null");
			this.from = System.nanoTime();
		}

		/**
		 * time costs from creation.
		 */
		public long nip() {
			return (System.nanoTime() - this.from) / this.unit.fact;
		}

		/**
		 * time costs from creation.
		 */
		public String nip2String() {
			return this.name + ": " + nip() + " " + this.unit.desc;
		}

		/**
		 * print time costs from creation.
		 */
		public void nipAndPrint() {
			System.out.println(nip2String());
		}

		@Override
		public String toString() {
			return "Tick{" +
							"name='" + name + '\'' +
							", unit=" + unit +
							", from=" + from +
							'}';
		}
	}

	/**
	 * start an execution timer(milli-sec) now.
	 */
	public static Tick tick() {
		return new Tick(null, Tick.Unit.MilliSec);
	}

	/**
	 * start an execution timer(milli-sec) now.
	 */
	public static Tick tick(String name) {
		return new Tick(name, Tick.Unit.MilliSec);
	}

	/**
	 * start an execution timer now.
	 */
	public static Tick tick(String name, Tick.Unit unit) {
		return new Tick(name, unit);
	}
}
