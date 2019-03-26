package com.hu.easy.entity;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hu.easy.annotation.RequestParam;

public class HandlerMapping {

	private String url;

	private Object currentClass;
	
	private Method method;
	
	// key=方法的参数列表的参数的索引位置 ， value=参数名或者参数的类型【仅仅当时request和response的时候才会存参数的类型】
	private Map<Integer,String> paramsList;

	public HandlerMapping(){}
	
	public HandlerMapping(String url , Object currentClass ,  Method method){
		this.url = url;
		this.currentClass = currentClass;
		this.method = method;
		initParamsList();
	}
	
	private void initParamsList() {
		paramsList = new HashMap<Integer,String>();
		Parameter[] parms =  method.getParameters();
		//目前只支持加了@RequestParam注解的参数和HttpServletRequest，HttpServletResponse的形参列表注入值
		for(int i = 0 ; i < parms.length ; i++){
			if(parms[i].isAnnotationPresent(RequestParam.class)){//加了@RequestParam注解的参数处理
				RequestParam reqParm = parms[i].getAnnotation(RequestParam.class);
				String paramName = reqParm.value();
				if(!paramName.isEmpty() && paramName != null){
					paramsList.put(i, paramName);
				}
			}else{//HttpServletRequest，HttpServletResponse参数处理
				Class<?> type = parms[i].getType();
				if (type == HttpServletRequest.class ){
					paramsList.put(i, HttpServletRequest.class.getName());
				} else if(type == HttpServletResponse.class){
					paramsList.put(i, HttpServletResponse.class.getName());
				} else{
					paramsList.put(i, "IT-NULL");//没有处理的参数，占个位置
				}
			}
		}
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Object getCurrentClass() {
		return currentClass;
	}

	public void setCurrentClass(Object currentClass) {
		this.currentClass = currentClass;
	}

	public Map<Integer, String> getParamsList() {
		return paramsList;
	}

	public void setParamsList(Map<Integer, String> paramsList) {
		this.paramsList = paramsList;
	}
}
