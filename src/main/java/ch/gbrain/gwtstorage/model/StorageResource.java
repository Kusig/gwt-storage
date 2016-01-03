package ch.gbrain.gwtstorage.model;

/*
 * #%L
 * GwtStorage
 * %%
 * Copyright (C) 2016 gbrain.ch
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

public class StorageResource
{
  private static String RESOURCEKEYPREFIX = "resource-";
  private static String RESOURCEKEYVERSIONSUFFIX = "-v";

  String resourceUrl;
  Integer version;

  public StorageResource(String url, Integer version)
  {
    this.resourceUrl = url;
    this.version = version;
  }

  public String getResourceIdKey()
  {
    return RESOURCEKEYPREFIX + resourceUrl;
  }

  public String getResourceVersionKey()
  {
    return getResourceVersionKey(getResourceIdKey());
  }

  public static String getResourceVersionKey(String resourceKey)
  {
    return resourceKey + RESOURCEKEYVERSIONSUFFIX;
  }

  public static boolean isResourceKey(String key)
  {
    if (key == null) return false;
    if (key.startsWith(RESOURCEKEYPREFIX)) return true;
    return false;
  }

  public static boolean isResourceIdKey(String key)
  {
    if (key == null) return false;
    if (isResourceKey(key) && !key.endsWith(RESOURCEKEYVERSIONSUFFIX)) return true;
    return false;
  }

  public static boolean isResourceVersionKey(String key)
  {
    if (key == null) return false;
    if (isResourceKey(key) && key.endsWith(RESOURCEKEYVERSIONSUFFIX)) return true;
    return false;
  }

  public String getResourceUrl()
  {
    return resourceUrl;
  }

  public Integer getVersion()
  {
    return version;
  }

}
