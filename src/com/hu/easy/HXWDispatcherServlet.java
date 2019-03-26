package com.hu.easy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hu.easy.annotation.AutoWired;
import com.hu.easy.annotation.Controller;
import com.hu.easy.annotation.RequestMapping;
import com.hu.easy.annotation.Service;
import com.hu.easy.entity.HandlerMapping;
import com.hu.easy.enums.StringToDate;

public class HXWDispatcherServlet extends HttpServlet {

	private static final long serialVersionUID = -19680426927734905L;
	
	private final Properties properties = new Properties();
	
	//�������������ȫ�޶���
	private List<String> classNames = new ArrayList<String>();
	
	//IOC���� key= ���� ���� ����  value=ʵ������
	private final Map<String,Object> IOC = new HashMap<String,Object>();
	
	//���������ַ�ͷ�����Ӧ��ϵ�ļ���
	private final List<HandlerMapping> urlMethodMapping = new ArrayList<HandlerMapping>();

	@Override
	public void init(ServletConfig config) throws ServletException {
		// ���������ļ�
		loadConfigFile(config.getInitParameter("contextConfigLocation"));
		// ɨ����ص���
		scannerClass();
		// ��ʼ��ɨ�赽���࣬���ҽ����Ƿ��뵽ICO����֮��
		loadClassToIOC();
		// ʵ������ע��
		autoWired();
		// ����HandlerMapping
		loadHandlerMapping();
	}
	
