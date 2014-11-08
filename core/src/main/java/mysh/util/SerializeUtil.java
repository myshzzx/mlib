package mysh.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Objects;

/**
 * @author Mysh
 * @since 2014/10/20 20:49
 */
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
}
