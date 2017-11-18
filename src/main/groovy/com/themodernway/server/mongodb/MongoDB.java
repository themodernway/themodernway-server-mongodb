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

package com.themodernway.server.mongodb;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Filters.nin;
import static com.mongodb.client.model.Filters.nor;
import static com.mongodb.client.model.Filters.not;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Filters.regex;
import static com.themodernway.server.mongodb.IMongoConstants.DOCUMENT;
import static com.themodernway.server.mongodb.IMongoConstants.ENSUREID;
import static com.themodernway.server.mongodb.IMongoConstants.INCLUDE_N;
import static com.themodernway.server.mongodb.IMongoConstants.INCLUDE_Y;
import static com.themodernway.server.mongodb.IMongoConstants.MONGODB_ID_KEY;
import static com.themodernway.server.mongodb.IMongoConstants.ORDER_A;
import static com.themodernway.server.mongodb.IMongoConstants.ORDER_D;
import static com.themodernway.server.mongodb.IMongoConstants.UPSERT_OPTIONS_TRUE;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.bson.BSON;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.IndexOptions;
import com.themodernway.common.api.java.util.CommonOps;
import com.themodernway.common.api.java.util.StringOps;
import com.themodernway.common.api.types.INamed;
import com.themodernway.server.core.ICoreCommon;
import com.themodernway.server.core.json.JSONUtils;
import com.themodernway.server.core.logging.IHasLogging;
import com.themodernway.server.mongodb.support.spring.IMongoDBCollectionOptions;
import com.themodernway.server.mongodb.support.spring.IMongoDBOptions;

public final class MongoDB implements ICoreCommon
{
    private static final Logger                m_logger = Logger.getLogger(MongoDB.class);

    private final MongoClient                  m_mongo;

    private final String                       m_usedb;

    private final boolean                      m_useid;

    private final Map<String, IMongoDBOptions> m_dbops;

    public MongoDB(final List<ServerAddress> addr, final List<MongoCredential> auth, final MongoClientOptions opts, final boolean repl, final String usedb, final boolean useid, final Map<String, IMongoDBOptions> dbops)
    {
        m_useid = useid;

        m_dbops = requireNonNull(dbops);

        m_usedb = requireTrimOrNull(usedb);

        BSON.addEncodingHook(BigDecimal.class, object -> {

            if (null == object)
            {
                return null;
            }
            return JSONUtils.asDouble(object);
        });
        BSON.addEncodingHook(BigInteger.class, object -> {

            if (null == object)
            {
                return null;
            }
            final Long lval = JSONUtils.asLong(object);

            if (null != lval)
            {
                return lval;
            }
            return JSONUtils.asInteger(object);
        });
        if (addr.isEmpty())
        {
            throw new IllegalArgumentException("no ServerAddress");
        }
        if ((addr.size() == 1) && (false == repl))
        {
            final ServerAddress main = addr.get(0);

            if (null == main)
            {
                throw new IllegalArgumentException("null ServerAddress");
            }
            if ((null == auth) || (auth.isEmpty()))
            {
                m_mongo = new MongoClient(main, requireNonNull(opts));
            }
            else
            {
                m_mongo = new MongoClient(main, auth, requireNonNull(opts));
            }
        }
        else
        {
            if ((null == auth) || (auth.isEmpty()))
            {
                m_mongo = new MongoClient(addr, requireNonNull(opts));
            }
            else
            {
                m_mongo = new MongoClient(addr, auth, requireNonNull(opts));
            }
        }
    }

    @Override
    public Logger logger()
    {
        return m_logger;
    }

    public boolean isAddingID()
    {
        return m_useid;
    }

    public void close()
    {
        if (null != m_mongo)
        {
            m_mongo.close();
        }
    }

    public List<String> getDatabaseNames()
    {
        return toUnmodifiableList(toUnique(m_mongo.listDatabaseNames().into(arrayList())));
    }

    public final MDatabase db() throws Exception
    {
        return db(m_usedb);
    }

    public final MDatabase db(final String name) throws Exception
    {
        return db(name, isAddingID());
    }

