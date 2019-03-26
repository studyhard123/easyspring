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
	
	//存在所有类的完全限定名
	private List<String> classNames = new ArrayList<String>();
	
	//IOC容器 key= 类名 或者 类型  value=实例对象
	private final Map<String,Object> IOC = new HashMap<String,Object>();
	
	//保存请求地址和方法对应关系的集合
	private final List<HandlerMapping> urlMethodMapping = new ArrayList<HandlerMapping>();

	@Override
	public void init(ServletConfig config) throws ServletException {
		// 加载配置文件
		loadConfigFile(config.getInitParameter("contextConfigLocation"));
		// 扫描相关的类
		scannerClass();
		// 初始化扫描到的类，并且将它们放入到ICO容器之中
		loadClassToIOC();
		// 实现依赖注入
		autoWired();
		// 加载HandlerMapping
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
						throw new Exception("@RequestMapping注解配置错误，请以/开头，并且长度大于等于2");
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
				if(IOC.containsKey(fileNameKey)){//根据字段的名字或者@AutoWired注解的value，注入bean
					beanName = fileNameKey;
				} else {//根据类型注入bean
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
					System.out.println("不能为字段名为" + field.getName() + "的字段赋值，原因是：IOC容器中未找到相关的实例对象！");
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
				//spring 默认 类首字母小写
				String beanName = null;
				if(ObjClass.isAnnotationPresent(Controller.class)){
					beanName = toLowerFirstCase(ObjClass.getSimpleName());
					if(IOC.containsKey(beanName)){
						throw new Exception("名称为：" + beanName + " 的Bean已经存在！");
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
						throw new Exception("名称为：" + beanName + " 的Bean已经存在！");
					}
					IOC.put(beanName, serviceObj);
					//实现类型也能进行自动赋值[多肽,实现类也能直接赋值给接口]
					for(Class<?> interfaceObj : ObjClass.getInterfaces()){
						if(IOC.containsKey(interfaceObj.getName())){
							throw new Exception("名称为：" + interfaceObj.getName() + " 的Bean已经存在！");
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
	 * 把第一个字母转换成小写字母
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

	//实现请求和方法的一一对应
	private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		if(urlMethodMapping == null){
			throw new Exception("400:not found method");
		}
		//绝对路径
		String requestUrl = req.getRequestURI();
		//处理成相对路径
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
							params[indexAndName.getKey()] = null;//因此建议，方法参数列表尽量使用对象或者基本数据类型的包装类
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
	
	//数据类型的转换器
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
			throw new Exception("数据类型转换错误，不能从java.lang.String 转换成：" + paramType);
		}else if(paramType == Long.class){
			paramConvertValue = Long.parseLong(parameterValue);
		}else if(paramType == Float.class){
			paramConvertValue = Float.parseFloat(parameterValue);
		}else if(paramType == Double.class){
			paramConvertValue = Double.parseDouble(parameterValue);
		}else if(paramType == Date.class){
			paramConvertValue = StringToDate.convert(parameterValue,properties);
		}else{
			throw new Exception("数据类型转换错误，不能从java.lang.String 转换成：" + paramType);
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
