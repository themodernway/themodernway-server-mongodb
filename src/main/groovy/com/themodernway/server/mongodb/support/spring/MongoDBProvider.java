/*
 * Copyright (c) 2018, The Modern Way. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.themodernway.server.mongodb.support.spring;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import com.themodernway.common.api.java.util.CommonOps;
import com.themodernway.common.api.java.util.StringOps;
import com.themodernway.server.core.io.IO;
import com.themodernway.server.core.logging.LoggingOps;

public class MongoDBProvider implements BeanFactoryAware, IMongoDBProvider
{
    private static final Logger                             logger        = LoggingOps.LOGGER(MongoDBProvider.class);

    private final String                                    m_default_name;

    private final String                                    m_default_base;

    private final LinkedHashMap<String, IMongoDBDescriptor> m_descriptors = new LinkedHashMap<>();

    public MongoDBProvider(final String default_base, final String default_name)
    {
        m_default_base = StringOps.requireTrimOrNull(default_base);

        m_default_name = StringOps.requireTrimOrNull(default_name);
    }

    @Override
    public String getMongoDBDefaultPropertiesBase()
    {
        return m_default_base;
    }

    @Override
    public String getMongoDBDefaultDescriptorName()
    {
        return m_default_name;
    }

    @Override
    public IMongoDBDescriptor getMongoDBDescriptor(final String name)
    {
        return m_descriptors.get(StringOps.requireTrimOrNull(name));
    }

    @Override
    public List<String> getMongoDBDescriptorNames()
    {
        return CommonOps.toUnmodifiableList(m_descriptors.keySet());
    }

    @Override
    public List<IMongoDBDescriptor> getMongoDBDescriptors()
    {
        return CommonOps.toUnmodifiableList(m_descriptors.values());
    }

    @Override
    public void close() throws IOException
    {
        IO.close(m_descriptors.values());
    }

    @Override
    public void setBeanFactory(final BeanFactory factory) throws BeansException
    {
        if (factory instanceof DefaultListableBeanFactory)
        {
            for (final IMongoDBDescriptor descriptor : ((DefaultListableBeanFactory) factory).getBeansOfType(IMongoDBDescriptor.class).values())
            {
                descriptor.setActive(true);

                final String name = StringOps.requireTrimOrNull(descriptor.getName());

                if (null == m_descriptors.get(name))
                {
                    if (logger.isInfoEnabled())
                    {
                        logger.info(String.format("Adding IMongoDBDescriptor(%s) class (%s)", name, descriptor.getClass().getName()));
                    }
                    m_descriptors.put(name, descriptor);
                }
                else if (logger.isErrorEnabled())
                {
                    logger.error(String.format("Duplicate IMongoDBDescriptor(%s) class (%s)", name, descriptor.getClass().getName()));
                }
            }
        }
    }
}
