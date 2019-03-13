
package mysh.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

/**
 * 文件工具类.
 *
 * @author ZhangZhx
 */
public class FilesUtil {
    private static final Logger log = LoggerFactory.getLogger(FilesUtil.class);

    public static final String fileSep;

    static {
        String fileSepProp = System.getProperty("file.separator");
        fileSep = fileSepProp != null ? fileSepProp : "/";
    }

    /**
     * 文件处理任务.
     */
    @Deprecated
    public interface FileTask {
        void handle(File f);
    }

    /**
     * 从文件取得数据.
     * return null if file not exist.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getObjectFromFile(File file) throws IOException, ClassNotFoundException {
        if (!file.exists()) {
            log.error(file + " 不存在, 加载文件失败.");
            return null;
        }

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            T o = (T) in.readObject();
            log.debug("load object from file: " + file);
            return o;
        }
    }

    /**
     * 从文件取得数据(使用 nio 的 file map, 可大幅提升反序列化速度, 但之后文件可能不能被写入, 因为资源释放不受jvm控制).<br/>
     * 建议处理大文件时使用.
     * see <a href='https://en.wikipedia.org/wiki/Memory-mapped_file'>Memory-mapped_file</a>
     * 失败则返回 null.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getObjectFromFileWithFileMap(File file) throws IOException, ClassNotFoundException {
        try (FileChannel fileChannel = FileChannel.open(file.toPath())) {
            MappedByteBuffer fileMap = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, file.length());
            byte[] buf = new byte[(int) file.length()];
            fileMap.get(buf);
            T obj = Serializer.BUILD_IN.deserialize(buf, null);
            log.debug("load object from file: " + file);
            return obj;
        } finally {
            //			http://stackoverflow.com/questions/2972986/how-to-unmap-a-file-from-memory-mapped-using-filechannel-in-java
            System.gc();
        }
    }

    /**
     * 将数据写入到文件.
     *
     * @param file 文件
     * @param obj  要写入的对象.
     */
    public static void writeObjectToFile(File file, Object obj) throws IOException {
        file.getAbsoluteFile().getParentFile().mkdirs();
        File writeFile = getWriteFile(file);
        try (ObjectOutput out = new ObjectOutputStream(new FileOutputStream(writeFile))) {
            out.writeObject(obj);
        }

        file.delete();
        if (!writeFile.renameTo(file))
            throw new IOException("rename file error. " + writeFile.getAbsolutePath() + " -> " + file.getAbsolutePath());
        log.debug("write to file: " + file.getAbsolutePath());
    }

    /**
     * 将数据写入文件.
     *
     * @param file 文件.
     * @param data 要写入数据.
     */
    public static void writeFile(File file, byte[] data) throws IOException {
        file.getAbsoluteFile().getParentFile().mkdirs();
        File writeFile = getWriteFile(file);
        try (FileOutputStream out = new FileOutputStream(writeFile)) {
            out.write(data);
        }

        file.delete();
        if (!writeFile.renameTo(file))
            throw new IOException("rename file error. " + writeFile.getAbsolutePath() + " -> " + file.getAbsolutePath());
        log.debug("write to file: " + file.getAbsolutePath());
    }

    /**
     * return a new file with added extension "~write~", usually used as a temp file.
     */
    public static File getWriteFile(File file) {
        return new File(file.getPath() + ".~write~");
    }

    /**
     * 取文件扩展名(小写).
     *
     * @return 文件扩展名. 不包含点.
     */
    public static String getFileExtension(File file) {

        String filename = file.getName().trim().toLowerCase();
        if (filename.length() == 0)
            return "";
        if (filename.charAt(filename.length() - 1) == '.')
            filename = filename.substring(0, filename.length() - 1);

        int lastPointIndex = filename.lastIndexOf('.');
        if (lastPointIndex < 0)
            return "";
        else
            return filename.substring(lastPointIndex + 1, filename.length());
    }

    /**
     * 取不含扩展名的文件名.
     */
    public static String getFileNameWithoutExtension(File file) {

        String filename = file.getName().trim();
        if (filename.length() == 0)
            return "";
        if (filename.charAt(filename.length() - 1) == '.')
            filename = filename.substring(0, filename.length() - 1);

        int lastPointIndex = filename.lastIndexOf('.');
        if (lastPointIndex < 0)
            return filename;
        else if (lastPointIndex == 0)
            return "";
        else
            return filename.substring(0, lastPointIndex);
    }

    /**
     * 取给定文件路径可写的File, 若文件已存在, 则在文件名后加 "(数字)", 以保证可写入.<br/>
     * 如若给定的路径 c:/a.txt, 目录下 a.txt 和 a (1).txt 已存在, 则返回 c:/a (2).txt 的可写文件.<br/>
     * 注意: 可写文件的状态是瞬时的, 此方法不能锁定此文件, 它随时可能被其他程序锁定.
     */
    public static File getWritableFile(File file) {
        if (file.exists()) {
            File dir = file.getAbsoluteFile().getParentFile();
            String fName = FilesUtil.getFileNameWithoutExtension(file);
            String fExt = FilesUtil.getFileExtension(file);
            fExt = fExt.length() > 0 ? ("." + fExt) : "";

            int i = 0;
            File test;
            do {
                test = new File(dir, fName + " (" + (++i) + ")" + fExt);
            } while (test.exists());
            return test;
        } else {
            file.getAbsoluteFile().getParentFile().mkdirs();
            return file;
        }
    }

    public enum HandleType {FoldersAndFiles, FilesOnly}

