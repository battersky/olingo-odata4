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
package org.apache.olingo.client.core.communication.request.retrieve;

import java.io.IOException;
import java.net.URI;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.communication.request.retrieve.ODataValueRequest;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ODataJClientEdmPrimitiveType;
import org.apache.olingo.client.api.domain.ODataPrimitiveValue;
import org.apache.olingo.client.api.domain.ODataValue;
import org.apache.olingo.client.api.format.ODataValueFormat;
import org.apache.olingo.client.api.http.HttpClientException;

/**
 * This class implements an OData entity property value query request.
 */
public class ODataValueRequestImpl extends AbstractODataRetrieveRequest<ODataValue, ODataValueFormat>
        implements ODataValueRequest {

  /**
   * Private constructor.
   *
   * @param odataClient client instance getting this request
   * @param query query to be executed.
   */
  ODataValueRequestImpl(final ODataClient odataClient, final URI query) {
    super(odataClient, ODataValueFormat.class, query);
  }

  /**
   * {@inheritDoc }
   */
  @Override
  public ODataRetrieveResponse<ODataValue> execute() {
    final HttpResponse res = doExecute();
    return new ODataValueResponseImpl(httpClient, res);
  }

  /**
   * Response class about an ODataDeleteReODataValueRequestquest.
   */
  protected class ODataValueResponseImpl extends ODataRetrieveResponseImpl {

    private ODataValue value = null;

    /**
     * Constructor.
     * <p>
     * Just to create response templates to be initialized from batch.
     */
    private ODataValueResponseImpl() {
    }

    /**
     * Constructor.
     *
     * @param client HTTP client.
     * @param res HTTP response.
     */
    private ODataValueResponseImpl(final HttpClient client, final HttpResponse res) {
      super(client, res);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ODataValue getBody() {
      if (value == null) {
        final ODataValueFormat format = ODataValueFormat.fromString(getContentType());

        try {
          value = new ODataPrimitiveValue.Builder(odataClient).
                  setType(format == ODataValueFormat.TEXT
                  ? ODataJClientEdmPrimitiveType.String : ODataJClientEdmPrimitiveType.Stream).
                  setText(IOUtils.toString(getRawResponse())).
                  build();
        } catch (IOException e) {
          throw new HttpClientException(e);
        } finally {
          this.close();
        }
      }
      return value;
    }
  }
}