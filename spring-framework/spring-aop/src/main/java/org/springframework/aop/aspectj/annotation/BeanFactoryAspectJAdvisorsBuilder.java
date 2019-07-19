/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.aspectj.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.reflect.PerClauseKind;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Helper for retrieving @AspectJ beans from a BeanFactory and building Spring
 * Advisors based on them, for use with auto-proxying.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see AnnotationAwareAspectJAutoProxyCreator
 */
public class BeanFactoryAspectJAdvisorsBuilder {

	private final ListableBeanFactory beanFactory;

	private final AspectJAdvisorFactory advisorFactory;

	@Nullable
	private volatile List<String> aspectBeanNames;

	private final Map<String, List<Advisor>> advisorsCache = new ConcurrentHashMap<>();

	private final Map<String, MetadataAwareAspectInstanceFactory> aspectFactoryCache = new ConcurrentHashMap<>();

	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 * 
	 * @param beanFactory
	 *            the ListableBeanFactory to scan
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory) {
		this(beanFactory, new ReflectiveAspectJAdvisorFactory(beanFactory));
	}

	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 * 
	 * @param beanFactory
	 *            the ListableBeanFactory to scan
	 * @param advisorFactory
	 *            the AspectJAdvisorFactory to build each Advisor with
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {
		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
		Assert.notNull(advisorFactory, "AspectJAdvisorFactory must not be null");
		this.beanFactory = beanFactory;
		this.advisorFactory = advisorFactory;
	}

	/**
	 * Look for AspectJ-annotated aspect beans in the current bean factory, and
	 * return to a list of Spring AOP Advisors representing them.
	 * <p>
	 * Creates a Spring Advisor for each AspectJ advice method.
	 * 
	 * @return the list of {@link org.springframework.aop.Advisor} beans
	 * @see #isEligibleBean
	 */
	/**
	 * 方法的逻辑：<br>
	 * 1、看有没有缓存下带有@Ascpect注解的Bean的名字：aspectBeanNames。<br>
	 * 2、没有这个缓存的名字，表示这是第一来创建AspectJAdvisors,则获取BeanFactory中所有的beanNames；<br>
	 * 遍历beanNames，获取每个beanName对应的Class,看它上面有没有@Aspect注解，有则提前它里面的注解advice,创建Advisor;
	 * <br>
	 * 并缓存下创建的Advisor,缓存记录BeanName，返回Advisor <br>
	 * 3、有这个缓存表示已经创建过了，则直接从缓存中获取缓存的Advisor,返回。
	 */
	public List<Advisor> buildAspectJAdvisors() {
		// 将缓存的aspectBeanNames集合赋给aspectNames本地变量
		List<String> aspectNames = this.aspectBeanNames;

		if (aspectNames == null) { // 为空，则表示还没创建过，则来创建Advisors
			synchronized (this) { // 同步控制，保证只创建一次
				aspectNames = this.aspectBeanNames;
				if (aspectNames == null) { // 只做一次的双重检查
					List<Advisor> advisors = new ArrayList<>();
					aspectNames = new ArrayList<>();
					// 获取所有的BeanNames
					String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.beanFactory,
							Object.class, true, false);
					// 变量BeanNames, 处理每个BeanName的class,看有没有@Aspect注解
					for (String beanName : beanNames) {
						if (!isEligibleBean(beanName)) {
							continue;
						}
						// We must be careful not to instantiate beans eagerly
						// as in this case they
						// would be cached by the Spring container but would not
						// have been weaved.
						Class<?> beanType = this.beanFactory.getType(beanName); // 获得class
						if (beanType == null) {
							continue;
						}
						if (this.advisorFactory.isAspect(beanType)) { // 判断类上是否有@Aspect注解
							aspectNames.add(beanName); // 加入到aspectNames集合中，最后会将集合赋值给this.aspectBeanNames
							AspectMetadata amd = new AspectMetadata(beanType, beanName);

							// 这里是判断该单例方式还是原型方式来创建Aspect中定义的Advisor。判断的依据是用户在@Aspect注解上指定的值，默认是单例
							if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
								MetadataAwareAspectInstanceFactory factory = new BeanFactoryAspectInstanceFactory(
										this.beanFactory, beanName);
								// 怎么从注解创建Advisor的，请进入this.advisorFactory.getAdvisors(factory)看
								List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);
								// 如果Bean本身是单例的，则缓存创建的Advisors，Advisor就是单例方式;
								// 如果Bean不是单例的，则缓存的是创建Advisor的factory,advisor也就不是单例的。
								if (this.beanFactory.isSingleton(beanName)) {
									this.advisorsCache.put(beanName, classAdvisors);
								} else {
									this.aspectFactoryCache.put(beanName, factory);
								}
								advisors.addAll(classAdvisors);
							} else {
								// Per target or per this.
								// //用户指定了Advisor不是单例方式创建，但Bean确是单例的，则抛出异常！
								if (this.beanFactory.isSingleton(beanName)) {
									throw new IllegalArgumentException("Bean with name '" + beanName
											+ "' is a singleton, but aspect instantiation model is not singleton");
								}
								// 原型方式的创建工厂
								MetadataAwareAspectInstanceFactory factory = new PrototypeAspectInstanceFactory(
										this.beanFactory, beanName);
								this.aspectFactoryCache.put(beanName, factory); // 缓存的是创建工厂
								advisors.addAll(this.advisorFactory.getAdvisors(factory));
							}
						}
					}
					this.aspectBeanNames = aspectNames; // 将aspectNames集合赋值给this.aspectBeanNames,缓存起来。
					return advisors;
				}
			}
		}

		if (aspectNames.isEmpty()) {
			return Collections.emptyList();
		}

		// 从缓存中取Advisors
		List<Advisor> advisors = new ArrayList<>();
		for (String aspectName : aspectNames) {
			List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
			if (cachedAdvisors != null) {
				advisors.addAll(cachedAdvisors);
			} else {
				MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
				advisors.addAll(this.advisorFactory.getAdvisors(factory));
			}
		}
		return advisors;
	}

	/**
	 * Return whether the aspect bean with the given name is eligible.
	 * 
	 * @param beanName
	 *            the name of the aspect bean
	 * @return whether the bean is eligible
	 */
	protected boolean isEligibleBean(String beanName) {
		return true;
	}

}
