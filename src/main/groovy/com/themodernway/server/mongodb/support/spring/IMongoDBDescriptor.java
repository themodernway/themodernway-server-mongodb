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

import java.io.Closeable;
import java.util.List;
import java.util.Map;

import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.themodernway.common.api.types.IActivatable;
import com.themodernway.server.mongodb.MongoDB;

public interface IMongoDBDescriptor extends IMongoDBConfigurationBase, IActivatable, Closeable
{
    public MongoDB getMongoDB();

    public int getConnectionTimeout();

    public int getConnectionMultiplier();

    public int getConnectionPoolSize();

    public String getDefaultDB();

    public boolean isReplicas();

    public List<MongoCredential> getCredentials();

    public List<ServerAddress> getAddresses();

    public MongoClientOptions getClientOptions();

    public Map<String, IMongoDBOptions> getDBOptions();
}
