package com.hu.easy.enums;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class StringToDate {
	
	private static List<String> patterns = null;
	
	static {
		patterns = new ArrayList<String>();
		patterns.add("yyyy-MM-dd");
		patterns.add("yyyy/MM/dd");
		patterns.add("yyyy-MM-dd HH:mm:ss");
		patterns.add("yyyy/MM/dd HH/mm/ss");
		patterns.add("HH:mm:ss");
		patterns.add("HH/mm/ss");
	}
	
	public static Object convert(String parameterValue, Properties properties) throws Exception {
		String pattern = properties.getProperty("datePattern");
		Date date = null;
		if(!pattern.isEmpty() && pattern != null){
			try {
				date = new SimpleDateFormat(pattern).parse(parameterValue);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		} else {
			for(String pat : patterns){
				try {
					date = new SimpleDateFormat(pat).parse(parameterValue);
					break;
				} catch (ParseException e) {
					continue;
				}
			}
		}
		if(date == null){
			throw new Exception("数据类型不能从：java.lang.String 装换成   java.util.Date");
		}else{
			return date;
		}
	}

}
