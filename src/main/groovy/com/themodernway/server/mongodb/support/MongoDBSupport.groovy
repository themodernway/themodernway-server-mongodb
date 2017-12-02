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

package com.themodernway.server.mongodb.support

import java.util.regex.Pattern

import com.themodernway.common.api.java.util.StringOps
import com.themodernway.server.core.json.JSONArray
import com.themodernway.server.core.json.JSONObject
import com.themodernway.server.core.support.CoreGroovySupport
import com.themodernway.server.mongodb.MongoDB
import com.themodernway.server.mongodb.MongoDB.IMCursor
import com.themodernway.server.mongodb.MongoDB.MAggregationGroup
import com.themodernway.server.mongodb.MongoDB.MAggregationMatch
import com.themodernway.server.mongodb.MongoDB.MCollection
import com.themodernway.server.mongodb.MongoDB.MCollectionPreferences
import com.themodernway.server.mongodb.MongoDB.MDatabase
import com.themodernway.server.mongodb.MongoDB.MProjection
import com.themodernway.server.mongodb.MongoDB.MQuery
import com.themodernway.server.mongodb.MongoDB.MSort
import com.themodernway.server.mongodb.support.spring.IMongoDBContext
import com.themodernway.server.mongodb.support.spring.IMongoDBProvider
import com.themodernway.server.mongodb.support.spring.MongoDBContextInstance

import groovy.transform.CompileStatic
import groovy.transform.Memoized

@CompileStatic
public class MongoDBSupport extends CoreGroovySupport
{
    private static final MongoDBSupport INSTANCE = new MongoDBSupport()

    @Memoized
    public static final MongoDBSupport getMongoDBSupport()
    {
        INSTANCE
    }

    public MongoDBSupport()
    {
    }

    @Memoized
    public IMongoDBContext getMongoDBContext()
    {
        MongoDBContextInstance.getMongoDBContextInstance()
    }

    @Memoized
    public IMongoDBProvider getMongoDBProvider()
    {
        getMongoDBContext().getMongoDBProvider()
    }

    @Memoized
    public MCollection collection(String name) throws Exception
    {
        db().collection(StringOps.requireTrimOrNull(name))
    }

    public MCollection collection(String name, MCollectionPreferences opts) throws Exception
    {
        db().collection(StringOps.requireTrimOrNull(name), opts)
    }

    @Memoized
    public MDatabase db(String name) throws Exception
    {
        getMongoDB().db(StringOps.requireTrimOrNull(name))
    }

    @Memoized
    public MDatabase db() throws Exception
    {
        getMongoDB().db()
    }

    @Memoized
    public MongoDB getMongoDB()
    {
        getMongoDB(getMongoDBDefaultDescriptorName())
    }

    @Memoized
    public MongoDB getMongoDB(String name)
    {
        getMongoDBProvider().getMongoDBDescriptor(StringOps.requireTrimOrNull(name)).getMongoDB()
    }

    @Memoized
    public String getMongoDBDefaultDescriptorName()
    {
        getMongoDBProvider().getMongoDBDefaultDescriptorName()
    }

    public JSONArray jarr(IMCursor cursor)
    {
        new JSONArray(cursor.into([]))
    }

    public JSONObject json(IMCursor cursor)
    {
        new JSONObject(cursor.into([]))
    }

    public JSONObject json(String name, IMCursor cursor)
    {
        new JSONObject(name, cursor.into([]))
    }

    public Map INC(Map args)
    {
        ['$inc': args]
    }

    public Map MUL(Map args)
    {
        ['$mul': args]
    }

    public Map RENAME(Map args)
    {
        ['$rename': args]
    }

    public Map SET(Map args)
    {
        ['$set': args]
    }

    public Map UNSET(Map args)
    {
        ['$unset': args]
    }

    public Map MIN(Map args)
    {
        ['$min': args]
    }

    public Map MAX(Map args)
    {
        ['$max': args]
    }

    public MSort SORTS(Map map)
    {
        new MSort(map)
    }

    public MSort ASCENDING(String... fields)
    {
        MSort.ASCENDING(fields)
    }

