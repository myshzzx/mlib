package mysh.codegen;


import org.junit.Assert;
import org.junit.Test;

import static mysh.codegen.CodeUtil.*;


/**
 * @author Mysh
 * @since 2014/4/17 16:13
 */
public class CodeUtilTest {
	@Test
	public void testUnderline2camel() {
		Assert.assertEquals("MyshZzx", underline2camel("mysh_zzx"));
		Assert.assertEquals("MyshZZX", underline2camel("mysh_z_z_x"));
		Assert.assertEquals("Mysh", underline2camel("MYSH"));
		Assert.assertEquals("MZZx", underline2camel("M_Z_ZX"));
	}

	@Test
	public void testUnderline2FieldCamel() {
		Assert.assertEquals("myshZzx", underline2FieldCamel("mysh_zzx"));
		Assert.assertEquals("myshZZX", underline2FieldCamel("mysh_z_z_x"));
		Assert.assertEquals("mysh", underline2FieldCamel("MYSH"));
		Assert.assertEquals("mZZx", underline2FieldCamel("M_Z_ZX"));
	}

	@Test
	public void testCamel2underline() {
		Assert.assertEquals("MYSH_ZZX", camel2underline("MyshZzx"));
		Assert.assertEquals("MYSH_Z_Z_X", camel2underline("MyshZZX"));
		Assert.assertEquals("M_Y_S_H_ZZX", camel2underline("MYSHZzx"));
		Assert.assertEquals("M_YSH_Z_ZX", camel2underline("MYshZZx"));
		Assert.assertEquals("MYSH_Z_ZX", camel2underline("myshZZx"));
		Assert.assertEquals("MY_Z_ZX", camel2underline("myZZx"));
		Assert.assertEquals("M_Z_ZX", camel2underline("mZZx"));
		Assert.assertEquals("M_Z_X", camel2underline("mZX"));
		Assert.assertEquals("MY_Z", camel2underline("myZ"));
	}

	@Test
	public void testIsUpperCase() {
		Assert.assertTrue(isUpperCase('A'));
		Assert.assertTrue(isUpperCase('R'));
		Assert.assertTrue(isUpperCase('Z'));

		Assert.assertFalse(isUpperCase('a'));
		Assert.assertFalse(isUpperCase('k'));
		Assert.assertFalse(isUpperCase('z'));
		Assert.assertFalse(isUpperCase('\r'));
	}

	@Test
	public void testIsLowerCase() {

		Assert.assertTrue(isLowerCase('a'));
		Assert.assertTrue(isLowerCase('d'));
		Assert.assertTrue(isLowerCase('z'));

		Assert.assertFalse(isLowerCase('A'));
		Assert.assertFalse(isLowerCase('R'));
		Assert.assertFalse(isLowerCase('Z'));
		Assert.assertFalse(isUpperCase('\r'));
	}

	@Test
	public void testToUpperCase() {
		Assert.assertEquals('A', toUpperCase('a'));
		Assert.assertEquals('H', toUpperCase('h'));
		Assert.assertEquals('Z', toUpperCase('z'));
		Assert.assertEquals('B', toUpperCase('B'));
		Assert.assertEquals('\n', toUpperCase('\n'));
	}

	@Test
	public void testMethod2FieldSign() throws Exception {
		Assert.assertEquals("field", method2FieldSign("Field"));
		Assert.assertEquals("array", method2FieldSign("Array"));
		Assert.assertEquals("zzx", method2FieldSign("Zzx"));
		Assert.assertEquals("field", method2FieldSign("field"));
		Assert.assertEquals("array", method2FieldSign("array"));
		Assert.assertEquals("zzx", method2FieldSign("zzx"));
	}

	@Test
	public void testField2MethodSign() throws Exception {
		Assert.assertEquals("Method", field2MethodSign("method"));
		Assert.assertEquals("Array", field2MethodSign("array"));
		Assert.assertEquals("Zzx", field2MethodSign("zzx"));
		Assert.assertEquals("Method", field2MethodSign("Method"));
		Assert.assertEquals("Array", field2MethodSign("Array"));
		Assert.assertEquals("Zzx", field2MethodSign("Zzx"));
	}
}
