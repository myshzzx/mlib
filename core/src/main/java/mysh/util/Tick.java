package mysh.util;

import mysh.annotation.NotThreadSafe;

import java.util.Objects;

/**
 * execution timer. call nip funcs to get time costs.<br/>
 * WARNING: should NOT implements Serializable.
 */
@NotThreadSafe
public final class Tick {
	/**
	 * start an execution timer(milli-sec) now.
	 */
	public static Tick tick() {
		return new Tick(null, Unit.MilliSec);
	}

	/**
	 * start an execution timer(milli-sec) now.
	 */
	public static Tick tick(String name) {
		return new Tick(name, Unit.MilliSec);
	}

	/**
	 * start an execution timer now.
	 */
	public static Tick tick(String name, Unit unit) {
		return new Tick(name, unit);
	}

	public enum Unit {
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
	private long from;

	public Tick(String name, Unit unit) {
		this.name = name == null ? "tick" : name;
		this.unit = Objects.requireNonNull(unit, "tick unit can't be null");
		this.from = System.nanoTime();
	}

	/**
	 * reset tick to current time.
	 */
	public void reset() {
		this.from = System.nanoTime();
	}

	private long nipsTotal;

	/**
	 * time costs from creation/reset.
	 */
	public long nip() {
		long nip = (System.nanoTime() - this.from) / this.unit.fact;
		nipsTotal += nip;
		return nip;
	}

	/**
	 * time costs from creation/reset.
	 */
	public String nip2String() {
		return this.name + ": " + nip() + " " + this.unit.desc;
	}

	/**
	 * time costs from creation/reset.
	 */
	public String nip2String(String comment) {
		return this.name + " (" + comment + "): " + nip() + " " + this.unit.desc;
	}

	/**
	 * print time costs from creation/reset.
	 */
	public void nipAndPrint() {
		System.out.println(nip2String());
	}

	/**
	 * print time costs from creation/reset.
	 */
	public void nipAndPrint(String comment) {
		System.out.println(nip2String(comment));
	}

	/**
	 * clear counted nipsTotal.
	 */
	public void clearNipsTotal() {
		nipsTotal = 0;
	}

	/**
	 * sum of all nips.
	 */
	public long nipsTotal() {
		return nipsTotal;
	}

	/**
	 * sum of all nips.
	 */
	public String nipsTotal2String() {
		return this.name + " (TOTAL): " + nipsTotal() + " " + this.unit.desc;
	}

	/**
	 * sum of all nips.
	 */
	public void printNipsTotal() {
		System.out.println(nipsTotal2String());
	}

	/**
	 * sum of all nips.
	 */
	public String nipsTotal2String(String comment) {
		return this.name + " (" + comment + ") (TOTAL): " + nipsTotal() + " " + this.unit.desc;
	}

	/**
	 * sum of all nips.
	 */
	public void printNipsTotal(String comment) {
		System.out.println(nipsTotal2String(comment));
	}

	@Override
	public String toString() {
		return "Tick{" +
						"name='" + name + '\'' +
						", unit=" + unit +
						'}';
	}
}
