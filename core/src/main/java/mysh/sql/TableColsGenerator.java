package mysh.sql;

import mysh.codegen.CodeUtil;
import org.reflections.Reflections;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * TableColsGenerator
 *
 * @author mysh
 * @since 2016/1/19
 */
public class TableColsGenerator {
	public static void gen(String packageName, String dstDir, String dstPackage, String className) throws ClassNotFoundException, IOException {
		try (PrintWriter out = new PrintWriter(
						Files.newOutputStream(Paths.get(dstDir, dstPackage.replace('.', '/'), className + ".java")))) {
			out.println("package " + dstPackage + ";");
			out.println();
			out.println("public abstract class " + className + " {");
			for (Class clazz : new Reflections(packageName).getSubTypesOf(Serializable.class)) {
				String colClassName = clazz.getSimpleName() + "Cols";
				out.println("\tpublic static class " + colClassName + " {");
				Field[] fields = clazz.getDeclaredFields();
				for (Field field : fields) {
					String fieldName = field.getName();
					if ("serialVersionUID".equals(fieldName)) continue;
					out.println("\t\tpublic final String " + fieldName + " = \"" + CodeUtil.camel2underline(fieldName) + "\";");
				}
				out.println("\t}");
				out.println("\tpublic static final " + colClassName + " " + colClassName + " = new " + colClassName + "();");
				out.println();
			}
			out.println("}");
		}
	}

}
