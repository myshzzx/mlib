package mysh.util;

import mysh.annotation.Nullable;
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

	public static interface ClassLoaderFetcher {
		ClassLoader get();
	}

	/**
	 * serialize object to byte array.
	 */
	public abstract byte[] serialize(Serializable obj);

	public final <T extends Serializable> T unSerialize(byte[] buf) {
		return unSerialize(buf, 0, buf.length, null);
	}

	/**
	 * unSerialize obj from buf.
	 *
	 * @param clFetcher nullable
	 */
	public final <T extends Serializable> T unSerialize(byte[] buf,
	                                                    @Nullable ClassLoaderFetcher clFetcher) {
		return unSerialize(buf, 0, buf.length, clFetcher);
	}

	/**
	 * unSerialize obj from buf.
	 *
	 * @param clFetcher nullable
	 */
	public abstract <T extends Serializable> T unSerialize(byte[] buf, int offset, int length,
	                                                       @Nullable ClassLoaderFetcher clFetcher);

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
		public <T extends Serializable> T unSerialize(byte[] buf, int offset, int length, ClassLoaderFetcher clFetcher) {

			Objects.requireNonNull(buf);

			try (CustObjIS in = new CustObjIS(new ByteArrayInputStream(buf, offset, length), clFetcher)) {
				return (T) in.readObject();
			} catch (Exception e) {
				throw ExpUtil.unchecked(e);
			}
		}

	};

	private static class CustObjIS extends ObjectInputStream {

		private ClassLoaderFetcher clFetcher;

		public CustObjIS(InputStream in, ClassLoaderFetcher clFetcher) throws IOException {
			super(in);
			this.clFetcher = clFetcher;
		}

		@Override
		protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
			return clFetcher == null ?
							super.resolveClass(desc) :
							Class.forName(desc.getName(), false, clFetcher.get());
		}
	}

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
		public <T extends Serializable> T unSerialize(byte[] b, int offset, int length, ClassLoaderFetcher clFetcher) {
			final DefaultCoder c = getCoder();
			if (clFetcher != null)
				c.getConf().setClassLoader(clFetcher.get());
			return (T) c.toObject(b, offset, length);
		}
	};
}
