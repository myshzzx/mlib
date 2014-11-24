package mysh.util;

import mysh.annotation.ThreadSafe;
import org.nustaq.serialization.simpleapi.DefaultCoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Objects;

/**
 * @author Mysh
 * @since 2014/11/24 11:31
 */
public abstract class Serializer {
	static final Logger log = LoggerFactory.getLogger(Serializer.class);

	/**
	 * serialize object to byte array.
	 */
	public abstract byte[] serialize(Serializable obj);

	/**
	 * unSerialize obj from buf.
	 */
	public abstract <T extends Serializable> T unSerialize(byte[] buf);

	/**
	 * unSerialize obj from buf.
	 */
	public abstract <T extends Serializable> T unSerialize(byte[] buf, int offset, int length);

	/**
	 * java build-in serializer.
	 */
	@ThreadSafe
	public static final Serializer buildIn = new Serializer() {

		public byte[] serialize(Serializable obj) {
			ByteArrayOutputStream arrOut = new ByteArrayOutputStream();
			try (ObjectOutputStream out = new ObjectOutputStream(arrOut)) {
				out.writeObject(obj);
			} catch (Exception e) {
				throw ExpUtil.unchecked(e);
			}
			return arrOut.toByteArray();
		}

		@SuppressWarnings("unchecked")
		public <T extends Serializable> T unSerialize(byte[] buf) {

			return unSerialize(buf, 0, buf.length);
		}

		@SuppressWarnings("unchecked")
		public <T extends Serializable> T unSerialize(byte[] buf, int offset, int length) {

			Objects.requireNonNull(buf);

			try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buf, offset, length))) {
				return (T) in.readObject();
			} catch (Exception e) {
				throw ExpUtil.unchecked(e);
			}
		}
	};

	/**
	 * fast-serialization.
	 */
	@ThreadSafe
	public static final Serializer fst = new Serializer() {
		private ThreadLocal<DefaultCoder> coder = new ThreadLocal<>();

		private DefaultCoder getCoder() {
			if (coder.get() == null)
				coder.set(new DefaultCoder());
			return coder.get();
		}

		public byte[] serialize(Serializable obj) {
			return getCoder().toByteArray(obj);
		}

		@SuppressWarnings("unchecked")
		public <T extends Serializable> T unSerialize(byte[] b) {
			return (T) getCoder().toObject(b, 0, b.length);
		}

		@SuppressWarnings("unchecked")
		public <T extends Serializable> T unSerialize(byte[] b, int offset, int length) {
			return (T) getCoder().toObject(b, offset, length);
		}
	};
}