    public final MDatabase db(String name, boolean id) throws Exception
    {
        name = requireTrimOrNull(name);

        final IMongoDBOptions op = m_dbops.get(name);

        if (null != op)
        {
            id = op.isCreateID();
        }
        return new MDatabase(m_mongo.getDatabase(name), id, op);
    }

    public static final class MDatabase implements ICoreCommon, INamed
    {
        private final MongoDatabase   m_db;

        private final IMongoDBOptions m_op;

        private final boolean         m_id;

        private static final Logger   m_logger = Logger.getLogger(MDatabase.class);

        protected MDatabase(final MongoDatabase db, final boolean id, final IMongoDBOptions op) throws Exception
        {
            m_id = id;

            m_op = op;

            m_db = requireNonNull(db);
        }

        @Override
        public Logger logger()
        {
            return m_logger;
        }

        public boolean isCreateID()
        {
            return m_id;
        }

        @Override
        public final String getName()
        {
            return m_db.getName();
        }

        public final void drop()
        {
            m_db.drop();
        }

        public final boolean isCollection(final String name)
        {
            return getCollectionNames().contains(requireTrimOrNull(name));
        }

        public final List<String> getCollectionNames()
        {
            return toUnmodifiableList(toUnique(m_db.listCollectionNames().into(arrayList())));
        }

        public final MCollection collection(String name) throws Exception
        {
            name = requireTrimOrNull(name);

            if (null != m_op)
            {
                final IMongoDBCollectionOptions cops = m_op.getCollectionOptions(name);

                if (null != cops)
                {
                    return new MCollection(m_db.getCollection(name), cops.isCreateID());
                }
            }
            return new MCollection(m_db.getCollection(name), isCreateID());
        }

        public final MCollection collection(String name, final MCollectionPreferences opts) throws Exception
        {
            name = requireTrimOrNull(name);

            boolean crid = isCreateID();

            if (null != m_op)
            {
                final IMongoDBCollectionOptions cops = m_op.getCollectionOptions(name);

                if (null != cops)
                {
                    crid = cops.isCreateID();
                }
                if ((null != opts) && (opts.isValid()))
                {
                    return opts.withCollectionOptions(m_db.getCollection(name), crid);
                }
            }
            return new MCollection(m_db.getCollection(name), crid);
        }
    }

    public static final class MCollectionPreferences implements IHasLogging
    {
        private final WriteConcern   m_write;

        private final ReadPreference m_prefs;

        private final CodecRegistry  m_codec;

        private static final Logger  m_logger = Logger.getLogger(MCollectionPreferences.class);

        public MCollectionPreferences(final WriteConcern write, final ReadPreference prefs, final CodecRegistry codec)
        {
            m_write = write;

            m_prefs = prefs;

            m_codec = codec;
        }

        public MCollectionPreferences(final WriteConcern write)
        {
            this(write, null, null);
        }

        public MCollectionPreferences(final ReadPreference prefs)
        {
            this(null, prefs, null);
        }

        public MCollectionPreferences(final CodecRegistry codec)
        {
            this(null, null, codec);
        }

        public MCollectionPreferences(final WriteConcern write, final ReadPreference prefs)
        {
            this(write, prefs, null);
        }

        public MCollectionPreferences(final WriteConcern write, final CodecRegistry codec)
        {
            this(write, null, codec);
        }

        public MCollectionPreferences(final ReadPreference prefs, final CodecRegistry codec)
        {
            this(null, prefs, codec);
        }

        final boolean isValid()
        {
            return (false == ((null == m_write) && (null == m_prefs) && (null == m_codec)));
        }

        final MCollection withCollectionOptions(final MongoCollection<Document> collection, final boolean id)
        {
            return new MCollection(withCodecRegistry(withReadPreference(withWriteConcern(collection, m_write), m_prefs), m_codec), id);
        }

        private final static MongoCollection<Document> withWriteConcern(final MongoCollection<Document> collection, final WriteConcern write)
        {
            if (null == write)
            {
                return collection;
            }
            return collection.withWriteConcern(write);
        }

