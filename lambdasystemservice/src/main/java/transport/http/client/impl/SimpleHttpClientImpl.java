/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package transport.http.client.impl;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import transport.MessageProcessor;

import java.net.URI;

public class SimpleHttpClientImpl extends HttpClientImpl {

    private URI uri;

    protected SimpleHttpClientImpl(URI uri) {

        this.uri = uri;
    }

    public void sendHttpRequest(HttpRequest httpRequest, MessageProcessor messageProcessor) {
        super.sendHttpRequest(httpRequest, messageProcessor, uri);
    }


    public HttpRequest getDefaultGETHttpRequest() {
        return super.getDefaultGETHttpRequest(uri);
    }


    public FullHttpRequest getDefaultPOSTHttpRequest(ByteBuf content, String contentType) {
        return super.getDefaultPOSTHttpRequest(content, contentType, uri);
    }


    public FullHttpRequest getDefaultPOSTHttpRequest(String content) {
        return super.getDefaultPOSTHttpRequest(content, uri);
    }
}
