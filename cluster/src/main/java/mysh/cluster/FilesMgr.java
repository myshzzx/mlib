package mysh.cluster;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import mysh.util.FilesUtil;
import mysh.util.Oss;
import mysh.util.Strings;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * file structure:
 * <pre>
 *     main/core/* : cluster core files
 *     main/su/ns/*.jar : super user jars, with super privilege
 *     main/user/ns/*.jar : common user jars, with limited privilege
 *     update/core/* : cluster core update files
 *     work/ns/* : user work files
 * </pre>
 *
 * @author Mysh
 * @since 2014/12/10 19:21
 */
public class FilesMgr implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(FilesMgr.class);

    public static final String mainDir = "main";
    public static final String updateDir = "update";
    public static final String workDir = "work";

    public enum FileType {
        /**
         * cluster's core files
         */
        CORE("core"),
        /**
         * super user files
         */
        SU("su"),
        /**
         * user files
         */
        USER("user");

        private String dir;

        FileType(String dir) {
            this.dir = dir;
        }

        static FileType parse(String name) {
            if (CORE.dir.equals(name))
                return CORE;
            else if (SU.dir.equals(name))
                return SU;
            else if (USER.dir.equals(name))
                return USER;
            else
                throw new IllegalArgumentException("unknown fileType name: " + name);
        }

        String getDir() {
            return dir;
        }
    }

    public enum UpdateType {
        UPDATE, DELETE;
    }

    static {
        new File(mainDir, FileType.CORE.dir).mkdirs();
        new File(mainDir, FileType.SU.dir).mkdirs();
        new File(mainDir, FileType.USER.dir).mkdirs();
        new File(updateDir, FileType.CORE.dir).mkdirs();
        new File(workDir).mkdirs();
    }

    public final Map<String, ClassLoader> loaders = new ConcurrentHashMap<>();

    private volatile FilesInfo old;

    private final FilesInfo curr = new FilesInfo();

    public FilesMgr() throws IOException {
        curr.filesTsMap = new HashMap<>();

        Path mainPath = Paths.get(mainDir);
        Files.walk(mainPath, 3).forEach(p -> {
            File file = p.toFile();
            if (!file.isFile() || file.getName().endsWith(".DS_Store"))
                return;
            try {
                final String name = mainPath.relativize(p).toString().replace('\\', '/');
                final String sha = getThumbStamp(Files.readAllBytes(p));
                curr.filesTsMap.put(name, sha);
            } catch (Throwable e) {
                log.error("read-file-error: " + p, e);
            }
        });

        this.refreshTS();

        old = new FilesInfo(curr);

        renewCl(null);
    }

    private static String[] userLibDirs = {FileType.SU.dir, FileType.USER.dir};

    private void renewCl(String ns) throws IOException {
        if (ns == null) { // renew all loaders
            Set<String> nsSet = new HashSet<>();
            for (String dir : userLibDirs) {
                for (File f : new File(mainDir, dir).listFiles()) {
                    if (f.isDirectory())
                        nsSet.add(f.getName());
                }
            }

            for (String tNs : nsSet)
                renewCl(tNs);
        } else { // renew specified namespace class loader
            ArrayList<URL> urls = new ArrayList<>();
            for (String dir : userLibDirs) {
                File nsDir = Paths.get(mainDir, dir, ns).toFile();
                if (nsDir.isDirectory()) {
                    Files.walk(nsDir.toPath(), 1).forEach(p -> {
                        File f = p.toFile();
                        if (f.isFile() && f.getName().toLowerCase().endsWith(".jar")) {
                            try {
                                urls.add(new URL("jar:file:///" + f.getAbsolutePath() + "!/"));
                            } catch (MalformedURLException e) {
                                log.error("renewCl-add-jar-error", e);
                            }
                        }
                    });
                }
            }
            URLClassLoader cl = new URLClassLoader(urls.toArray(new URL[urls.size()]), getClass().getClassLoader());
            ClassLoader oldCl = loaders.put(ns, cl);
            if (oldCl instanceof Closeable)
                ((Closeable) oldCl).close();
            // todo : if the NS needs to do init and destroy, a config file can be used to config the
            // life-cycle management, like specifying a LifeCycMgr, which will be executed when putFile and removeFile
        }
    }

    /**
     * get current applied files info. (files in main/core and main/user)
     */
    FilesInfo getFilesInfo() {
        return old;
    }

    private void refreshTS() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            List<Map.Entry<String, String>> filesTs = new ArrayList<>(curr.filesTsMap.entrySet());
            filesTs.sort(Comparator.comparing(Map.Entry::getKey));
            for (Map.Entry<String, String> e : filesTs) {
                out.write(e.getKey().getBytes(StandardCharsets.UTF_8));
                out.write('|');
                out.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                out.write('|');
            }
        } catch (Throwable e) {
            log.error("refresh-thumbStamp-error.", e);
        }
        curr.thumbStamp = getThumbStamp(out.toByteArray());
    }

    private static String getThumbStamp(byte[] ctx) {
        return DigestUtils.md5Hex(ctx);
    }

    private final ReentrantReadWriteLock fileLock = new ReentrantReadWriteLock();

    private Cache<String, WeakReference<byte[]>> filesCache = CacheBuilder.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build();

    /**
     * get file by name. about name, see {@link mysh.cluster.FilesInfo#filesTsMap}
     */
    byte[] getFile(String name) throws IOException {
        name = name.trim();

        fileLock.readLock().lock();
        try {
            final String filesCacheKey = name.intern();

            // read from cache
            WeakReference<byte[]> fileCtx = filesCache.getIfPresent(filesCacheKey);
            byte[] ctx;
            if (fileCtx != null && (ctx = fileCtx.get()) != null)
                return ctx;

            // read file and update cache
            synchronized (filesCacheKey) {
                ctx = Files.readAllBytes(Paths.get(mainDir, name));
                filesCache.put(filesCacheKey, new WeakReference<>(ctx));
                return ctx;
            }
        } finally {
            fileLock.readLock().unlock();
        }
    }

    /**
     * put file operation, then update FilesInfo if update SU/USER files.
     */
    void putFile(FileType type, String ns, String fileName, byte[] ctx) throws IOException {
        fileName = fileName.trim();

        log.info("update-file: (" + type + ") " + (ns != null ? ns + '/' : "") + fileName);
        checkNs(ns);
        checkFileName(fileName);

        fileLock.writeLock().lock();
        try {
            if (type == FileType.CORE) {
                if (ns != null)
                    throw new IllegalArgumentException("namespace should be null");
                writeFile(Paths.get(updateDir, type.dir, fileName), ctx);
            } else if (type == FileType.USER || type == FileType.SU) {
                if (ns == null)
                    throw new IllegalArgumentException("namespace can't be null");
                try {
                    // close class loader and write file
                    ClassLoader oldCl = loaders.get(ns);
                    if (oldCl instanceof Closeable)
                        ((Closeable) oldCl).close();

                    writeFile(Paths.get(mainDir, type.dir, ns, fileName), ctx);

                    // update files info
                    String fileKey = type.dir + '/' + ns + '/' + fileName;
                    curr.filesTsMap.put(fileKey, getThumbStamp(ctx));
                    refreshTS();
                    old = new FilesInfo(curr);

                    // put cache
                    filesCache.put(fileKey.intern(), new WeakReference<>(ctx));
                } finally {
                    renewCl(ns);
                }
            } else
                throw new RuntimeException("unknown fileType: " + type);
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    private static void writeFile(Path p, byte[] ctx) throws IOException {
        p.toFile().getAbsoluteFile().getParentFile().mkdirs();
        Files.write(p, ctx);
    }

    private static void checkFileName(String fileName) {
        if (Strings.isBlank(fileName) || fileName.contains("\\") || fileName.contains("/") || fileName.contains(FilesUtil.fileSep))
            throw new IllegalArgumentException("illegal file name: " + fileName);
    }

    /**
     * remove file operation, then update FilesInfo if update USER files.
     */
    void removeFile(FileType type, String ns, String fileName) throws IOException {
        fileName = fileName.trim();

        log.info("remove-file: (" + type + ") " + (ns != null ? ns + '/' : "") + fileName);
        checkNs(ns);
        checkFileName(fileName);

        fileLock.writeLock().lock();
        try {
            if (type == FileType.CORE) { // update core files
                if (ns != null)
                    throw new IllegalArgumentException("namespace should be null");

                // delete file in update dir
                final File updateDirFile = Paths.get(updateDir, FileType.CORE.dir, fileName).toFile();
                updateDirFile.delete();

                // write delete script
                String updateScriptFile, script;
                Charset charset;
                if (Oss.getOS() == Oss.OS.Windows) {
                    updateScriptFile = "update.bat";
                    charset = StandardCharsets.US_ASCII;
                    script = "del /f /q \"" + mainDir + "\\" + type.dir + "\\" + fileName + "\"";
                } else {
                    updateScriptFile = "update.sh";
                    charset = StandardCharsets.UTF_8;
                    script = "rm -f \"" + mainDir + "/" + type.dir + "/" + fileName + "\"";
                }
                script += System.lineSeparator();
                final Path usFile = Paths.get(updateScriptFile);
                Files.write(usFile, script.getBytes(charset),
                        StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                usFile.toFile().setExecutable(true, false);
            } else if (type == FileType.USER || type == FileType.SU) { // update user files
                if (ns == null)
                    throw new IllegalArgumentException("namespace can't be null");
                try {
                    // close class loader and delete file
                    ClassLoader oldCl = loaders.get(ns);
                    if (oldCl instanceof Closeable)
                        ((Closeable) oldCl).close();

                    Paths.get(mainDir, type.dir, ns, fileName).toFile().delete();

                    // update files info
                    String fileKey = type.dir + '/' + ns + '/' + fileName;
                    curr.filesTsMap.remove(fileKey);
                    refreshTS();
                    old = new FilesInfo(curr);

                    // clear cache
                    filesCache.invalidate(fileKey.intern());
                } finally {
                    renewCl(ns);
                }
            } else
                throw new RuntimeException("unknown fileType: " + type);
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    /**
     * remove all files
     */
    void clearNsFiles(String ns) throws IOException {
        checkNs(ns);

        Stream.of(FileType.SU, FileType.USER).forEach(type -> {
            try {
                Path dir = Paths.get(mainDir, type.dir, ns);
                if (Files.isDirectory(dir)) {
                    Files.walk(dir).forEach(p -> {
                        File file = p.toFile();
                        String name = file.getName();
                        if (!file.isFile() || name.endsWith(".DS_Store"))
                            return;

                        try {
                            removeFile(type, ns, name);
                        } catch (IOException e) {
                            log.error("clearFiles-error:" + file, e);
                        }
                    });
                }
            } catch (IOException e) {
                log.error("clearNsFiles-fail,type={},ns={}", type, ns, e);
            }
        });
    }

    @Override
    public void close() throws IOException {
        for (ClassLoader loader : loaders.values()) {
            if (loader instanceof Closeable)
                ((Closeable) loader).close();
        }
    }

    /**
     * get restart cmd.
     */
    public String[] getRestartCmd() {
        final String pid = String.valueOf(Oss.getPid());

        if (Oss.getOS() == Oss.OS.Windows)
            return new String[]{"startCluster.bat", pid};
        else
            return new String[]{"/bin/sh", "startCluster.sh", pid};
    }

    /**
     * check namespace. namespace can be null, or consists of [a-zA-Z0-9\.-_].
     */
    public static void checkNs(String ns) {
        if (ns != null) {
            if (ns.length() == 0)
                throw new IllegalArgumentException("namespace can't be blank string");
            for (int i = 0; i < ns.length(); i++) {
                char c = ns.charAt(i);
                if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '.' || c == '-' || c == '_'))
                    throw new IllegalArgumentException("illegal character in namespace: [" + c + "]");
            }
        }
    }
}
