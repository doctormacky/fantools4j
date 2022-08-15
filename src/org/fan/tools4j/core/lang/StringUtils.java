package org.fan.tools4j.core.lang;

/**  
 * @Title: StringUtils.java
 *
 * @Description: TODO
 *
 * @author longrm
 *
 * @date 2022-08-15 02:50:46 
 */
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;

public class StringUtils {

	public static final String INTEGER_REGEX = "^-?[\\d]+$";

	public static final String DOUBLE_REGEX = "^[-\\+]?[\\d]*\\.?[\\d]+$";

	public static final String CHINESE_REGEX = "[\u0391-\uFFE5]+$";

	public static final String EMAIL_REGEX = "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";

	public static final String HTTP_URL_REGEX = "^https?://.*$";

	public static final char[] ILLEGAL_FILE_CHARS = { '\\', '/', ':', '*', '?', '"', '<', '>', '|' };

	public static final char[] REGEX_CHARS = { '.', '$', '^', '{', '[', '(', '|', ')', '*', '+', '?', '\\' };

	public static final String ALL_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

	/**
	 * 判断是否为整数
	 * @param str
	 * @return 是整数返回true,否则返回false
	 */
	public static boolean isInteger(String str) {
		return str.matches(INTEGER_REGEX);
	}

	/**
	 * 判断是否为浮点数，包括double和float
	 * @param str
	 * @return 是浮点数返回true,否则返回false
	 */
	public static boolean isDouble(String str) {
		return str.matches(DOUBLE_REGEX);
	}

	/**
	 * 判断输入的字符串是否为纯汉字
	 * @param str
	 * @return 如果是纯汉字返回true,否则返回false
	 */
	public static boolean isChinese(String str) {
		return str.matches(CHINESE_REGEX);
	}

	/**
	 * 判断输入的字符串是否符合Email样式.
	 * @param str
	 * @return 是Email样式返回true,否则返回false
	 */
	public static boolean isEmail(String str) {
		return str.matches(EMAIL_REGEX);
	}

	/**
	 * 判断输入的字符串是否是合法的网址
	 * @param str
	 * @return 是网址返回true,否则返回false
	 */
	public static boolean isHttpUrl(String str) {
		return str.matches(HTTP_URL_REGEX);
	}

	public static boolean isEmpty(CharSequence cs) {
		return cs == null || cs.length() == 0;
	}

	/**
	 * 获取网址的web站点
	 * @param httpUrl
	 * @return
	 */
	public static String getWebSite(String httpUrl) {
		if (httpUrl.startsWith("http://")) {
			httpUrl = httpUrl.substring("http://".length());
		}
		if (httpUrl.startsWith("https://")) {
			httpUrl = httpUrl.substring("https://".length());
		}

		int index = httpUrl.indexOf("/");
		if (index == -1) {
			return httpUrl;
		} else {
			return httpUrl.substring(0, index);
		}
	}

	/**
	 * 判断字符是否是非法的文件字符
	 * @param c
	 * @return
	 */
	public static boolean isIllegalFileChar(char c) {
		for (char fileChar : ILLEGAL_FILE_CHARS) {
			if (c != fileChar) {
				continue;
			}
			return true;
		}
		return false;
	}

	/**
	 * 判断字符是否是正则表达式关键字符
	 * @param c
	 * @return
	 */
	public static boolean isRegexChar(char c) {
		for (char regexChar : REGEX_CHARS) {
			if (c != regexChar) {
				continue;
			}
			return true;
		}
		return false;
	}

	/**
	 * 将字符串中的正则表达式关键字符用\转义
	 * @param str
	 * @return
	 */
	public static String escapeRegex(String str) {
		int len = str.length();
		// 查找字符串里是否含有正则表达式字符
		int i = -1;
		while (++i < len) {
			if (isRegexChar(str.charAt(i))) {
				break;
			}
		}

		// 字符串含有正则表达式字符
		if (i < len) {
			char buf[] = new char[len * 2];
			// 复制前面的字符
			for (int j = 0; j < i; j++) {
				buf[j] = str.charAt(j);
			}
			int count = 0;
			while (i < len) {
				char c = str.charAt(i);
				if (isRegexChar(c)) {
					buf[i + count] = '\\';
					count++;
				}
				buf[i + count] = c;
				i++;
			}
			return new String(buf, 0, len + count);
		}
		return str;
	}

	/**
	 * 全角转半角
	 * @param fullStr
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String fullToHalf(String fullStr) throws UnsupportedEncodingException {
		StringBuffer result = new StringBuffer("");
		for (int i = 0; i < fullStr.length(); i++) {
			String tmpStr = "" + fullStr.charAt(i);
			// 全角空格转换成半角空格
			if (tmpStr.equals("　")) {
				result.append(" ");
				continue;
			}
			// 得到 unicode 字节数据
			byte[] b = tmpStr.getBytes("unicode");
			if (b[2] == -1) {
				// 表示全角？
				b[3] = (byte) (b[3] + 32);
				b[2] = 0;
				result.append(new String(b, "unicode"));
			} else {
				result.append(tmpStr);
			}
		}
		return result.toString();
	}

	/**
	 * 半角转全角
	 * @param halfStr
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static final String halfToFull(String halfStr) throws UnsupportedEncodingException {
		StringBuffer resultSb = new StringBuffer("");
		for (int i = 0; i < halfStr.length(); i++) {
			String tmpStr = halfStr.substring(i, i + 1);
			if (tmpStr.equals(" ")) {
				// 半角空格
				resultSb.append(tmpStr);
				continue;
			}
			byte[] b = tmpStr.getBytes("unicode");
			if (b[2] == 0) {
				// 半角?
				b[3] = (byte) (b[3] - 32);
				b[2] = -1;
				resultSb.append(new String(b, "unicode"));
			} else {
				resultSb.append(tmpStr);
			}
		}
		return resultSb.toString();
	}

	/**
	 * display text correctly in xml
	 * @param str
	 * @return
	 */
	public static String xmlEncode(String str) {
		if (isEmpty(str)) {
			return str;
		}

		str = str.replaceAll("&", "&amp;");
		str = str.replaceAll("<", "&lt;");
		str = str.replaceAll(">", "&gt;");
		str = str.replaceAll("'", "&apos;");
		str = str.replaceAll("\"", "&quot;");
		return str;
	}

