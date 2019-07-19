/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.transaction.config;

import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.NoRollbackRuleAttribute;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser
 * BeanDefinitionParser} for the {@code <tx:advice/>} tag.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @author Chris Beams
 * @since 2.0
 */
class TxAdviceBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	private static final String METHOD_ELEMENT = "method";

	private static final String METHOD_NAME_ATTRIBUTE = "name";

	private static final String ATTRIBUTES_ELEMENT = "attributes";

	private static final String TIMEOUT_ATTRIBUTE = "timeout";

	private static final String READ_ONLY_ATTRIBUTE = "read-only";

	private static final String PROPAGATION_ATTRIBUTE = "propagation";

	private static final String ISOLATION_ATTRIBUTE = "isolation";

	private static final String ROLLBACK_FOR_ATTRIBUTE = "rollback-for";

	private static final String NO_ROLLBACK_FOR_ATTRIBUTE = "no-rollback-for";

	// 这是创建的bean定义的类
	@Override
	protected Class<?> getBeanClass(Element element) {
		return TransactionInterceptor.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		// 1、从xml配置获得transactionManager，设置transactionManager属性依赖
		builder.addPropertyReference("transactionManager", TxNamespaceHandler.getTransactionManagerName(element));

		// 2、解析<tx:attributes>元素，只可有一个该元素
		List<Element> txAttributes = DomUtils.getChildElementsByTagName(element, ATTRIBUTES_ELEMENT);
		if (txAttributes.size() > 1) {
			parserContext.getReaderContext()
					.error("Element <attributes> is allowed at most once inside element <advice>", element);
		} else if (txAttributes.size() == 1) {
			// Using attributes source.
			Element attributeSourceElement = txAttributes.get(0);
			// 解析<tx:attributes>元素，获得对应的bean定义
			RootBeanDefinition attributeSourceDefinition = parseAttributeSource(attributeSourceElement, parserContext);
			// 添加transactionAttributeSource属性依赖注入到TransactionInterceptor的bean定义
			builder.addPropertyValue("transactionAttributeSource", attributeSourceDefinition);
		} else {
			// Assume annotations source.
			// 没有<tx:attributes>元素，则假定为注解来源，同样添加transactionAttributeSource属性依赖注入
			builder.addPropertyValue("transactionAttributeSource", new RootBeanDefinition(
					"org.springframework.transaction.annotation.AnnotationTransactionAttributeSource"));
		}
	}

	// 解析<tx:attributes>元素，创建NameMatchTransactionAttributeSource的bean定义
	private RootBeanDefinition parseAttributeSource(Element attrEle, ParserContext parserContext) {
		// 1、获得attributes下所有的<tx:method>元素
		List<Element> methods = DomUtils.getChildElementsByTagName(attrEle, METHOD_ELEMENT);

		// 2、存放每个<method>对应的事务定义信息的Map
		ManagedMap<TypedStringValue, RuleBasedTransactionAttribute> transactionAttributeMap = new ManagedMap<>(
				methods.size());
		transactionAttributeMap.setSource(parserContext.extractSource(attrEle));
		// 3、解析每个<tx:method>
		for (Element methodEle : methods) {
			// 3.1<method name 属性解析
			String name = methodEle.getAttribute(METHOD_NAME_ATTRIBUTE);
			TypedStringValue nameHolder = new TypedStringValue(name);
			nameHolder.setSource(parserContext.extractSource(methodEle));
			// 3.2 解析基本的事务定义信息，存放到RuleBasedTransactionAttribute中
			RuleBasedTransactionAttribute attribute = new RuleBasedTransactionAttribute();
			String propagation = methodEle.getAttribute(PROPAGATION_ATTRIBUTE);
			String isolation = methodEle.getAttribute(ISOLATION_ATTRIBUTE);
			String timeout = methodEle.getAttribute(TIMEOUT_ATTRIBUTE);
			String readOnly = methodEle.getAttribute(READ_ONLY_ATTRIBUTE);
			if (StringUtils.hasText(propagation)) {
				attribute.setPropagationBehaviorName(RuleBasedTransactionAttribute.PREFIX_PROPAGATION + propagation);
			}
			if (StringUtils.hasText(isolation)) {
				attribute.setIsolationLevelName(RuleBasedTransactionAttribute.PREFIX_ISOLATION + isolation);
			}
			if (StringUtils.hasText(timeout)) {
				try {
					attribute.setTimeout(Integer.parseInt(timeout));
				} catch (NumberFormatException ex) {
					parserContext.getReaderContext().error("Timeout must be an integer value: [" + timeout + "]",
							methodEle);
				}
			}
			if (StringUtils.hasText(readOnly)) {
				attribute.setReadOnly(Boolean.valueOf(methodEle.getAttribute(READ_ONLY_ATTRIBUTE)));
			}

			// 3.3 解析里面的rollback定义信息，存放到rollbackRules中
			List<RollbackRuleAttribute> rollbackRules = new LinkedList<>();
			if (methodEle.hasAttribute(ROLLBACK_FOR_ATTRIBUTE)) {
				String rollbackForValue = methodEle.getAttribute(ROLLBACK_FOR_ATTRIBUTE);
				addRollbackRuleAttributesTo(rollbackRules, rollbackForValue);
			}
			if (methodEle.hasAttribute(NO_ROLLBACK_FOR_ATTRIBUTE)) {
				String noRollbackForValue = methodEle.getAttribute(NO_ROLLBACK_FOR_ATTRIBUTE);
				addNoRollbackRuleAttributesTo(rollbackRules, noRollbackForValue);
			}
			// 3.4 将解析到的rollbackRules给入attribute中
			attribute.setRollbackRules(rollbackRules);
			// 3.5 将attribute放入到Map中，以nameHolder为key
			transactionAttributeMap.put(nameHolder, attribute);
		}

		// 4 创建NameMatchTransactionAttributeSource的Bean定义
		RootBeanDefinition attributeSourceDefinition = new RootBeanDefinition(
				NameMatchTransactionAttributeSource.class);
		attributeSourceDefinition.setSource(parserContext.extractSource(attrEle));
		// 添加属性依赖 transactionAttributeMap
		attributeSourceDefinition.getPropertyValues().add("nameMap", transactionAttributeMap);
		return attributeSourceDefinition;
	}

	private void addRollbackRuleAttributesTo(List<RollbackRuleAttribute> rollbackRules, String rollbackForValue) {
		String[] exceptionTypeNames = StringUtils.commaDelimitedListToStringArray(rollbackForValue);
		for (String typeName : exceptionTypeNames) {
			rollbackRules.add(new RollbackRuleAttribute(StringUtils.trimWhitespace(typeName)));
		}
	}

	private void addNoRollbackRuleAttributesTo(List<RollbackRuleAttribute> rollbackRules, String noRollbackForValue) {
		String[] exceptionTypeNames = StringUtils.commaDelimitedListToStringArray(noRollbackForValue);
		for (String typeName : exceptionTypeNames) {
			rollbackRules.add(new NoRollbackRuleAttribute(StringUtils.trimWhitespace(typeName)));
		}
	}

}
