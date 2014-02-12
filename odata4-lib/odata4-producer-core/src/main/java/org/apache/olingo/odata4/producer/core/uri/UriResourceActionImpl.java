/*******************************************************************************
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
 ******************************************************************************/
package org.apache.olingo.odata4.producer.core.uri;

import org.apache.olingo.odata4.commons.api.edm.EdmAction;
import org.apache.olingo.odata4.commons.api.edm.EdmActionImport;
import org.apache.olingo.odata4.commons.api.edm.EdmType;
import org.apache.olingo.odata4.producer.api.uri.UriResourceAction;
import org.apache.olingo.odata4.producer.api.uri.UriResourceKind;

public class UriResourceActionImpl extends UriResourceTypedImpl implements UriResourceAction {

  protected EdmAction action;
  protected EdmActionImport actionImport;

  public UriResourceActionImpl() {
    super(UriResourceKind.action);
  }

  @Override
  public EdmAction getAction() {
    return action;
  }

  public UriResourceActionImpl setAction(final EdmAction action) {
    this.action = action;
    return this;
  }

  @Override
  public EdmActionImport getActionImport() {
    return actionImport;
  }

  public UriResourceActionImpl setActionImport(final EdmActionImport actionImport) {
    this.actionImport = actionImport;
    setAction(actionImport.getAction());
    return this;
  }

  @Override
  public boolean isCollection() {
    return action.getReturnType().isCollection();
  }

  @Override
  public EdmType getType() {
    return action.getReturnType().getType();
  }

  @Override
  public String toString() {
    if (actionImport != null) {
      return actionImport.getName();
    } else if (action != null) {
      return action.getName();
    }
    return "";
  }

}
