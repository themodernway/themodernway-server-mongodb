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

package com.themodernway.server.mongodb;

import java.util.Map;

import org.bson.BsonInt32;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.model.UpdateOptions;
import com.themodernway.common.api.java.util.CommonOps;
import com.themodernway.common.api.java.util.StringOps;
import com.themodernway.server.core.servlet.ICoreServletConstants;

public interface IMongoConstants extends ICoreServletConstants
{
    public static final String        ENSURES_ID_KEY       = "id";

    public static final String        MONGODB_ID_KEY       = "_id";

    public static final BsonInt32     INCLUDE_N           = new BsonInt32(0);

    public static final BsonInt32     INCLUDE_Y           = new BsonInt32(1);

    public static final BsonInt32     ORDER_A             = new BsonInt32(1);

    public static final BsonInt32     ORDER_D             = new BsonInt32(CommonOps.IS_NOT_FOUND);

    public static final UpdateOptions UPSERT_OPTIONS_TRUE = new UpdateOptions().upsert(true);

    public static Document DOCUMENT(final Map<String, ?> maps)
    {
        return new Document(CommonOps.STRMAP(CommonOps.requireNonNull(maps)));
    }

    public static Map<String, ?> ENSUREID(final Map<String, ?> update)
    {
        final Object id = update.get(ENSURES_ID_KEY);

        if ((false == (id instanceof String)) || (null == StringOps.toTrimOrNull(id.toString())))
        {
            CommonOps.STRMAP(update).put(ENSURES_ID_KEY, (new ObjectId()).toString());
        }
        return update;
    }
}