	/**
	 * restore text from xmlEncode()
	 * @param str
	 * @return
	 */
	public static String xmlDecode(String str) {
		if (isEmpty(str)) {
			return str;
		}

		str = str.replaceAll("&lt;", "<");
		str = str.replaceAll("&gt;", ">");
		str = str.replaceAll("&apos;", "'");
		str = str.replaceAll("&quot;", "\"");
		str = str.replaceAll("&amp;", "&");
		return str;
	}

	/**
	 * display text correctly in html
	 * @param str
	 * @return
	 */
	public static String htmlEncode(String str) {
		if (isEmpty(str)) {
			return str;
		}

		str = str.replaceAll("&", "&amp;");
		str = str.replaceAll("<", "&lt;");
		str = str.replaceAll(">", "&gt;");
		str = str.replaceAll("\"", "&quot;");
		return str;
	}

	/**
	 * restore text from htmlEncode()
	 * @param str
	 * @return
	 */
	public static String htmlDecode(String str) {
		if (isEmpty(str)) {
			return str;
		}

		str = str.replaceAll("&lt;", "<");
		str = str.replaceAll("&gt;", ">");
		str = str.replaceAll("&quot;", "\"");
		str = str.replaceAll("&amp;", "&");
		return str;
	}

	/**
	 * show text int html
	 * @param str
	 * @return
	 */
	public static String toHtmlText(String str) {
		if (isEmpty(str)) {
			return str;
		}

		str = str.replaceAll("<", "&lt;");
		str = str.replaceAll(">", "&gt;");
		str = str.replaceAll("\"", "&quot;");
		str = str.replaceAll(" ", "&nbsp;"); // 空格
		str = str.replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;"); // TAB
		str = str.replaceAll("\r\n", "<br/>"); // 回车
		str = str.replaceAll("\n", "<br/>");
		return str;
	}

	/**
	 * convert to ascii
	 * @param str
	 * @return
	 */
	public static int toAscii(String str) {
		int ascii = 0;
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			ascii = ascii + c;
		}
		return ascii;
	}

	public static String toEllipsis(String str, int length) {
		return toEllipsis(str, length, "...");
	}

	public static String toEllipsis(String str, int length, String ellipsis) {
		if (str.length() <= length) {
			return str;
		} else {
			return str.substring(0, length - ellipsis.length()) + ellipsis;
		}
	}

	public static String toFixedLength(String str, int length, char fillChar) {
		return toFixedLength(str, length, -1, fillChar);
	}

	/**
	 * get fixed length string
	 * @param str
	 * @param length
	 * @param direction <0: from front; >0: from behind;
	 * @param fillChar fill the char in the lack of bit
	 * @return
	 */
	public static String toFixedLength(String str, int length, int direction, char fillChar) {
		if (str.length() == length) {
			return str;
		}
		else if (str.length() > length) {
			if (direction < 0) {
				return str.substring(0, length);
			} else {
				return str.substring(str.length() - length);
			}
		}
		else {
			String fillStr = "";
			for (int i = 0; i < length - str.length(); i++) {
				fillStr += fillChar;
			}
			if (direction < 0) {
				return fillStr + str;
			} else {
				return str + fillStr;
			}
		}
	}

	/**
	 * get reverse string
	 * @param str
	 * @return
	 */
	public static String toReverse(String str) {
		if (isEmpty(str)) {
			return str;
		}
		
		int length = str.length();
		char buf[] = new char[length];
		for (int i = 0; i < length; i++) {
			buf[i] = str.charAt(length - 1 - i);
		}
		return new String(buf);
	}

    public static String randomString(int length) {
    	return randomString(length, null);
    }

    /**
     * generate random string
     * @param length
     * @param type null: all char; u: upper char; l: lower char; n: number char
     * @return
     */
	public static String randomString(int length, String type) {
		StringBuffer sb = new StringBuffer();
		while (sb.length() < length) {
			sb.append(randomChar(type));
		}
		return sb.toString();
	}

	/**
	 * @param type null: all char; u: upper char; l: lower char; n: number char
	 * @return
	 */
	public static char randomChar(String type) {
		int numberCount = 10;
		int letterCount = 26;

		int index = 0;
		SecureRandom random = new SecureRandom();
		char t = isEmpty(type) ? '*' : type.charAt(random.nextInt(type.length()));
		switch (t) {
		case 'u':
			index = numberCount + letterCount + random.nextInt(letterCount);
			break;
		case 'l':
			index = numberCount + random.nextInt(letterCount);
			break;
		case 'n':
			index = random.nextInt(numberCount);
			break;
		default:
			index = random.nextInt(ALL_CHARS.length());
		}
		return ALL_CHARS.charAt(index);
	}

	public static boolean isNumberChar(char c) {
		return c >= '0' && c <= '9';
	}

	public static boolean isUpperChar(char c) {
		return c >= 'a' && c <= 'z';
	}

	public static boolean isLowerChar(char c) {
		return c >= 'A' && c <= 'Z';
	}

}
