package mysh.util;

import mysh.annotation.Nullable;
import mysh.annotation.ThreadSafe;
import org.nustaq.serialization.simpleapi.DefaultCoder;

import java.io.*;
import java.util.Objects;

/**
 * multi serializer collection.
 * Kryo 3.0 was tested and obsoleted, which was slower than fst and less compatible.
 *
 * @author Mysh
 * @since 2014/11/24 11:31
 */
public interface Serializer {

	/**
	 * serialize object to byte array.
	 */
	byte[] serialize(Serializable obj);

	/**
	 * serialize object to output stream.
	 */
	void serialize(Serializable obj, OutputStream out);

	/**
	 * deserialize obj from buf using default class loader.
	 */
	default <T extends Serializable> T deserialize(byte[] buf) {
		return deserialize(buf, 0, buf.length, null);
	}

	/**
	 * deserialize obj from buf.
	 *
	 * @param cl nullable
	 */
	default <T extends Serializable> T deserialize(byte[] buf, @Nullable ClassLoader cl) {
		return deserialize(buf, 0, buf.length, cl);
	}

	/**
	 * deserialize obj from buf.
	 *
	 * @param cl nullable
	 */
	<T extends Serializable> T deserialize(byte[] buf, int offset, int length, @Nullable ClassLoader cl);

	/**
	 * deserialize obj from input stream.
	 *
	 * @param cl nullable
	 */
	<T extends Serializable> T deserialize(InputStream is, ClassLoader cl);

	/**
	 * deserialize obj from input stream.
	 */
	<T extends Serializable> T deserialize(InputStream is);

	/**
	 * java build-in serializer.
	 */
	@ThreadSafe
	Serializer buildIn = new Serializer() {

		public byte[] serialize(Serializable obj) {
			ByteArrayOutputStream arrOut = new ByteArrayOutputStream();
			serialize(obj, arrOut);
			return arrOut.toByteArray();
		}

		public void serialize(Serializable obj, OutputStream out) {
			try {
				ObjectOutputStream oos = new ObjectOutputStream(out);
				oos.writeObject(obj);
			} catch (Exception e) {
				throw Exps.unchecked(e);
			}
		}

		public <T extends Serializable> T deserialize(byte[] buf, int offset, int length, ClassLoader cl) {
			Objects.requireNonNull(buf);

			ByteArrayInputStream bis = new ByteArrayInputStream(buf, offset, length);
			return deserialize(bis, cl);
		}

		@SuppressWarnings("unchecked")
		public <T extends Serializable> T deserialize(InputStream is, ClassLoader cl) {
			try {
				CustObjIS in = new CustObjIS(is, cl);
				return (T) in.readObject();
			} catch (Exception e) {
				throw Exps.unchecked(e);
			}
		}

		public <T extends Serializable> T deserialize(InputStream is) {
			return deserialize(is, null);
		}
	};

	class CustObjIS extends ObjectInputStream {

		private ClassLoader cl;

		public CustObjIS(InputStream in, ClassLoader cl) throws IOException {
			super(in);
			this.cl = cl;
		}

		@Override
		protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
			return cl == null ?
							super.resolveClass(desc) :
							Class.forName(desc.getName(), false, cl);
		}
	}

	/**
	 * fast-serialization.
	 */
	@ThreadSafe
	Serializer fst = new Serializer() {
		private ThreadLocal<DefaultCoder> coder = new ThreadLocal<>();

		private DefaultCoder getCoder() {
			if (coder.get() == null)
				coder.set(new DefaultCoder());
			return coder.get();
		}

		public byte[] serialize(Serializable obj) {
			return getCoder().toByteArray(obj);
		}

		@Override
		public void serialize(Serializable obj, OutputStream out) {
			try {
				out.write(serialize(obj));
			} catch (IOException e) {
				throw Exps.unchecked(e);
			}
		}

		@SuppressWarnings("unchecked")
		public <T extends Serializable> T deserialize(byte[] b, int offset, int length, ClassLoader cl) {
			final DefaultCoder c = getCoder();
			c.getConf().setClassLoader(cl == null ? getClass().getClassLoader() : cl);
			return (T) c.toObject(b, offset, length);
		}

		@SuppressWarnings("unchecked")
		public <T extends Serializable> T deserialize(InputStream is, ClassLoader cl) {
			throw new UnsupportedOperationException("doesn't support multi-object now");

//			final DefaultCoder c = getCoder();
//			c.getConf().setClassLoader(cl == null ? getClass().getClassLoader() : cl);
//			try {
//				return (T) c.getConf().getObjectInput(is).readObject();
//			} catch (Exception e) {
//				throw Exps.unchecked(e);
//			}
		}

		public <T extends Serializable> T deserialize(InputStream is) {
			return deserialize(is, null);
		}
	};


}
