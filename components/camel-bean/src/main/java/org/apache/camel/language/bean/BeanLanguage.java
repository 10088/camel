/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.language.bean;

import java.util.Map;

import org.apache.camel.Expression;
import org.apache.camel.Predicate;
import org.apache.camel.support.ExpressionToPredicateAdapter;
import org.apache.camel.support.LanguageSupport;
import org.apache.camel.util.StringHelper;

/**
 * A <a href="http://camel.apache.org/bean-language.html">bean language</a> which uses a simple text notation to invoke
 * methods on beans to evaluate predicates or expressions
 * <p/>
 * The notation is essentially <code>beanName.methodName</code> which is then invoked using the beanName to lookup in
 * the <a href="http://camel.apache.org/registry.html>registry</a> then the method is invoked to evaluate the expression
 * using the <a href="http://camel.apache.org/bean-integration.html">bean integration</a> to bind the
 * {@link org.apache.camel.Exchange} to the method arguments.
 * <p/>
 * As of Camel 1.5 the bean language also supports invoking a provided bean by its classname or the bean itself.
 */
@org.apache.camel.spi.annotations.Language("bean")
public class BeanLanguage extends LanguageSupport {

    public BeanLanguage() {
    }

    @Override
    public Predicate createPredicate(String expression) {
        return ExpressionToPredicateAdapter.toPredicate(createExpression(expression));
    }

    @Override
    public Predicate createPredicate(Map<String, Object> properties) {
        return ExpressionToPredicateAdapter.toPredicate(createExpression(properties));
    }

    @Override
    public Expression createExpression(Map<String, Object> properties) {
        Object bean = properties.get("bean");
        Class<?> beanType = (Class<?>) properties.get("beanType");
        String ref = (String) properties.get("ref");
        String method = (String) properties.get("method");

        if (bean != null) {
            return new BeanExpression(bean, method);
        } else if (beanType != null) {
            return new BeanExpression(beanType, method);
        } else if (ref != null) {
            return new BeanExpression(ref, method);
        } else {
            throw new IllegalArgumentException("Bean language requires bean, beanType, or ref argument");
        }
    }

    @Override
    public Expression createExpression(String expression) {
        // favour using the configured options
        String beanName = expression;
        String method = null;

        // we support both the .method name and the ?method= syntax
        // as the ?method= syntax is very common for the bean component
        if (expression.contains("?method=")) {
            beanName = StringHelper.before(expression, "?");
            method = StringHelper.after(expression, "?method=");
        } else {
            //first check case :: because of my.own.Bean::method
            int doubleColonIndex = expression.indexOf("::");
            //need to check that not inside params
            int beginOfParameterDeclaration = expression.indexOf('(');
            if (doubleColonIndex > 0 && (!expression.contains("(") || doubleColonIndex < beginOfParameterDeclaration)) {
                beanName = expression.substring(0, doubleColonIndex);
                method = expression.substring(doubleColonIndex + 2);
            } else {
                int idx = expression.indexOf('.');
                if (idx > 0) {
                    beanName = expression.substring(0, idx);
                    method = expression.substring(idx + 1);
                }
            }
        }

        return new BeanExpression(beanName, method);
    }

}