        private final static MongoCollection<Document> withReadPreference(final MongoCollection<Document> collection, final ReadPreference prefs)
        {
            if (null == prefs)
            {
                return collection;
            }
            return collection.withReadPreference(prefs);
        }

        private final static MongoCollection<Document> withCodecRegistry(final MongoCollection<Document> collection, final CodecRegistry codec)
        {
            if (null == codec)
            {
                return collection;
            }
            return collection.withCodecRegistry(codec);
        }

        @Override
        public Logger logger()
        {
            return m_logger;
        }
    }

    public static final class MCollection implements ICoreCommon, INamed
    {
        private final MongoCollection<Document> m_collection;

        private final boolean                   m_id;

        private static final Logger             m_logger = Logger.getLogger(MCollection.class);

        protected MCollection(final MongoCollection<Document> collection, final boolean id)
        {
            m_collection = requireNonNull(collection);

            m_id = id;
        }

        @Override
        public Logger logger()
        {
            return m_logger;
        }

        public boolean isCreateID()
        {
            return m_id;
        }

        @Override
        public final String getName()
        {
            return m_collection.getNamespace().getCollectionName();
        }

        public final String createIndex(final Map<String, ?> keys)
        {
            return m_collection.createIndex(DOCUMENT(keys));
        }

        public final String createIndex(final Map<String, ?> keys, final String name)
        {
            return m_collection.createIndex(DOCUMENT(keys), new IndexOptions().name(requireNonNull(name)));
        }

        public final String createIndex(final Map<String, ?> keys, final IndexOptions opts)
        {
            return m_collection.createIndex(DOCUMENT(keys), requireNonNull(opts));
        }

        public final MCollection dropIndex(final String name)
        {
            m_collection.dropIndex(requireNonNull(name));

            return this;
        }

        public final MCollection dropIndexes()
        {
            m_collection.dropIndexes();

            return this;
        }

        public final MIndexCursor getIndexes()
        {
            return new MIndexCursor(m_collection.listIndexes());
        }

        @SafeVarargs
        public final <T extends Document> MAggregateCursor aggregate(final T... list)
        {
            return aggregate(new MAggregationPipeline(requireNonNull(list)));
        }

        public final <T extends Document> MAggregateCursor aggregate(final List<T> list)
        {
            return aggregate(new MAggregationPipeline(requireNonNull(list)));
        }

        public final MAggregateCursor aggregate(final MAggregationPipeline pipeline)
        {
            return new MAggregateCursor(m_collection.aggregate(requireNonNull(pipeline.pipeline())));
        }

        public final void drop()
        {
            m_collection.drop();
        }

        public final long deleteMany(final Map<String, ?> query)
        {
            return deleteMany(new MQuery(query));
        }

        public final long deleteMany(final MQuery query)
        {
            return m_collection.deleteMany(requireNonNull(query)).getDeletedCount();
        }

        public final boolean deleteOne(final Map<String, ?> query)
        {
            return deleteOne(new MQuery(query));
        }

        public final boolean deleteOne(final MQuery query)
        {
            return (m_collection.deleteOne(requireNonNull(query)).getDeletedCount() == 1L);
        }

        public final Map<String, ?> insertOne(final Map<String, ?> record)
        {
            if (isCreateID())
            {
                final Map<String, ?> withid = ENSUREID(requireNonNull(record));

                m_collection.insertOne(DOCUMENT(withid));

                return withid;
            }
            else
            {
                m_collection.insertOne(DOCUMENT(record));

                return record;
            }
        }

        public final MCollection insertMany(final List<Map<String, ?>> list)
        {
            if (list.isEmpty())
            {
                logger().warn("MCollection.insertMany(empty)");

                return this;
            }
            if (1 == list.size())
            {
                insertOne(requireNonNull(list.get(0))); // let this do checkID

                return this;
            }
            final List<Document> save = arrayList();

            if (isCreateID())
            {
                for (final Map<String, ?> lmap : list)
                {
                    save.add(DOCUMENT(ENSUREID(lmap)));
                }
            }
            else
            {
                for (final Map<String, ?> lmap : list)
                {
                    save.add(DOCUMENT(ENSUREID(lmap)));
                }
            }
            m_collection.insertMany(save);

            return this;
        }

