package mysh.codegen;


import mysh.util.Encodings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static mysh.codegen.CodeUtil.camel2underline;
import static mysh.codegen.CodeUtil.field2MethodSign;
import static mysh.codegen.CodeUtil.isLowerCase;
import static mysh.codegen.CodeUtil.isUpperCase;
import static mysh.codegen.CodeUtil.method2FieldSign;
import static mysh.codegen.CodeUtil.toUpperCase;
import static mysh.codegen.CodeUtil.underline2FieldCamel;
import static mysh.codegen.CodeUtil.underline2camel;


/**
 * @author Mysh
 * @since 2014/4/17 16:13
 */
public class CodeUtilTest {
    @Test
    @Disabled
    public void genPropCopy() throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get("e:/temp/code.txt"));
        String fieldsDefine = Encodings.isUTF8Bytes(bytes) ?
                new String(bytes, Encodings.UTF_8) : new String(bytes, Encodings.GBK);
        String code = CodeUtil.genPropCopy("status", "bean", fieldsDefine);
        System.out.println(code);
    }

    @Test
    public void testUnderline2camel() {
        Assertions.assertEquals("MyshZzx", underline2camel("mysh_zzx"));
        Assertions.assertEquals("MyshZZX", underline2camel("mysh_z_z_x"));
        Assertions.assertEquals("Mysh", underline2camel("MYSH"));
        Assertions.assertEquals("MZZx", underline2camel("M_Z_ZX"));
    }

    @Test
    public void testUnderline2FieldCamel() {
        Assertions.assertEquals("myshZzx", underline2FieldCamel("mysh_zzx"));
        Assertions.assertEquals("myshZZX", underline2FieldCamel("mysh_z_z_x"));
        Assertions.assertEquals("mysh", underline2FieldCamel("MYSH"));
        Assertions.assertEquals("mZZx", underline2FieldCamel("M_Z_ZX"));
    }

    @Test
    public void testCamel2underline() {
        Assertions.assertEquals("MYSH_ZZX", camel2underline("MyshZzx"));
        Assertions.assertEquals("MYSH_Z_Z_X", camel2underline("MyshZZX"));
        Assertions.assertEquals("M_Y_S_H_ZZX", camel2underline("MYSHZzx"));
        Assertions.assertEquals("M_YSH_Z_ZX", camel2underline("MYshZZx"));
        Assertions.assertEquals("MYSH_Z_ZX", camel2underline("myshZZx"));
        Assertions.assertEquals("MY_Z_ZX", camel2underline("myZZx"));
        Assertions.assertEquals("M_Z_ZX", camel2underline("mZZx"));
        Assertions.assertEquals("M_Z_X", camel2underline("mZX"));
        Assertions.assertEquals("MY_Z", camel2underline("myZ"));
    }

    @Test
    public void testIsUpperCase() {
        Assertions.assertTrue(isUpperCase('A'));
        Assertions.assertTrue(isUpperCase('R'));
        Assertions.assertTrue(isUpperCase('Z'));

        Assertions.assertFalse(isUpperCase('a'));
        Assertions.assertFalse(isUpperCase('k'));
        Assertions.assertFalse(isUpperCase('z'));
        Assertions.assertFalse(isUpperCase('\r'));
    }

    @Test
    public void testIsLowerCase() {

        Assertions.assertTrue(isLowerCase('a'));
        Assertions.assertTrue(isLowerCase('d'));
        Assertions.assertTrue(isLowerCase('z'));

        Assertions.assertFalse(isLowerCase('A'));
        Assertions.assertFalse(isLowerCase('R'));
        Assertions.assertFalse(isLowerCase('Z'));
        Assertions.assertFalse(isUpperCase('\r'));
    }

    @Test
    public void testToUpperCase() {
        Assertions.assertEquals('A', toUpperCase('a'));
        Assertions.assertEquals('H', toUpperCase('h'));
        Assertions.assertEquals('Z', toUpperCase('z'));
        Assertions.assertEquals('B', toUpperCase('B'));
        Assertions.assertEquals('\n', toUpperCase('\n'));
    }

    @Test
    public void testMethod2FieldSign() throws Exception {
        Assertions.assertEquals("field", method2FieldSign("Field"));
        Assertions.assertEquals("array", method2FieldSign("Array"));
        Assertions.assertEquals("zzx", method2FieldSign("Zzx"));
        Assertions.assertEquals("field", method2FieldSign("field"));
        Assertions.assertEquals("array", method2FieldSign("array"));
        Assertions.assertEquals("zzx", method2FieldSign("zzx"));
    }

    @Test
    public void testField2MethodSign() throws Exception {
        Assertions.assertEquals("Method", field2MethodSign("method"));
        Assertions.assertEquals("Array", field2MethodSign("array"));
        Assertions.assertEquals("Zzx", field2MethodSign("zzx"));
        Assertions.assertEquals("Method", field2MethodSign("Method"));
        Assertions.assertEquals("Array", field2MethodSign("Array"));
        Assertions.assertEquals("Zzx", field2MethodSign("Zzx"));
    }
}
