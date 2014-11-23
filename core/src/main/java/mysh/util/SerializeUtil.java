package mysh.util;

import mysh.annotation.ThreadSafe;
import org.nustaq.serialization.simpleapi.DefaultCoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Base64;
import java.util.Objects;

/**
 * @author Mysh
 * @since 2014/10/20 20:49
 */
@ThreadSafe
public class SerializeUtil {
	private static final Logger log = LoggerFactory.getLogger(SerializeUtil.class);


	/**
	 * serialize object to byte array.
	 */
	public static byte[] serialize(Serializable obj) throws IOException {
		ByteArrayOutputStream arrOut = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(arrOut)) {
			out.writeObject(obj);
		}
		return arrOut.toByteArray();
	}

	/**
	 * 从缓存反序列化数据. 失败则抛异常.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T unSerialize(byte[] buf) throws IOException, ClassNotFoundException {

		return unSerialize(buf, 0, buf.length);
	}

	/**
	 * 从缓存反序列化数据. 失败则抛异常.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T unSerialize(byte[] buf, int offset, int length)
					throws IOException, ClassNotFoundException {

		Objects.requireNonNull(buf);

		try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buf, offset, length))) {
			return (T) in.readObject();
		}
	}

	/**
	 * lazy loader.
	 */
	private static class FstCoder {
		@ThreadSafe
		private static DefaultCoder coder = new DefaultCoder();
	}

	/**
	 * serialize object to byte array using fast-serialization.
	 */
	public static byte[] serializeFST(Serializable obj) {
		try {
			byte[] bytes = FstCoder.coder.toByteArray(obj);
			log.debug("ser " + Base64.getEncoder().encodeToString(bytes));
			return bytes;
		} catch (Exception e) {
			log.error("serializeFST error.\n" + obj, e);
			return null;
		}
	}

	/**
	 * unSerialize to object using fast-serialization.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T unSerializeFST(byte[] b) {
		return unSerializeFST(b, 0, b.length);
	}

	/**
	 * unSerialize to object using fast-serialization.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T unSerializeFST(byte[] b, int offset, int length) {
		try {
			return (T) FstCoder.coder.toObject(b, offset, length);
		} catch (Exception e) {
			byte[] bb = new byte[length];
			System.arraycopy(b, offset, bb, 0, length);
			log.error("unSerializeFST error.\n" + Base64.getEncoder().encodeToString(bb), e);
			return null;
		}
	}
}
