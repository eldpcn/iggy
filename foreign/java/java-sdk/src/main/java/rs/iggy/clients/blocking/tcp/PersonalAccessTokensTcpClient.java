/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package rs.iggy.clients.blocking.tcp;

import io.netty.buffer.Unpooled;
import rs.iggy.clients.blocking.PersonalAccessTokensClient;
import rs.iggy.personalaccesstoken.PersonalAccessTokenInfo;
import rs.iggy.personalaccesstoken.RawPersonalAccessToken;
import rs.iggy.user.IdentityInfo;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static rs.iggy.clients.blocking.tcp.BytesDeserializer.readPersonalAccessTokenInfo;
import static rs.iggy.clients.blocking.tcp.BytesDeserializer.readRawPersonalAccessToken;
import static rs.iggy.clients.blocking.tcp.BytesSerializer.nameToBytes;
import static rs.iggy.clients.blocking.tcp.BytesSerializer.toBytesAsU64;

class PersonalAccessTokensTcpClient implements PersonalAccessTokensClient {

    private static final int GET_PERSONAL_ACCESS_TOKENS_CODE = 41;
    private static final int CREATE_PERSONAL_ACCESS_TOKEN_CODE = 42;
    private static final int DELETE_PERSONAL_ACCESS_TOKEN_CODE = 43;
    private static final int LOGIN_WITH_PERSONAL_ACCESS_TOKEN_CODE = 44;

    private final InternalTcpClient tcpClient;

    public PersonalAccessTokensTcpClient(InternalTcpClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    @Override
    public RawPersonalAccessToken createPersonalAccessToken(String name, BigInteger expiry) {
        var payload = Unpooled.buffer();
        payload.writeBytes(nameToBytes(name));
        payload.writeBytes(toBytesAsU64(expiry));
        var response = tcpClient.send(CREATE_PERSONAL_ACCESS_TOKEN_CODE, payload);
        return readRawPersonalAccessToken(response);
    }

    @Override
    public List<PersonalAccessTokenInfo> getPersonalAccessTokens() {
        var response = tcpClient.send(GET_PERSONAL_ACCESS_TOKENS_CODE);
        var tokens = new ArrayList<PersonalAccessTokenInfo>();
        while (response.isReadable()) {
            tokens.add(readPersonalAccessTokenInfo(response));
        }
        return tokens;
    }

    @Override
    public void deletePersonalAccessToken(String name) {
        var payload = nameToBytes(name);
        tcpClient.send(DELETE_PERSONAL_ACCESS_TOKEN_CODE, payload);
    }

    @Override
    public IdentityInfo loginWithPersonalAccessToken(String token) {
        var payload = nameToBytes(token);
        var response = tcpClient.send(LOGIN_WITH_PERSONAL_ACCESS_TOKEN_CODE, payload);
        var userId = response.readUnsignedIntLE();
        return new IdentityInfo(userId, Optional.empty());
    }
}