        public final String getNameSpace()
        {
            return m_collection.getNamespace().toString();
        }

        public final long count()
        {
            return m_collection.count();
        }

        public final long count(final Map<String, ?> query)
        {
            return count(new MQuery(query));
        }

        public final long count(final MQuery query)
        {
            return m_collection.count(requireNonNull(query));
        }

        public final MCursor find() throws Exception
        {
            return find(false);
        }

        public final MCursor find(final boolean with_id) throws Exception
        {
            if (with_id)
            {
                return new MCursor(m_collection.find());
            }
            else
            {
                return new MCursor(m_collection.find().projection(MProjection.NO_ID()));
            }
        }

        public final MCursor find(final Map<String, ?> query) throws Exception
        {
            return find(query, false);
        }

        public final MCursor find(final MQuery query) throws Exception
        {
            return find(query, false);
        }

        public final MCursor find(final Map<String, ?> query, final boolean with_id) throws Exception
        {
            return find(new MQuery(query), with_id);
        }

        public final MCursor find(final MQuery query, final boolean with_id) throws Exception
        {
            if (with_id)
            {
                return new MCursor(m_collection.find(requireNonNull(query)));
            }
            else
            {
                return new MCursor(m_collection.find(requireNonNull(query)).projection(MProjection.NO_ID()));
            }
        }

        public final MCursor find(final Map<String, ?> query, final Map<String, ?> fields) throws Exception
        {
            return find(new MQuery(query), new MProjection(fields));
        }

        public final MCursor find(final MQuery query, final Map<String, ?> fields) throws Exception
        {
            return find(query, new MProjection(fields));
        }

        public final MCursor find(final Map<String, ?> query, final MProjection fields) throws Exception
        {
            return find(new MQuery(query), requireNonNull(fields), false);
        }

        public final MCursor find(final MQuery query, final MProjection fields) throws Exception
        {
            return find(query, requireNonNull(fields), false);
        }

        public final MCursor find(final Map<String, ?> query, final Map<String, ?> fields, final boolean with_id) throws Exception
        {
            return find(new MQuery(query), new MProjection(fields), with_id);
        }

        public final MCursor find(final Map<String, ?> query, final MProjection fields, final boolean with_id) throws Exception
        {
            return find(new MQuery(query), requireNonNull(fields), with_id);
        }

        public final MCursor find(final MQuery query, final MProjection fields, final boolean with_id) throws Exception
        {
            if (with_id)
            {
                return new MCursor(m_collection.find(requireNonNull(query)).projection(requireNonNull(fields)));
            }
            else
            {
                return new MCursor(m_collection.find(requireNonNull(query)).projection(MProjection.FIELDS(requireNonNull(fields), MProjection.NO_ID())));
            }
        }

        public final Map<String, ?> findAndModify(final Map<String, ?> query, final Map<String, ?> update)
        {
            return update(new MQuery(query), update, false, true);
        }

        public final Map<String, ?> findAndModify(final MQuery query, final Map<String, ?> update)
        {
            return update(query, update, false, true);
        }

        public final Map<String, ?> upsert(final Map<String, ?> query, final Map<String, ?> update)
        {
            return update(new MQuery(query), update, true, false);
        }

        public final Map<String, ?> upsert(final Map<String, ?> query, final Map<String, ?> update, final boolean multi)
        {
            return update(new MQuery(query), update, true, multi);
        }

        public final Map<String, ?> upsert(final MQuery query, final Map<String, ?> update)
        {
            return update(query, update, true, false);
        }

        public final Map<String, ?> upsert(final MQuery query, final Map<String, ?> update, final boolean multi)
        {
            return update(query, update, true, multi);
        }

        public final Map<String, ?> update(final Map<String, ?> query, final Map<String, ?> update, final boolean upsert, final boolean multi)
        {
            return update(new MQuery(query), update, upsert, multi);
        }

