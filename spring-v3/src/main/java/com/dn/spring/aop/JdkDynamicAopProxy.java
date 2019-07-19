package com.dn.spring.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.dn.spring.aop.advisor.Advisor;
import com.dn.spring.beans.BeanFactory;

public class JdkDynamicAopProxy implements AopProxy, InvocationHandler {
	private static final Log logger = LogFactory.getLog(JdkDynamicAopProxy.class);

	private String beanName;
	private Object target;
	private List<Advisor> matchAdvisors;

	private BeanFactory beanFactory;

	public JdkDynamicAopProxy(String beanName, Object target, List<Advisor> matchAdvisors, BeanFactory beanFactory) {
		super();
		this.beanName = beanName;
		this.target = target;
		this.matchAdvisors = matchAdvisors;
		this.beanFactory = beanFactory;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		return AopProxyUtils.applyAdvices(target, method, args, matchAdvisors, proxy, beanFactory);
	}

	@Override
	public Object getProxy() {
		return this.getProxy(target.getClass().getClassLoader());
	}

	@Override
	public Object getProxy(ClassLoader classLoader) {
		if (logger.isDebugEnabled()) {
			logger.debug("为" + target + "创建代理。");
		}
		return Proxy.newProxyInstance(classLoader, target.getClass().getInterfaces(), this);
	}

}
