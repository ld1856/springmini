package com.yld.spring.servlet;

import com.yld.spring.annotations.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;

public class DispatcherServlet extends HttpServlet {
    private static final String contextConfigLocation = "contextConfigLocation";

    private Properties properties = new Properties();

    private List<String> classList = new ArrayList<>();

    private Map<String, Object> iocContext = new HashMap<>();

//    private Map<String, Method> handlerMapping = new HashMap<>();

    private List<HandlerMapping> handlerMapping = new ArrayList<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        super.doGet(req, resp);

        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        super.doPost(req, resp);

        doDispather(req, resp);
    }

    private void doDispather(HttpServletRequest req, HttpServletResponse resp) {
        String url = req.getRequestURI();
        url = url.replace(req.getServletPath(), "").replaceAll("//", "/");

        HandlerMapping mapping = getHandlerMapping(url);
        Object[] params = getParams(req, resp, mapping);

        try {
            Method method = mapping.getMethod();
            Object invoke = method.invoke(mapping.getController(), params);

            if(!method.getReturnType().getName().equals("void")){
                resp.getWriter().write(String.valueOf(invoke));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException | IOException e) {
            e.printStackTrace();
        }
    }

    private Object[] getParams(HttpServletRequest req, HttpServletResponse resp, HandlerMapping mapping) {
        Class<?>[] paramType = mapping.getParamType();
        List<String> paramName = mapping.getParamName();
        Object[] params = new Object[paramType.length];
        for (int i = 0; i < paramType.length; i++) {
            if(paramType[i] == HttpServletRequest.class){
                params[i] = req;
            }else if(paramType[i] == HttpServletResponse.class){
                params[i] = resp;
            }else{
                params[i] = convertParamType(req.getParameter(paramName.get(i)), paramType[i]);
            }
        }
        return params;
    }

    private Object convertParamType(String p, Class<?> clz) {
        Object ret = null;
        if(clz == String.class){
            ret = p;
        }else if(clz == Integer.class){
            ret = Integer.parseInt(p);
        }else if(clz == Double.class){
            ret = Double.parseDouble(p);
        }
        return ret;
    }

    private HandlerMapping getHandlerMapping(String url) {
        HandlerMapping ret = null;
        for (HandlerMapping mapping : handlerMapping) {
            if(mapping.getUrl().equals(url)){
                ret = mapping;
                break;
            }
        }
        return ret;
    }


    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        //1. 初始化配置文件
        initProperties(config.getInitParameter(contextConfigLocation));

        //2. 扫描java类
        scannerClass(properties.getProperty("scannerPack"));

        //3. 初始化ioc
        initIocContext();

        //4. DI
        doAutowired();
        
        //5. 初始化handler
        initHandlerMapping();
    }

    private void initHandlerMapping() {
        if(iocContext.isEmpty()) { return; }

        for (Object value : iocContext.values()) {
            Class<?> clz = value.getClass();
            if(clz.isAnnotationPresent(Controller.class) && clz.isAnnotationPresent(RequestMapping.class)){
                RequestMapping controller = clz.getAnnotation(RequestMapping.class);
                String path = controller.value();

                for (Method method : clz.getMethods()) {
                    if (method.isAnnotationPresent(RequestMapping.class)) {
                        RequestMapping mapping = method.getAnnotation(RequestMapping.class);
                        handlerMapping.add(
                                new HandlerMapping("/" + path + "/" + mapping.value().
                                        replaceAll("//", "/"), value, method));

                        System.out.println("Mapping " + ("/" + path + "/" + mapping.value().
                                replaceAll("//", "/")));
                    }
                }

            }
        }

    }

    private void doAutowired() {
        if(iocContext.isEmpty()) { return; }

        for (Object value : iocContext.values()) {
            Field[] fields = value.getClass().getDeclaredFields();

            for (Field field : fields) {
                if(field.isAnnotationPresent(Autowrite.class)){
                    Autowrite anno = field.getAnnotation(Autowrite.class);

                    String beanName = anno.value();
                    if(isEmptyStr(beanName)){
                        Class<?> type = field.getType();
                        beanName = toLowerFirstCase(type.getSimpleName());
                        if(type.isInterface()){
                            beanName = type.getName();
                        }
                    }

                    field.setAccessible(true);
                    try {
                        field.set(value, iocContext.get(beanName));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

    }

    private void initIocContext() {

        try {
            for (String str : classList) {
                Class<?> clz = Class.forName(str);

                if(clz.isAnnotationPresent(Controller.class)){
                    Controller anno = clz.getAnnotation(Controller.class);
                    String beanName = anno.value();
                    if(isEmptyStr(beanName)){
                        beanName = toLowerFirstCase(clz.getSimpleName());
                    }
                    iocContext.put(beanName, clz.newInstance());
                }else if(clz.isAnnotationPresent(Service.class)){
                    Service anno = clz.getAnnotation(Service.class);
                    String beanName = anno.value();
                    if(isEmptyStr(beanName)){
                        beanName = toLowerFirstCase(clz.getSimpleName());
                    }
                    Object bean = clz.newInstance();
                    iocContext.put(beanName, bean);

                    //将实例按照接口类型传入，方便后续根据借口注入
                    for (Class<?> i : clz.getInterfaces()) {
                        if(iocContext.containsKey(i.getName())){
                            throw new Exception("the " + i.getName() + " is exists!");
                        }
                        iocContext.put(i.getName(), bean);
                    }

                }else{
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private boolean isEmptyStr(String str){
        return str==null || str.trim().equals("");
    }

    private void scannerClass(String scannerPack) {
        URL url = this.getClass().getClassLoader().getResource(scannerPack.replaceAll("\\.", "/"));
        File file = new File(url.getFile());
        for (File f : file.listFiles()) {
            String path = scannerPack + "." + f.getName().replace(".class", "");
            if(f.isDirectory()){
                scannerClass(path);
            }else{
                classList.add(path);
            }
        }

    }

    private void initProperties(String initParameter) {
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(initParameter)) {
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("not read contextConfigLocation file");
        }
    }

    private class HandlerMapping{
        private String url;
        private Object controller;
        private Method method;
        private Class<?>[] paramType;
        private List<String> paramName = new ArrayList<>();

        public HandlerMapping(String url, Object controller, Method method) {
            this.url = url;
            this.controller = controller;
            this.method = method;
            this.paramType = method.getParameterTypes();

            this.initParamName(paramName);
        }

        public List<String> getParamName() {
            return paramName;
        }

        private void initParamName(List<String> paramName) {
            for (Parameter param : this.method.getParameters()) {
                String name = param.getName();
                if(param.isAnnotationPresent(RequestParam.class)){
                    RequestParam anno = param.getAnnotation(RequestParam.class);
                    String val = anno.value();
                    if(!isEmptyStr(val)){
                        name = val;
                    }
                }
                this.paramName.add(name);
            }

        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Object getController() {
            return controller;
        }

        public void setController(Object controller) {
            this.controller = controller;
        }

        public Method getMethod() {
            return method;
        }

        public void setMethod(Method method) {
            this.method = method;
        }

        public Class<?>[] getParamType() {
            return paramType;
        }
    }
}
