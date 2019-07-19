package v3;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.dn.spring.aop.AdvisorAutoProxyCreator;
import com.dn.spring.aop.advisor.AspectJPointcutAdvisor;
import com.dn.spring.beans.BeanReference;
import com.dn.spring.beans.GenericBeanDefinition;
import com.dn.spring.beans.PreBuildBeanFactory;
import com.dn.spring.samples.ABean;
import com.dn.spring.samples.CBean;
import com.dn.spring.samples.aop.MyAfterReturningAdvice;
import com.dn.spring.samples.aop.MyBeforeAdvice;
import com.dn.spring.samples.aop.MyMethodInterceptor;

public class AOPTest {

	static PreBuildBeanFactory bf = new PreBuildBeanFactory();

	@Test
	public void testCirculationDI() throws Throwable {

		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(ABean.class);
		List<Object> args = new ArrayList<>();
		args.add("abean01");
		args.add(new BeanReference("cbean"));
		bd.setConstructorArgumentValues(args);
		bf.registerBeanDefinition("abean", bd);

		bd = new GenericBeanDefinition();
		bd.setBeanClass(CBean.class);
		args = new ArrayList<>();
		args.add("cbean01");
		bd.setConstructorArgumentValues(args);
		bf.registerBeanDefinition("cbean", bd);

		// 前置增强advice bean注册
		bd = new GenericBeanDefinition();
		bd.setBeanClass(MyBeforeAdvice.class);
		bf.registerBeanDefinition("myBeforeAdvice", bd);

		// 环绕增强advice bean注册
		bd = new GenericBeanDefinition();
		bd.setBeanClass(MyMethodInterceptor.class);
		bf.registerBeanDefinition("myMethodInterceptor", bd);

		// 后置增强advice bean注册
		bd = new GenericBeanDefinition();
		bd.setBeanClass(MyAfterReturningAdvice.class);
		bf.registerBeanDefinition("myAfterReturningAdvice", bd);

		// 往BeanFactory中注册AOP的BeanPostProcessor
		AdvisorAutoProxyCreator aapc = new AdvisorAutoProxyCreator();
		bf.registerBeanPostProcessor(aapc);
		// 向AdvisorAutoProxyCreator注册Advisor
		aapc.registAdvisor(
				new AspectJPointcutAdvisor("myBeforeAdvice", "execution(* com.dn.spring.samples.ABean.*(..))"));
		// 向AdvisorAutoProxyCreator注册Advisor
		aapc.registAdvisor(
				new AspectJPointcutAdvisor("myMethodInterceptor", "execution(* com.dn.spring.samples.ABean.do*(..))"));
		// 向AdvisorAutoProxyCreator注册Advisor
		aapc.registAdvisor(new AspectJPointcutAdvisor("myAfterReturningAdvice",
				"execution(* com.dn.spring.samples.ABean.do*(..))"));

		bf.preInstantiateSingletons();

		ABean abean = (ABean) bf.getBean("abean");

		abean.doSomthing();
		System.out.println("--------------------------------");
		abean.sayHello();
	}
}