        public final Map<String, ?> update(final MQuery query, final Map<String, ?> update, final boolean upsert, final boolean multi)
        {
            if (multi)
            {
                if (upsert)
                {
                    m_collection.updateMany(requireNonNull(query), DOCUMENT(update), UPSERT_OPTIONS_TRUE);
                }
                else
                {
                    m_collection.updateMany(requireNonNull(query), DOCUMENT(update));
                }
            }
            else
            {
                if (upsert)
                {
                    m_collection.updateOne(requireNonNull(query), DOCUMENT(update), UPSERT_OPTIONS_TRUE);
                }
                else
                {
                    m_collection.updateOne(requireNonNull(query), DOCUMENT(update));
                }
            }
            return update;
        }

        public final Map<String, ?> create(final Map<String, ?> record)
        {
            return insertOne(requireNonNull(record));
        }

        public final Map<String, ?> findOne(final Map<String, ?> query)
        {
            return findOne(new MQuery(query));
        }

        public final Map<String, ?> findOne(final MQuery query)
        {
            final FindIterable<Document> iter = m_collection.find(requireNonNull(query)).limit(1).projection(MProjection.NO_ID());

            if (null != iter)
            {
                return iter.first();
            }
            return null;
        }

        public final Optional<Map<String, ?>> findOneOptional(final Map<String, ?> query)
        {
            return CommonOps.toOptional(findOne(new MQuery(query)));
        }

        public final Optional<Map<String, ?>> findOneOptional(final MQuery query)
        {
            return CommonOps.toOptional(findOne(query));
        }

        public final boolean updateOne(final Map<String, ?> query, final Map<String, ?> update)
        {
            return updateOne(new MQuery(query), update);
        }

        public final boolean updateOne(final MQuery query, final Map<String, ?> update)
        {
            return (m_collection.updateOne(requireNonNull(query), DOCUMENT(update)).getModifiedCount() == 1L);
        }

        public final long updateMany(final Map<String, ?> query, final Map<String, ?> update)
        {
            return updateMany(new MQuery(query), update);
        }

        public final long updateMany(final MQuery query, final Map<String, ?> update)
        {
            return m_collection.updateMany(requireNonNull(query), DOCUMENT(update)).getModifiedCount();
        }

        public final List<?> distinct(final String field)
        {
            return m_collection.distinct(requireTrimOrNull(field), Document.class).into(arrayList());
        }

        public final List<?> distinct(final String field, final Map<String, ?> query)
        {
            return m_collection.distinct(requireTrimOrNull(field), Document.class).filter(DOCUMENT(query)).into(arrayList());
        }
    }

    private static class MAggregationOp extends Document
    {
        private static final long serialVersionUID = 1L;

        private MAggregationOp(final Document doc)
        {
            super(CommonOps.requireNonNull(doc));
        }

        protected static final MAggregationOp makeAggregationOp(final String op, final Map<String, ?> map)
        {
            final LinkedHashMap<String, Object> make = new LinkedHashMap<String, Object>(1);

            make.put(CommonOps.requireNonNull(op), CommonOps.requireNonNull(map));

            return new MAggregationOp(new Document(make));
        }

        protected static final MAggregationOp makeAggregationOp(final String op, final Document doc)
        {
            final LinkedHashMap<String, Object> make = new LinkedHashMap<String, Object>(1);

            make.put(CommonOps.requireNonNull(op), CommonOps.requireNonNull(doc));

            return new MAggregationOp(new Document(make));
        }

        public static final MAggregationMatch MATCH(final Map<String, ?> map)
        {
            return new MAggregationMatch(CommonOps.requireNonNull(map));
        }

        public static final MAggregationMatch MATCH(final Document doc)
        {
            return new MAggregationMatch(CommonOps.requireNonNull(doc));
        }

        public static final MAggregationGroup GROUP(final Map<String, ?> map)
        {
            return new MAggregationGroup(CommonOps.requireNonNull(map));
        }

        public static final MAggregationGroup GROUP(final Document doc)
        {
            return new MAggregationGroup(CommonOps.requireNonNull(doc));
        }
    }

    public static final class MAggregationGroup extends MAggregationOp
    {
        private static final long serialVersionUID = 1L;

