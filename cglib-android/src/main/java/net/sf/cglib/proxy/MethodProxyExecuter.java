package net.sf.cglib.proxy;

import java.lang.reflect.Method;

public class MethodProxyExecuter {
	
	@SuppressWarnings({ "rawtypes" })
	public static Object executeInterceptor(MethodInterceptor[] interceptors, Class superClass, String methodName,
			Class[] argsType, Object[] argsValue, Object object) {
		if (interceptors == null||interceptors.length==0) {
			throw new ProxyException("Did not set method interceptor !");
		}

		try {
			MethodProxy methodProxy = new MethodProxy(superClass, methodName, argsType);
			if(interceptors.length==1){
				return interceptors[0].intercept(object,methodProxy.getOriginalMethod(), argsValue, methodProxy);
			}else{
				return new RuntimeException("no support Multiple callback types possible but no filter specified");
			}
//			return interceptor.intercept(object,methodProxy.getOriginalMethod(), argsValue, methodProxy);
//			for (MethodInterceptor interceptor:interceptors ){
//				Object intercept = interceptor.intercept(object, methodProxy.getOriginalMethod(), argsValue, methodProxy);
//			}

		} catch (Exception e) {
			throw new ProxyException(e.getMessage());
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object executeMethod(Class subClass, String methodName, Class[] argsType, Object[] argsValue, Object object) {
		try {
			Method method = subClass.getDeclaredMethod(methodName + Const.SUBCLASS_INVOKE_SUPER_SUFFIX, argsType);
			return method.invoke(object, argsValue);
		} catch (Exception e) {
			throw new ProxyException(e.getMessage());
		}
	}

}
