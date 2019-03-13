package mysh.spring;

import mysh.util.Serializer;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.Assert.*;

/**
 * @author <pre>凯泓(zhixian.zzx@alibaba-inc.com)</pre>
 * @since 2019-02-19
 */
@Ignore
public class SpringExporterTest extends Assert {

    @Test
    public void fstIO() {
        Serializer fst = Serializer.FST;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        fst.serialize("abc", out);
        Serializable dobj = fst.deserialize(new ByteArrayInputStream(out.toByteArray()));
        assertEquals("abc", dobj);
    }

    @Test
    public void fstNetI() throws IOException {
        Serializer fst = Serializer.FST;
        ServerSocket ss = new ServerSocket(9834);
        Socket sock = ss.accept();
        Serializable dobj = fst.deserialize(sock.getInputStream());
        assertEquals("abc", dobj);
    }

    @Test
    public void fstNetO() throws IOException {
        Serializer fst = Serializer.FST;
        Socket sock = new Socket("l", 9834);
        fst.serialize("abc", sock.getOutputStream());
        System.out.println("end");
    }


}