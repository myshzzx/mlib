package mysh.codegen;

import mysh.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Mysh
 * @since 2014/4/17 16:11
 */
public class CodeUtil {
    private static final Logger log = LoggerFactory.getLogger(CodeUtil.class);

    private static final int alphaStep = 'a' - 'A';


    /**
     * underline to camel , e.g. MYSH_ZZX-> MyshZzx or myshZzx, depends on param <code>startLowerCase</code>
     */
    public static String underline2camel(String underline, boolean startLowerCase) {
        if (Strings.isBlank(underline)) return underline;
        StringBuilder hump = new StringBuilder();

        char[] chars = underline.trim().toLowerCase().toCharArray();
        boolean needUpperCase = !startLowerCase;
        for (char c : chars) {
            if (c == '_') {
                needUpperCase = true;
                continue;
            }
            hump.append((char) (c - (needUpperCase ? alphaStep : 0)));
            needUpperCase = false;
        }
        return hump.toString();
    }

    /**
     * underline to camel , e.g. MYSH_ZZX-> MyshZzx
     */
    public static String underline2camel(String underline) {
        return underline2camel(underline, false);
    }

    /**
     * underline to field camel , e.g. MYSH_ZZX-> myshZzx
     */
    public static String underline2FieldCamel(String underline) {
        return underline2camel(underline, true);
    }

    /**
     * 驼峰命名转为下划线命名.
     */
    public static String camel2underline(String hump) {
        StringBuilder underline = new StringBuilder();
        char[] chars = hump.trim().toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (i == 0)
                underline.append(toUpperCase(c));
            else {
                if (isUpperCase(c)) {
                    underline.append('_');
                    underline.append(c);
                } else
                    underline.append(toUpperCase(c));
            }
        }

        return underline.toString();
    }

    public static boolean isUpperCase(char c) {
        return c >= 'A' && c <= 'Z';
    }

    public static boolean isLowerCase(char c) {
        return c >= 'a' && c <= 'z';
    }

    public static char toUpperCase(char c) {
        return isLowerCase(c) ? (char) (c - alphaStep) : c;
    }

    /**
     * 方法签名转域签名: Method->method
     */
    public static String method2FieldSign(String mSign) {
        if (mSign == null) return null;
        else if (mSign.length() == 0) return "";
        else if (mSign.charAt(0) > 'Z') return mSign;
        else {
            char[] chars = mSign.toCharArray();
            chars[0] = (char) (chars[0] + alphaStep);
            return new String(chars);
        }
    }

    /**
     * 域签名转方法签名: field->Field
     */
    public static String field2MethodSign(String fSign) {
        if (fSign == null) return null;
        else if (fSign.length() == 0) return "";
        else if (fSign.charAt(0) < 'a') return fSign;
        else {
            char[] chars = fSign.toCharArray();
            chars[0] = (char) (chars[0] - alphaStep);
            return new String(chars);
        }
    }

    private static final Pattern fieldsExp = Pattern.compile("(/\\*\\*.+?\\*/).*?private (.+?);", Pattern.DOTALL);

    /**
     * @param className  类名, 用于 set 方法的返回类型.
     * @param fieldsCode 属性定义. 完整部分, 含注释, 需要先用 ide 格式化好.
     * @return
     */
    public static String genGetSetByFields(String className, String fieldsCode) {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = fieldsExp.matcher(fieldsCode);
        while (matcher.find()) {
            String comment = matcher.group(1);
            String fieldDefine = matcher.group(2);
            int sep = fieldDefine.lastIndexOf(' ');
            String type = fieldDefine.substring(0, sep + 1);
            String getMethodPrefix = type.toLowerCase().equals("boolean ") ? "is" : "get";
            String field = fieldDefine.substring(sep + 1);
            String fieldMethod = field2MethodSign(field);

            sb
                    .append(comment).append('\n')
                    .append("public ").append(type).append(getMethodPrefix).append(fieldMethod).append("() {").append('\n')
                    .append('\t').append("return this.").append(field).append(";\n}\n\n")

                    .append(comment).append('\n')
                    .append("public ").append(className).append(" set").append(fieldMethod).append('(')
                    .append(type).append(field)
                    .append(") {").append('\n')
                    .append("\tthis.").append(field).append(" = ").append(field).append(";\n")
                    .append('\t').append("return this;\n}\n\n")
            ;
        }
        return sb.toString();
    }

    /**
     * 属性复制代码
     *
     * @param dstVar     目标变量名
     * @param srcVar     源变量名
     * @param fieldsCode 属性定义. 完整部分, 含注释, 需要先用 ide 格式化好.
     * @return
     */
    public static String genPropCopy(String dstVar, String srcVar, String fieldsCode) {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = fieldsExp.matcher(fieldsCode);
        while (matcher.find()) {
            String comment = matcher.group(1).replace("/**", "").replace("*/", "").trim()
                    .replaceAll("((\r)?\n)*\\s*?\\*\\s*", "//");
            if (!comment.startsWith("//")) comment = "//" + comment;
            String fieldDefine = matcher.group(2);
            int sep = fieldDefine.lastIndexOf(' ');
            String type = fieldDefine.substring(0, sep + 1);
            String getMethodPrefix = type.toLowerCase().equals("boolean ") ? "is" : "get";
            String field = fieldDefine.substring(sep + 1);
            String fieldMethod = field2MethodSign(field);

            sb
                    .append(comment).append('\n')
                    .append(dstVar).append(".set").append(fieldMethod).append('(')
                    .append(srcVar).append(".").append(getMethodPrefix).append(fieldMethod).append("());\n")
            ;
        }
        return sb.toString();
    }
}
