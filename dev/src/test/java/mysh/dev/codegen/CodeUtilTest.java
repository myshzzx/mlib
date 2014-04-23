package mysh.dev.codegen;


import org.junit.Assert;
import org.junit.Test;

/**
 * @author Mysh
 * @since 2014/4/17 16:13
 */
public class CodeUtilTest {
	@Test
	public void testUnderline2hump() {
		Assert.assertEquals("MyshZzx", CodeUtil.underline2hump("mysh_zzx"));
		Assert.assertEquals("MyshZZX", CodeUtil.underline2hump("mysh_z_z_x"));
		Assert.assertEquals("Mysh", CodeUtil.underline2hump("MYSH"));
	}

	@Test
	public void testHump2underline() {
		Assert.assertEquals("MYSH_ZZX", CodeUtil.hump2underline("MyshZzx"));
		Assert.assertEquals("MYSH_ZZX", CodeUtil.hump2underline("MyshZZX"));
		Assert.assertEquals("MYSH_ZZX", CodeUtil.hump2underline("MYSHZzx"));
		Assert.assertEquals("M_YSH_Z_ZX", CodeUtil.hump2underline("MYshZZx"));
	}

	@Test
	public void testIsUpperCase() {
		Assert.assertTrue(CodeUtil.isUpperCase('A'));
		Assert.assertTrue(CodeUtil.isUpperCase('R'));
		Assert.assertTrue(CodeUtil.isUpperCase('Z'));

		Assert.assertFalse(CodeUtil.isUpperCase('a'));
		Assert.assertFalse(CodeUtil.isUpperCase('k'));
		Assert.assertFalse(CodeUtil.isUpperCase('z'));
		Assert.assertFalse(CodeUtil.isUpperCase('\r'));
	}

	@Test
	public void testIsLowerCase() {

		Assert.assertTrue(CodeUtil.isLowerCase('a'));
		Assert.assertTrue(CodeUtil.isLowerCase('d'));
		Assert.assertTrue(CodeUtil.isLowerCase('z'));

		Assert.assertFalse(CodeUtil.isLowerCase('A'));
		Assert.assertFalse(CodeUtil.isLowerCase('R'));
		Assert.assertFalse(CodeUtil.isLowerCase('Z'));
		Assert.assertFalse(CodeUtil.isUpperCase('\r'));
	}

	@Test
	public void testToUpperCase() {
		Assert.assertEquals('A', CodeUtil.toUpperCase('a'));
		Assert.assertEquals('H', CodeUtil.toUpperCase('h'));
		Assert.assertEquals('Z', CodeUtil.toUpperCase('z'));
		Assert.assertEquals('B', CodeUtil.toUpperCase('B'));
		Assert.assertEquals('\n', CodeUtil.toUpperCase('\n'));
	}
}
