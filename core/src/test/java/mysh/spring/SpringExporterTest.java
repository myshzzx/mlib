package mysh.spring;

import mysh.util.Serializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @since 2019-02-19
 */
@Disabled
public class SpringExporterTest extends Assertions {

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