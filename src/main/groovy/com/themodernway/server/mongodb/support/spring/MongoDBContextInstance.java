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

import java.util.Objects;

import com.themodernway.server.core.support.spring.ServerContextInstance;

public class MongoDBContextInstance extends ServerContextInstance implements IMongoDBContext
{
    private static final MongoDBContextInstance INSTANCE = new MongoDBContextInstance();

    public static final MongoDBContextInstance getMongoDBContextInstance()
    {
        return INSTANCE;
    }

    protected MongoDBContextInstance()
    {
    }

    @Override
    public final IMongoDBProvider getMongoDBProvider()
    {
        return Objects.requireNonNull(getBeanSafely("MongoDBProvider", IMongoDBProvider.class), "MongoDBProvider is null, initialization error.");
    }
}
