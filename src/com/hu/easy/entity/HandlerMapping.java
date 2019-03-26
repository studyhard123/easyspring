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
	
	// key=�����Ĳ����б�Ĳ���������λ�� �� value=���������߲��������͡�������ʱrequest��response��ʱ��Ż����������͡�
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
		//Ŀǰֻ֧�ּ���@RequestParamע��Ĳ�����HttpServletRequest��HttpServletResponse���β��б�ע��ֵ
		for(int i = 0 ; i < parms.length ; i++){
			if(parms[i].isAnnotationPresent(RequestParam.class)){//����@RequestParamע��Ĳ�������
				RequestParam reqParm = parms[i].getAnnotation(RequestParam.class);
				String paramName = reqParm.value();
				if(!paramName.isEmpty() && paramName != null){
					paramsList.put(i, paramName);
				}
			}else{//HttpServletRequest��HttpServletResponse��������
				Class<?> type = parms[i].getType();
				if (type == HttpServletRequest.class ){
					paramsList.put(i, HttpServletRequest.class.getName());
				} else if(type == HttpServletResponse.class){
					paramsList.put(i, HttpServletResponse.class.getName());
				} else{
					paramsList.put(i, "IT-NULL");//û�д���Ĳ�����ռ��λ��
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
