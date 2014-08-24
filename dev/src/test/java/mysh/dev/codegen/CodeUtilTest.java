package mysh.dev.codegen;


import org.junit.Assert;
import org.junit.Test;

import static mysh.dev.codegen.CodeUtil.*;

/**
 * @author Mysh
 * @since 2014/4/17 16:13
 */
public class CodeUtilTest {
	@Test
	public void testUnderline2hump() {
		Assert.assertEquals("MyshZzx", underline2hump("mysh_zzx"));
		Assert.assertEquals("MyshZZX", underline2hump("mysh_z_z_x"));
		Assert.assertEquals("Mysh", underline2hump("MYSH"));
	}

	@Test
	public void testHump2underline() {
		Assert.assertEquals("MYSH_ZZX", hump2underline("MyshZzx"));
		Assert.assertEquals("MYSH_ZZX", hump2underline("MyshZZX"));
		Assert.assertEquals("MYSH_ZZX", hump2underline("MYSHZzx"));
		Assert.assertEquals("M_YSH_Z_ZX", hump2underline("MYshZZx"));
		Assert.assertEquals("MYSH_Z_ZX", hump2underline("myshZZx"));
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