        public MAggregationGroup(final Map<String, ?> map)
        {
            super(makeAggregationOp("$group", CommonOps.requireNonNull(map)));
        }

        public MAggregationGroup(final Document doc)
        {
            super(makeAggregationOp("$group", CommonOps.requireNonNull(doc)));
        }
    }

    public static final class MAggregationMatch extends MAggregationOp
    {
        private static final long serialVersionUID = 1L;

        public MAggregationMatch(final Map<String, ?> map)
        {
            super(makeAggregationOp("$match", CommonOps.requireNonNull(map)));
        }

        public MAggregationMatch(final Document doc)
        {
            super(makeAggregationOp("$match", CommonOps.requireNonNull(doc)));
        }
    }

    public static final class MAggregationPipeline
    {
        private final List<Document> m_pipeline;

        public <T extends Document> MAggregationPipeline(final List<T> list)
        {
            m_pipeline = CommonOps.toUnmodifiableList(new ArrayList<Document>(CommonOps.requireNonNull(list)));
        }

        @SafeVarargs
        public <T extends Document> MAggregationPipeline(final T... list)
        {
            this(CommonOps.toList(CommonOps.requireNonNull(list)));
        }

        public List<Document> pipeline()
        {
            return m_pipeline;
        }
    }

    public static interface IMCursor extends Iterable<Map<String, ?>>, Iterator<Map<String, ?>>, Closeable, IHasLogging
    {
        public <A extends Collection<? super Map<String, ?>>> A into(A target);
    }

    protected static abstract class AbstractMCursor<T extends MongoIterable<Document>> implements IMCursor
    {
        private final T                     m_iterab;

        private final MongoCursor<Document> m_cursor;

        private final Logger                m_logger    = Logger.getLogger(getClass());

        private boolean                     m_closed    = false;

        private boolean                     m_autoclose = true;

        protected AbstractMCursor(final T iter)
        {
            m_iterab = CommonOps.requireNonNull(iter);

            m_cursor = CommonOps.requireNonNull(m_iterab.iterator());
        }

        @Override
        public Logger logger()
        {
            return m_logger;
        }

        protected final T self()
        {
            return m_iterab;
        }

        @Override
        public <A extends Collection<? super Map<String, ?>>> A into(final A target)
        {
            final A result = m_iterab.into(target);

            try
            {
                close();
            }
            catch (final IOException e)
            {
                logger().error("Error in AbstractMCursor.into() ", e);
            }
            return result;
        }

        @Override
        public Iterator<Map<String, ?>> iterator()
        {
            return this;
        }

        @Override
        public boolean hasNext()
        {
            final boolean next = ((m_closed == false) && (m_cursor.hasNext()));

            if ((false == next) && (false == m_closed) && (m_autoclose))
            {
                try
                {
                    close();
                }
                catch (final Exception e)
                {
                    logger().error("Error in AbstractMCursor.close() ", e);
                }
            }
            return next;
        }

        public void setAutoClose(final boolean autoclose)
        {
            m_autoclose = autoclose;
        }

        @Override
        public Map<String, ?> next()
        {
            return m_cursor.next();
        }

        @Override
        public void remove()
        {
            m_cursor.remove();
        }

        @Override
        public void close() throws IOException
        {
            if (false == m_closed)
            {
                m_cursor.close();

                m_closed = true;
            }
        }
    }

    public static final class MIndexCursor extends AbstractMCursor<ListIndexesIterable<Document>>
    {
        protected MIndexCursor(final ListIndexesIterable<Document> index)
        {
            super(index);
        }
    }

    public static final class MAggregateCursor extends AbstractMCursor<AggregateIterable<Document>>
    {
        protected MAggregateCursor(final AggregateIterable<Document> aggreg)
        {
            super(aggreg);
        }
    }

    public static final class MCursor extends AbstractMCursor<FindIterable<Document>>
    {
        protected MCursor(final FindIterable<Document> finder)
        {
            super(finder);
        }

        public MCursor projection(final MProjection projection)
        {
            return new MCursor(self().projection(CommonOps.requireNonNull(projection)));
        }

        public MCursor skip(final int skip)
        {
            return new MCursor(self().skip(Math.max(0, skip)));
        }

