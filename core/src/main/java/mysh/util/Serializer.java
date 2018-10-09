package mysh.util;

import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import javax.annotation.Nullable;
import java.io.*;
import java.util.Objects;

/**
 * multi serializer collection.
 * Kryo 3.0 was tested and obsoleted, which was slower than fst and less compatible.
 * all implementations here are thread-safe.
 *
 * @author Mysh
 * @since 2014/11/24 11:31
 */
public interface Serializer {

    /**
     * serialize object to byte array.
     */
    byte[] serialize(Serializable obj);

    byte[] serialize(Serializable obj, int bufSize);

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
    Serializer buildIn = new Serializer() {

        public byte[] serialize(Serializable obj) {
            ByteArrayOutputStream arrOut = new ByteArrayOutputStream();
            serialize(obj, arrOut);
            return arrOut.toByteArray();
        }

        @Override
        public byte[] serialize(Serializable obj, int bufSize) {
            ByteArrayOutputStream arrOut = new ByteArrayOutputStream(bufSize);
            serialize(obj, arrOut);
            return arrOut.toByteArray();
        }

        public void serialize(Serializable obj, OutputStream out) {
            try {
                ObjectOutputStream oos = new ObjectOutputStream(out);
                oos.writeObject(obj);
                oos.flush();
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
     * 由于不同版本序列化数据无法兼容, 故需要长时间保存的序列化数据不可使用此序列化器, 以免 fst 版本升级后无法处理旧数据.
     */
    Serializer fst = new Serializer() {
        private ThreadLocal<FSTConfiguration> coder = new ThreadLocal<>();
        private ThreadLocal<byte[]> properBuf = new ThreadLocal<>();

        private FSTConfiguration getCoder() {
            FSTConfiguration c = coder.get();
            if (c == null) {
                c = FSTConfiguration.createDefaultConfiguration();
                c.setForceSerializable(true);
                coder.set(c);
            }
            return c;
        }

        private byte[] getProperBuf() {
            byte[] buf = properBuf.get();
            if (buf == null) {
                buf = new byte[50_000];
                properBuf.set(buf);
            }
            return buf;
        }

        int BUF_LIMIT = 500_000;

        @Override
        public byte[] serialize(Serializable obj) {
            try {
                FSTObjectOutput fo = getCoder().getObjectOutput();
                fo.writeObject(obj);
                byte[] buf = fo.getCopyOfWrittenBuffer();

                if (fo.getBuffer().length > BUF_LIMIT)
                    fo.resetForReUse(getProperBuf());
                return buf;
            } catch (IOException e) {
                throw Exps.unchecked(e);
            }
        }

        @Override
        public byte[] serialize(Serializable obj, int bufSize) {
            return serialize(obj);
        }

        final ByteArrayOutputStream emptyOut = new ByteArrayOutputStream();

        @Override
        public void serialize(Serializable obj, OutputStream out) {
            try {
                FSTConfiguration c = getCoder();
                FSTObjectOutput fo = c.getObjectOutput(out);
                fo.writeObject(obj);
                fo.flush();

                if (fo.getBuffer().length > BUF_LIMIT)
                    fo.resetForReUse(getProperBuf());
                c.getObjectOutput(emptyOut);
            } catch (IOException e) {
                throw Exps.unchecked(e);
            }
        }

        @SuppressWarnings("unchecked")
        public <T extends Serializable> T deserialize(byte[] b, int offset, int length, ClassLoader cl) {
            try {
                FSTConfiguration c = getCoder();
                c.setClassLoader(cl == null ? getClass().getClassLoader() : cl);
                FSTObjectInput fi = offset == 0 ?
                        c.getObjectInput(b, length) : c.getObjectInputCopyFrom(b, offset, length);
                T obj = (T) fi.readObject();

                if (length > BUF_LIMIT)
                    fi.resetForReuseUseArray(getProperBuf());
                return obj;
            } catch (Exception e) {
                throw Exps.unchecked(e);
            }
        }

        ByteArrayInputStream emptyIn = new ByteArrayInputStream(new byte[0]);

        @SuppressWarnings("unchecked")
        public <T extends Serializable> T deserialize(InputStream is, ClassLoader cl) {
            FSTConfiguration c = getCoder();
            c.setClassLoader(cl == null ? getClass().getClassLoader() : cl);
            try {
                FSTObjectInput fi = c.getObjectInput(is);
                T obj = (T) fi.readObject();

                if (fi.getCodec().getBuffer().length > BUF_LIMIT)
                    fi.resetForReuseUseArray(getProperBuf());
                c.getObjectInput(emptyIn);
                return obj;
            } catch (Exception e) {
                throw Exps.unchecked(e);
            }
        }

        public <T extends Serializable> T deserialize(InputStream is) {
            return deserialize(is, null);
        }
    };


}