    /**
     * 递归处理(广度优先, 含子目录)目录下的所有文件或目录.<br/>
     * 若处理器抛异常, 处理将中断.
     *
     * @param dirRoot     起始目录.
     * @param filter      文件目录过滤器. (为 null 表示不过滤.)
     * @param handleTypes 处理类型
     * @param handler     处理器.
     */
    @Deprecated
    public static void recurDir(File dirRoot, FileFilter filter,
                                EnumSet<HandleType> handleTypes, FileTask handler) {
        Objects.requireNonNull(handleTypes, "handleTypes should not be null");
        boolean chkFolder = handleTypes.contains(HandleType.FoldersAndFiles);

        if (dirRoot == null || !dirRoot.isDirectory() || handler == null)
            throw new IllegalArgumentException();

        List<File> dirs = new LinkedList<>();
        dirs.add(dirRoot);

        while (!dirs.isEmpty()) {
            File[] children = dirs.remove(0).listFiles();

            if (children != null) {
                for (File child : children) {
                    if (child.isDirectory()) {
                        dirs.add(child);
                    }
                    if ((child.isFile() || chkFolder) && (filter == null || filter.accept(child))) {
                        handler.handle(child);
                    }
                }
            }
        }
    }

    /**
     * 递归处理(广度优先, 含子目录)目录下的所有文件.<br/>
     * 若处理器抛异常, 处理将中断.
     *
     * @param dirRoot 目录.
     * @param filter  文件过滤器. (为 null 表示不过滤文件.)
     * @param handler 文件处理器.
     */
    @Deprecated
    public static void recurDir(File dirRoot, FileFilter filter, FileTask handler) {
        recurDir(dirRoot, filter, EnumSet.of(HandleType.FilesOnly), handler);
    }

    public static long folderSize(File dir) throws IOException {
        if (!dir.isDirectory())
            throw new IllegalArgumentException("not directory: " + dir);

        return Files.walk(dir.toPath()).mapToLong(p -> {
            try {
                return Files.size(p);
            } catch (IOException e) {
                log.debug("get-file-size-error:" + p, e);
                return 0;
            }
        }).sum();
    }

    /**
     * escape invalid characters with replacement in file name.
     * invalid chars: [:*?"><|\/]
     */
    public static String escapeFileName(String name, String replacement) {
        return name.replaceAll("[:*?\"><\\|\\\\/]", replacement);
    }

    /**
     * compress object to file.
     */
    public static void compress2File(File file, Serializable obj) throws IOException {
        file.getAbsoluteFile().getParentFile().mkdirs();
        File writeFile = getWriteFile(file);
        try (FileOutputStream out = new FileOutputStream(writeFile)) {
            Compresses.compress("d", new ByteArrayInputStream(Serializer.BUILD_IN.serialize(obj)),
                    Long.MAX_VALUE, out, 100_000);
        }
        file.delete();
        if (!writeFile.renameTo(file))
            throw new IOException("rename file error. " + writeFile.getAbsolutePath() + " -> " + file.getAbsolutePath());
        log.debug("compressed to file: " + file.getAbsolutePath());
    }

    /**
     * compress big object to file.
     */
    public static void compressBigObj2File(File file, Object obj) throws IOException, ExecutionException, InterruptedException {
        if (!(obj instanceof Serializable))
            throw new RuntimeException("obj is not serializable");

        file.getAbsoluteFile().getParentFile().mkdirs();
        File writeFile = getWriteFile(file);
        try (FileOutputStream out = new FileOutputStream(writeFile)) {
            AtomicReference<RunnableFuture<Object>> r = new AtomicReference<>();
            try (OutputStream dataOut = Compresses.getCompressOutputStream("d", out, 1000_000, r)) {
                Serializer.BUILD_IN.serialize((Serializable) obj, dataOut);
            }
            r.get().get();
        }
        file.delete();
        if (!writeFile.renameTo(file))
            throw new IOException("rename file error. " + writeFile.getAbsolutePath() + " -> " + file.getAbsolutePath());
        log.debug("compressed to file: " + file.getAbsolutePath());
    }


    /**
     * decompress file
     */
    public static <T extends Serializable> T decompressFile(File file) throws IOException {
        AtomicReference<T> result = new AtomicReference<>();
        try (FileInputStream in = new FileInputStream(file)) {
            Compresses.deCompress((entry, ein) -> result.set(Serializer.BUILD_IN.deserialize(ein)), in);
            return result.get();
        }
    }

    /**
     * decompress big obj to file
     */
    public static <T extends Serializable> T decompressFileBigObj(File file, int initBuf) throws IOException {
        AtomicReference<T> result = new AtomicReference<>();
        try (FileInputStream in = new FileInputStream(file)) {
            Compresses.deCompress((entry, ein) -> {
                ByteArrayOutputStream out = new ByteArrayOutputStream(initBuf);
                int len;
                byte[] buf = new byte[1000_000];
                try {
                    while ((len = ein.read(buf)) > -1) {
                        out.write(buf, 0, len);
                    }
                } catch (IOException e) {
                    log.error("read-file-error", e);
                }
                T o = Serializer.BUILD_IN.deserialize(out.toByteArray());
                result.set(o);
            }, in);
        }
        return result.get();
    }

    /**
     * remove given file or dir.
     */
    public static void deleteDirOrFile(Path p, boolean followSymLinkDir) throws IOException {
        if (p == null || !Files.exists(p))
            return;

        if (!Files.isDirectory(p))
            Files.delete(p);
        else
            Files.walkFileTree(p, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (Files.isSymbolicLink(dir) && !followSymLinkDir) {
                        Files.delete(dir);
                        return SKIP_SUBTREE;
                    } else
                        return CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return CONTINUE;
                }
            });
    }

    /**
     * remove given file or dir. not follow symbolic link dir by default.
     */
    public static void deleteDirOrFile(Path path) throws IOException {
        deleteDirOrFile(path, false);
    }
}