        public MCursor limit(final int limit)
        {
            return new MCursor(self().limit(Math.max(0, limit)));
        }

        public MCursor sort(final Map<String, ?> sort)
        {
            return sort(new MSort(sort));
        }

        public MCursor sort(final MSort sort)
        {
            return new MCursor(self().sort(CommonOps.requireNonNull(sort)));
        }
    }

    public static final class MSort extends Document
    {
        private static final long   serialVersionUID = 1L;

        private static final Logger m_logger         = Logger.getLogger(MSort.class);

        private MSort()
        {
        }

        public MSort(final Map<String, ?> map)
        {
            super(CommonOps.STRMAP(map));
        }

        public static final MSort ASCENDING(final String... fields)
        {
            return ASCENDING(CommonOps.toList(fields));
        }

        public static final MSort ASCENDING(final List<String> fields)
        {
            return ORDER_BY(CommonOps.requireNonNull(fields), ORDER_A);
        }

        public static final MSort DESCENDING(final String... fields)
        {
            return DESCENDING(CommonOps.toList(fields));
        }

        public static final MSort DESCENDING(final List<String> fields)
        {
            return ORDER_BY(CommonOps.requireNonNull(fields), ORDER_D);
        }

        public static final MSort ORDER_BY(final MSort... sorts)
        {
            return ORDER_BY(CommonOps.toList(sorts));
        }

        public static final MSort ORDER_BY(final List<MSort> sorts)
        {
            CommonOps.requireNonNull(sorts);

            final MSort sort = new MSort();

            for (final MSort s : sorts)
            {
                if (null != s)
                {
                    for (final String k : CommonOps.toKeys(s))
                    {
                        if (null != StringOps.toTrimOrNull(k))
                        {
                            sort.remove(k);

                            sort.append(k, s.get(k));
                        }
                    }
                }
                else
                {
                    m_logger.warn("MSort.ORDER_BY(null)");
                }
            }
            return sort;
        }

        private static final MSort ORDER_BY(final List<String> fields, final BsonInt32 value)
        {
            CommonOps.requireNonNull(fields);

            final MSort sort = new MSort();

            for (final String name : fields)
            {
                if (null != StringOps.toTrimOrNull(name))
                {
                    sort.remove(name);

                    sort.append(name, value);
                }
            }
            return sort;
        }
    }

    public static final class MProjection extends Document
    {
        private static final long serialVersionUID = 1L;

        private MProjection()
        {
        }

        public MProjection(final Map<String, ?> map)
        {
            super(CommonOps.STRMAP(map));
        }

        private MProjection(final String name, final BsonValue value)
        {
            super(StringOps.requireTrimOrNull(name), value);
        }

        public static final MProjection INCLUDE(final String... fields)
        {
            return INCLUDE(CommonOps.toList(fields));
        }

        public static final MProjection INCLUDE(final List<String> fields)
        {
            return COMBINE(CommonOps.requireNonNull(fields), INCLUDE_Y);
        }

        public static final MProjection EXCLUDE(final String... fields)
        {
            return EXCLUDE(CommonOps.toList(fields));
        }

        public static final MProjection EXCLUDE(final List<String> fields)
        {
            return COMBINE(CommonOps.requireNonNull(fields), INCLUDE_N);
        }

        public static final MProjection NO_ID()
        {
            return new MProjection(MONGODB_ID_KEY, INCLUDE_N);
        }

        public static final MProjection FIELDS(final MProjection... projections)
        {
            return FIELDS(CommonOps.toList(projections));
        }

        public static final MProjection FIELDS(final List<MProjection> projections)
        {
            final MProjection projection = new MProjection();

            for (final MProjection p : projections)
            {
                for (String k : CommonOps.toKeys(p))
                {
                    if (null != (k = StringOps.toTrimOrNull(k)))
                    {
                        projection.remove(k);

                        projection.append(k, p.get(k));
                    }
                }
            }
            return projection;
        }

