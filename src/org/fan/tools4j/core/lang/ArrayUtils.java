package org.fan.tools4j.core.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**  
 * @Title: ArrayUtils.java
 *
 * @Description: TODO
 *
 * @author Macky liuyunsh@cn.ibm.com
 *
 * @Copyright: 2022-2099 IBM All rights reserved.
 *
 * @date 2022-08-15 03:01:30 
 */
public class ArrayUtils {
	
	public static List<String> deepCopy(List<String> original){
		return original.stream().collect(Collectors.toCollection(ArrayList::new));
	}

}