    public MSort ASCENDING(List<String> fields)
    {
        MSort.ASCENDING(fields)
    }

    public MSort DESCENDING(String... fields)
    {
        MSort.DESCENDING(fields)
    }

    public MSort DESCENDING(List<String> fields)
    {
        MSort.DESCENDING(fields)
    }

    public MSort ORDER_BY(MSort... sorts)
    {
        MSort.ORDER_BY(sorts)
    }

    public MSort ORDER_BY(List<MSort> sorts)
    {
        MSort.ORDER_BY(sorts)
    }

    public MProjection INCLUDE(String... fields)
    {
        MProjection.INCLUDE(fields)
    }

    public MProjection INCLUDE(List<String> fields)
    {
        MProjection.INCLUDE(fields)
    }

    public MProjection EXCLUDE(String... fields)
    {
        MProjection.EXCLUDE(fields)
    }

    public MProjection EXCLUDE(List<String> fields)
    {
        MProjection.EXCLUDE(fields)
    }

    @Memoized
    public MProjection NO_ID()
    {
        MProjection.NO_ID()
    }

    public MProjection FIELDS(MProjection... projections)
    {
        MProjection.FIELDS(projections)
    }

    public MProjection FIELDS(List<MProjection> projections)
    {
        MProjection.FIELDS(projections)
    }

    public MQuery QUERY(Map map)
    {
        MQuery.QUERY(map)
    }

    public MQuery QUERY(MQuery... queries)
    {
        MQuery.AND(queries)
    }

    public MQuery QUERY(List<MQuery> queries)
    {
        MQuery.AND(queries)
    }

    public <T> MQuery EQ(String name, T value)
    {
        MQuery.EQ(name, value)
    }

    public <T> MQuery NE(String name, T value)
    {
        MQuery.NE(name, value)
    }

    public <T> MQuery GT(String name, T value)
    {
        MQuery.GT(name, value)
    }

    public <T> MQuery LT(String name, T value)
    {
        MQuery.LT(name, value)
    }

    public <T> MQuery GTE(String name, T value)
    {
        MQuery.GTE(name, value)
    }

    public <T> MQuery LTE(String name, T value)
    {
        MQuery.LTE(name, value)
    }

    @SuppressWarnings("unchecked")
    public <T> MQuery IN(String name, T... list)
    {
        MQuery.IN(name, list)
    }

    public <T> MQuery IN(String name, List<T> list)
    {
        MQuery.IN(name, list)
    }

    @SuppressWarnings("unchecked")
    public <T> MQuery NIN(String name, T... list)
    {
        MQuery.NIN(name, list)
    }

    public <T> MQuery NIN(String name, List<T> list)
    {
        MQuery.NIN(name, list)
    }

    public MQuery NOT(MQuery query)
    {
        MQuery.NOT(query)
    }

    public MQuery AND(MQuery... queries)
    {
        MQuery.AND(queries)
    }

    public MQuery AND(List<MQuery> queries)
    {
        MQuery.AND(queries)
    }

    public MQuery OR(MQuery... queries)
    {
        MQuery.OR(queries)
    }

    public MQuery OR(List<MQuery> queries)
    {
        MQuery.OR(queries)
    }

    public MQuery NOR(MQuery... queries)
    {
        MQuery.NOR(queries)
    }

    public MQuery NOR(List<MQuery> queries)
    {
        MQuery.NOR(queries)
    }

    public MQuery EXISTS(String name, boolean exists)
    {
        MQuery.EXISTS(name, exists)
    }

    public MQuery EXISTS(String name)
    {
        MQuery.EXISTS(name)
    }

    public MQuery REGEX(String name, String pattern)
    {
        MQuery.REGEX(name, pattern)
    }

    public MQuery REGEX(String name, Pattern pattern)
    {
        MQuery.REGEX(name, pattern)
    }

    public MAggregationMatch MATCH(Map map)
    {
        new MAggregationMatch(map)
    }

    public MAggregationGroup GROUP(Map map)
    {
        new MAggregationGroup(map)
    }
}
