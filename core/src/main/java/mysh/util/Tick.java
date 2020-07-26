package mysh.util;

import javax.annotation.concurrent.NotThreadSafe;

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
	
	public final String name;
	public final Unit unit;
	private long lastFlag;
	
	public Tick(String name, Unit unit) {
		this.name = name == null ? "tick" : name;
		this.unit = Objects.requireNonNull(unit, "tick unit can't be null");
		this.lastFlag = System.nanoTime();
	}
	
	/**
	 * reset nip and statistics.
	 */
	public void resetAll() {
		lastFlag = System.nanoTime();
		nipsTotal = 0;
		nipsCount = 0;
	}
	
	public void resetNip() {
		lastFlag = System.nanoTime();
	}
	
	private long nipsTotal;
	private long nipsCount;
	
	/**
	 * time costs from creation/reset.
	 */
	public long nip() {
		long now = System.nanoTime();
		long nip = (now - lastFlag) / unit.fact;
		nipsTotal += nip;
		nipsCount++;
		lastFlag = now;
		return nip;
	}
	
	/**
	 * sum of all nips.
	 */
	public long nipsTotal() {
		return nipsTotal;
	}
	
	/**
	 * nips count.
	 */
	public long nipsCount() {
		return nipsCount;
	}
	
	/**
	 * nips average.
	 */
	public long nipsAverage() {
		return nipsTotal / nipsCount;
	}
	
	@Override
	public String toString() {
		return "Tick{" +
				"name='" + name + '\'' +
				", unit=" + unit +
				'}';
	}
	
}
