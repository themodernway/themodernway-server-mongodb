/*
 * Copyright (c) 2017, 2018, The Modern Way. All rights reserved.
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.themodernway.common.api.java.util.CommonOps;
import com.themodernway.common.api.java.util.StringOps;
import com.themodernway.common.api.types.Activatable;
import com.themodernway.server.core.support.spring.IPropertiesResolver;
import com.themodernway.server.core.support.spring.ServerContextInstance;
import com.themodernway.server.mongodb.MongoDB;

public class MongoDBDescriptor extends Activatable implements IMongoDBDescriptor
{
    private static final long                      serialVersionUID = 1L;

    private String                                 m_name;

    private MongoDB                                m_mongo_db;

    private boolean                                m_createid       = false;

    private boolean                                m_replicas       = false;

    private int                                    m_poolsize       = 100;

    private int                                    m_multiple       = 100;

    private int                                    m_ctimeout       = 10000;

    private String                                 m_defaultd;

    private MongoClientOptions                     m_coptions;

    private ArrayList<ServerAddress>               m_addrlist;

    private ArrayList<MongoCredential>             m_authlist;

    private LinkedHashMap<String, IMongoDBOptions> m_doptions;

    private String                                 m_baseprop;

    public MongoDBDescriptor()
    {
        m_baseprop = null;
    }

    public MongoDBDescriptor(final String baseprop)
    {
        m_baseprop = StringOps.requireTrimOrNull(baseprop);
    }

    @Override
    public boolean setActive(final boolean active)
    {
        if ((null == m_addrlist) && (false == init()))
        {
            return false;
        }
        return super.setActive(active);
    }

    private final boolean init()
    {
        if (null == m_baseprop)
        {
            m_baseprop = MongoDBContextInstance.getMongoDBContextInstance().getMongoDBProvider().getMongoDBDefaultPropertiesBase();
        }
        final IPropertiesResolver prop = ServerContextInstance.getServerContextInstance().getPropertiesResolver();

        setName(prop.getPropertyByName(m_baseprop + ".name"));

        setDefaultDB(prop.getPropertyByName(m_baseprop + ".db"));

        setReplicas(Boolean.valueOf(prop.getPropertyByName(m_baseprop + ".replicas", "false")));

        setCreateID(Boolean.valueOf(prop.getPropertyByName(m_baseprop + ".createid", "false")));

        final ArrayList<ServerAddress> addrlist = new ArrayList<ServerAddress>();

        for (String name : StringOps.requireTrimOrNull(prop.getPropertyByName(m_baseprop + ".host.list")).split(","))
        {
            name = StringOps.toTrimOrNull(name);

            if (null != name)
            {
                final String addr = StringOps.requireTrimOrNull(prop.getPropertyByName(m_baseprop + ".host." + name + ".addr"));

                final String port = StringOps.requireTrimOrNull(prop.getPropertyByName(m_baseprop + ".host." + name + ".port"));

                addrlist.add(new ServerAddress(addr, Integer.parseInt(port)));
            }
        }
        if (addrlist.isEmpty())
        {
            throw new IllegalArgumentException("no MongoDB server address");
        }
        m_addrlist = addrlist;

        m_authlist = new ArrayList<MongoCredential>();

        final String temp = StringOps.toTrimOrNull(prop.getPropertyByName(m_baseprop + ".auth.list"));

        if (null != temp)
        {
            for (String name : temp.split(","))
            {
                name = StringOps.toTrimOrNull(name);

                if (null != name)
                {
                    final String user = StringOps.requireTrimOrNull(prop.getPropertyByName(m_baseprop + ".auth." + name + ".user"));

                    final String pass = StringOps.requireTrimOrNull(prop.getPropertyByName(m_baseprop + ".auth." + name + ".pass"));

                    final String data = StringOps.requireTrimOrNull(prop.getPropertyByName(m_baseprop + ".auth." + name + ".db"));

                    m_authlist.add(MongoCredential.createCredential(user, data, pass.toCharArray()));
                }
            }
        }
        if (null == getClientOptions())
        {
            setClientOptions(MongoClientOptions.builder().connectionsPerHost(getConnectionPoolSize()).threadsAllowedToBlockForConnectionMultiplier(getConnectionMultiplier()).connectTimeout(getConnectionTimeout()).build());
        }
        m_doptions = new LinkedHashMap<String, IMongoDBOptions>();

        final String conf = StringOps.toTrimOrNull(prop.getPropertyByName(m_baseprop + ".dbconfig.list"));

        if (null != conf)
        {
            for (String name : conf.split(","))
            {
                name = StringOps.toTrimOrNull(name);

                if ((null != name) && (null == m_doptions.get(name)))
                {
                    boolean doid = isCreateID();

                    final ArrayList<IMongoDBCollectionOptions> list = new ArrayList<IMongoDBCollectionOptions>();

                    final String dbid = StringOps.toTrimOrNull(prop.getPropertyByName(m_baseprop + ".dbconfig." + name + ".createid"));

                    if (null != dbid)
                    {
                        doid = Boolean.valueOf(dbid);
                    }
                    final String base = m_baseprop + ".dbconfig." + name + ".collections";

                    final String cols = StringOps.toTrimOrNull(prop.getPropertyByName(base));

                    if (null != cols)
                    {
                        for (String coln : cols.split(","))
                        {
                            coln = StringOps.toTrimOrNull(coln);

                            if (null != coln)
                            {
                                final String icid = StringOps.toTrimOrNull(prop.getPropertyByName(base + "." + coln + ".createid"));

                                if (null != icid)
                                {
                                    list.add(new MongoDBCollectionOptions(coln, Boolean.valueOf(icid)));
                                }
                                else
                                {
                                    list.add(new MongoDBCollectionOptions(coln, doid));
                                }
                            }
                        }
                    }
                    m_doptions.put(name, new MongoDBOptions(name, doid, list));
                }
            }
        }
        return true;
    }

    @Override
    public boolean isCreateID()
    {
        return m_createid;
    }

    private final void setCreateID(final boolean createid)
    {
        m_createid = createid;
    }

    @Override
    public void close() throws IOException
    {
        if (null != m_mongo_db)
        {
            m_mongo_db.close();
        }
    }

    @Override
    public String getName()
    {
        return m_name;
    }

    private final void setName(final String name)
    {
        m_name = CommonOps.requireNonNull(StringOps.toTrimOrNull(name), "MongoDBDescriptor name is null or empty");
    }

    @Override
    public synchronized MongoDB getMongoDB()
    {
        if (null == m_mongo_db)
        {
            m_mongo_db = new MongoDB(getAddresses(), getCredentials(), getClientOptions(), isReplicas(), getDefaultDB(), isCreateID(), getDBOptions());
        }
        return m_mongo_db;
    }

    @Override
    public int getConnectionTimeout()
    {
        return m_ctimeout;
    }

    @Override
    public int getConnectionMultiplier()
    {
        return m_multiple;
    }

    @Override
    public int getConnectionPoolSize()
    {
        return m_poolsize;
    }

    public void setConnectionTimeout(final int timeout)
    {
        m_ctimeout = Math.max(0, timeout);
    }

    public void setConnectionMultiplier(final int multiplier)
    {
        m_multiple = Math.max(0, multiplier);
    }

    public void setConnectionPoolSize(final int poolsize)
    {
        m_poolsize = Math.max(1, poolsize);
    }

    @Override
    public String getDefaultDB()
    {
        return m_defaultd;
    }

    public void setDefaultDB(final String name)
    {
        m_defaultd = CommonOps.requireNonNull(StringOps.toTrimOrNull(name), "DefaultDB is null or empty");
    }

    @Override
    public List<MongoCredential> getCredentials()
    {
        return CommonOps.toUnmodifiableList(m_authlist);
    }

    @Override
    public boolean isReplicas()
    {
        return m_replicas;
    }

    public void setReplicas(final boolean replicas)
    {
        m_replicas = replicas;
    }

    @Override
    public List<ServerAddress> getAddresses()
    {
        return CommonOps.toUnmodifiableList(m_addrlist);
    }

    public void setClientOptions(final MongoClientOptions coptions)
    {
        m_coptions = CommonOps.requireNonNull(coptions);
    }

    @Override
    public MongoClientOptions getClientOptions()
    {
        return m_coptions;
    }

    @Override
    public Map<String, IMongoDBOptions> getDBOptions()
    {
        return CommonOps.toUnmodifiableMap(m_doptions);
    }
}