        private static final MProjection COMBINE(final List<String> fields, final BsonInt32 value)
        {
            final MProjection projection = new MProjection();

            for (String name : fields)
            {
                if (null != (name = StringOps.toTrimOrNull(name)))
                {
                    projection.remove(name);

                    projection.append(name, value);
                }
            }
            return projection;
        }
    }

    public static class MQuery extends Document
    {
        private static final long serialVersionUID = 1L;

        private MQuery()
        {
        }

        public MQuery(final Map<String, ?> map)
        {
            super(CommonOps.STRMAP(map));
        }

        public static final MQuery QUERY(final Map<String, ?> map)
        {
            return new MQuery(map);
        }

        public static final <T> MQuery EQ(final String name, final T value)
        {
            return convert(eq(StringOps.requireTrimOrNull(name), value));
        }

        public static final <T> MQuery NE(final String name, final T value)
        {
            return convert(ne(StringOps.requireTrimOrNull(name), value));
        }

        public static final <T> MQuery GT(final String name, final T value)
        {
            return convert(gt(StringOps.requireTrimOrNull(name), value));
        }

        public static final <T> MQuery LT(final String name, final T value)
        {
            return convert(lt(StringOps.requireTrimOrNull(name), value));
        }

        public static final <T> MQuery GTE(final String name, final T value)
        {
            return convert(gte(StringOps.requireTrimOrNull(name), value));
        }

        public static final <T> MQuery LTE(final String name, final T value)
        {
            return convert(lte(StringOps.requireTrimOrNull(name), value));
        }

        @SafeVarargs
        public static final <T> MQuery IN(final String name, final T... list)
        {
            return IN(StringOps.requireTrimOrNull(name), CommonOps.toList(list));
        }

        public static final <T> MQuery IN(final String name, final List<T> list)
        {
            return convert(in(StringOps.requireTrimOrNull(name), CommonOps.requireNonNull(list)));
        }

        @SafeVarargs
        public static final <T> MQuery NIN(final String name, final T... list)
        {
            return NIN(StringOps.requireTrimOrNull(name), CommonOps.toList(list));
        }

        public static final <T> MQuery NIN(final String name, final List<T> list)
        {
            return convert(nin(StringOps.requireTrimOrNull(name), CommonOps.requireNonNull(list)));
        }

        public static final MQuery AND(final MQuery... list)
        {
            return AND(CommonOps.toList(list));
        }

        public static final MQuery AND(final List<MQuery> list)
        {
            return convert(and(new ArrayList<Bson>(CommonOps.requireNonNull(list))));
        }

        public static final MQuery OR(final MQuery... list)
        {
            return OR(CommonOps.toList(list));
        }

        public static final MQuery OR(final List<MQuery> list)
        {
            return convert(or(new ArrayList<Bson>(CommonOps.requireNonNull(list))));
        }

        public static final MQuery NOR(final MQuery... list)
        {
            return NOR(CommonOps.toList(list));
        }

        public static final MQuery NOR(final List<MQuery> list)
        {
            return convert(nor(new ArrayList<Bson>(CommonOps.requireNonNull(list))));
        }

        public static final MQuery NOT(final MQuery query)
        {
            return convert(not(CommonOps.requireNonNull(query)));
        }

        public static final MQuery EXISTS(final String name, final boolean exists)
        {
            return convert(exists(StringOps.requireTrimOrNull(name), exists));
        }

        public static final MQuery EXISTS(final String name)
        {
            return convert(exists(StringOps.requireTrimOrNull(name), true));
        }

        public static final MQuery REGEX(final String name, final String pattern)
        {
            return convert(regex(StringOps.requireTrimOrNull(name), CommonOps.requireNonNull(pattern)));
        }

        public static final MQuery REGEX(final String name, final Pattern pattern)
        {
            return convert(regex(StringOps.requireTrimOrNull(name), CommonOps.requireNonNull(pattern)));
        }

        @SuppressWarnings("serial")
        private static final MQuery convert(final Bson bson)
        {
            return new MQuery()
            {
                @Override
                public <TDocument> BsonDocument toBsonDocument(final Class<TDocument> type, final CodecRegistry codec)
                {
                    return bson.toBsonDocument(type, codec);
                }
            };
        }
    }
}