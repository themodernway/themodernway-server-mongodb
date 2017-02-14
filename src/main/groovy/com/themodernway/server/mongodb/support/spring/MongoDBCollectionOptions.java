/*
 * Copyright (c) 2017, The Modern Way. All rights reserved.
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

import com.ait.tooling.common.api.java.util.StringOps;

public class MongoDBCollectionOptions implements IMongoDBCollectionOptions
{
    private final boolean m_icid;

    private final String  m_name;

    public MongoDBCollectionOptions(final String name, final boolean icid)
    {
        m_icid = icid;

        m_name = StringOps.requireTrimOrNull(name);
    }

    @Override
    public String getName()
    {
        return m_name;
    }

    @Override
    public boolean isCreateID()
    {
        return m_icid;
    }
}