	private void loadHandlerMapping() {
		if(IOC.isEmpty()){
			return;
		}
		for(String key : IOC.keySet()){
			Object bean = IOC.get(key);
			Class<?> beanClass = bean.getClass();
			StringBuilder urlStr = new StringBuilder();
			if(beanClass.isAnnotationPresent(RequestMapping.class)){
				urlStr.append(beanClass.getAnnotation(RequestMapping.class).value());
			}
			Method[] methods = beanClass.getMethods();
			for(Method method : methods){
				if(!method.isAnnotationPresent(RequestMapping.class)){
					continue;
				}
				String methodUrl = method.getAnnotation(RequestMapping.class).value();
				if(methodUrl.startsWith("/") && methodUrl.trim().length() >= 2 ){
					urlStr.append(methodUrl);
					HandlerMapping handler = new HandlerMapping(urlStr.toString(), bean , method);
					urlMethodMapping.add(handler);
				}else{
					try {
						throw new Exception("@RequestMappingע�����ô�������/��ͷ�����ҳ��ȴ��ڵ���2");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void autoWired() {
		if(IOC.isEmpty()){
			return;
		}
		for(String key : IOC.keySet()){
			Object bean = IOC.get(key);
			Field[] fields = bean.getClass().getDeclaredFields();
			for(Field field : fields){
				if(!field.isAnnotationPresent(AutoWired.class)){
					continue;
				}
				String fileNameKey = field.getName();
				String autoWiredValue = field.getAnnotation(AutoWired.class).value();
				if(!autoWiredValue.isEmpty()){
					fileNameKey = autoWiredValue;
				}
				String beanName = null;
				if(IOC.containsKey(fileNameKey)){//�����ֶε����ֻ���@AutoWiredע���value��ע��bean
					beanName = fileNameKey;
				} else {//��������ע��bean
					Class<?> type = field.getType();
					String typeName = type.getName();
					if(IOC.containsKey(typeName)){
						beanName = typeName;
					}
				}
				if( beanName != null ){
					field.setAccessible(true);
					try {
						field.set(bean, IOC.get(beanName));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}else{
					System.out.println("����Ϊ�ֶ���Ϊ" + field.getName() + "���ֶθ�ֵ��ԭ���ǣ�IOC������δ�ҵ���ص�ʵ������");
				}
			}
		}
	}

	private void scannerClass() {
		String packages = properties.getProperty("basePackage");
		for(String basePackage : packages.split(",")){
			loadClassName(basePackage.replaceAll("\\.", "/"));
		}
	}

	private void loadClassToIOC() {
		for(String className : classNames){
			try {
				Class<?> ObjClass = Class.forName(className);
				if(!ObjClass.isAnnotationPresent(Controller.class) && !ObjClass.isAnnotationPresent(Service.class)){
					continue;
				}
				//spring Ĭ�� ������ĸСд
				String beanName = null;
				if(ObjClass.isAnnotationPresent(Controller.class)){
					beanName = toLowerFirstCase(ObjClass.getSimpleName());
					if(IOC.containsKey(beanName)){
						throw new Exception("����Ϊ��" + beanName + " ��Bean�Ѿ����ڣ�");
					}
					IOC.put(beanName, ObjClass.newInstance());
				} else if(ObjClass.isAnnotationPresent(Service.class)) {
					Service service = ObjClass.getAnnotation(Service.class);
					beanName = service.value();
					Object serviceObj = ObjClass.newInstance();
					if(beanName.isEmpty()){
						beanName = toLowerFirstCase(ObjClass.getSimpleName());
					}
					if(IOC.containsKey(beanName)){
						throw new Exception("����Ϊ��" + beanName + " ��Bean�Ѿ����ڣ�");
					}
					IOC.put(beanName, serviceObj);
					//ʵ������Ҳ�ܽ����Զ���ֵ[����,ʵ����Ҳ��ֱ�Ӹ�ֵ���ӿ�]
					for(Class<?> interfaceObj : ObjClass.getInterfaces()){
						if(IOC.containsKey(interfaceObj.getName())){
							throw new Exception("����Ϊ��" + interfaceObj.getName() + " ��Bean�Ѿ����ڣ�");
						}
						IOC.put(interfaceObj.getName(), serviceObj);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * �ѵ�һ����ĸת����Сд��ĸ
	 */
	private String toLowerFirstCase(String simpleName) {
		char[] charArray = simpleName.toCharArray();
		if(charArray[0] >= 'a' && charArray[0] <= 'z'){
			return simpleName;
		}
		charArray[0] += 32;
		return String.valueOf(charArray);
	}

	private void loadConfigFile(String congfigFile) {
		InputStream configFileStram = null;
		try {
			configFileStram = this.getClass().getClassLoader().getResourceAsStream(congfigFile);
			properties.load(configFileStram);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(configFileStram != null){
				try {
					configFileStram.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}

	private void loadClassName(String basePackage) {
		if(basePackage.isEmpty() || basePackage == null){
			return;
		}
		URL url = this.getClass().getClassLoader().getResource("/" + basePackage);
		File directoryOrFile = new File(url.getFile());
		File[] files = directoryOrFile.listFiles();
		for(File file : files){
			if(file.isDirectory()){
				basePackage += "/" + file.getName();
				loadClassName(basePackage);
			}else{
				classNames.add(basePackage.replaceAll("/", "\\.") + "." + file.getName().replace(".class", ""));
			}
		}
	}

	//ʵ������ͷ�����һһ��Ӧ
	private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		if(urlMethodMapping == null){
			throw new Exception("400:not found method");
		}
		//����·��
		String requestUrl = req.getRequestURI();
		//��������·��
		String contextPath = req.getContextPath();
		String url = requestUrl.replaceAll(contextPath, ""); 
		for(HandlerMapping handler : urlMethodMapping){
			try {
				if(handler.getUrl().equals(url)){
					Method method = handler.getMethod();
					Object[] params = new Object[method.getParameters().length];
					Set<Entry<Integer, String>> indexAndNames = handler.getParamsList().entrySet();
					for(Entry<Integer, String> indexAndName : indexAndNames){
						if(indexAndName.getValue().equals(HttpServletRequest.class.getName())){
							params[indexAndName.getKey()] = req;
						} else if(indexAndName.getValue().equals(HttpServletResponse.class.getName())){
							params[indexAndName.getKey()] = resp;
						} else if(indexAndName.getValue().equals("IT-NULL")){
							params[indexAndName.getKey()] = null;//��˽��飬���������б���ʹ�ö�����߻����������͵İ�װ��
						} else{
							params[indexAndName.getKey()] = converter(method.getParameters()[indexAndName.getKey()],req.getParameter(indexAndName.getValue()));
						}
					}
					Object returnValue = method.invoke(handler.getCurrentClass(), params);
					if(returnValue == null || returnValue instanceof Void ){
						return;
					}
					resp.getWriter().write(String.valueOf(returnValue));
				}
			} catch(Exception e) {
				throw e;
			}
		}
	}
	
	//�������͵�ת����
	private Object converter(Parameter parameter, String parameterValue) throws Exception {
		Class<?> paramType = parameter.getType();
		Object paramConvertValue = null;
		if(paramType == String.class){
			paramConvertValue = parameterValue;
		}else if(paramType == Integer.class){
			paramConvertValue = Integer.parseInt(parameterValue);
		}else if(paramType == Boolean.class){
			paramConvertValue = Boolean.parseBoolean(parameterValue);
		}else if(paramType == Byte.class){
			paramConvertValue = Byte.parseByte(parameterValue);
		}else if(paramType == Short.class){
			paramConvertValue = Integer.parseInt(parameterValue);
		}else if(paramType == Character.class){
			throw new Exception("��������ת�����󣬲��ܴ�java.lang.String ת���ɣ�" + paramType);
		}else if(paramType == Long.class){
			paramConvertValue = Long.parseLong(parameterValue);
		}else if(paramType == Float.class){
			paramConvertValue = Float.parseFloat(parameterValue);
		}else if(paramType == Double.class){
			paramConvertValue = Double.parseDouble(parameterValue);
		}else if(paramType == Date.class){
			paramConvertValue = StringToDate.convert(parameterValue,properties);
		}else{
			throw new Exception("��������ת�����󣬲��ܴ�java.lang.String ת���ɣ�" + paramType);
		}
			return paramConvertValue;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			doDispatcher(req,resp);
		} catch (Exception e) {
			e.printStackTrace();
			resp.getWriter().write("500 Exception ," + Arrays.toString(e.getStackTrace()));
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}
}
