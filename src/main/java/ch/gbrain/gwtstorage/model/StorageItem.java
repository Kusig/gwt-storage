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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;

/**
 * The main parent class for any object that must be stored locally or remote By
 * defining the abstract converter methods for your particular inherited object
 * the object is available for simple storage by the StorageManager
 *
 */
// @JsonSubTypes({@Type(DomainItem.class), @Type(DomainItems.class)})
public abstract class StorageItem
{

  private String id;

  public void setId(String id)
  {
    this.id = id;
  }

  public String getId()
  {
    return this.id;
  }

  private static String STORAGEITEMKEYPREFIX = "item-";
  private static String STORAGEITEMTIMESUFFIX = "-t";

  /**
   * Retrieve a unique key to identify StorageItems in a key/value storage
   * 
   * @return Retrieve a unique key for this storage item instance. Build from a prefix + object-type-name + id
   */
  @JsonIgnore
  public String getStorageItemIdKey()
  {
    // return this.id.toString();
    return STORAGEITEMKEYPREFIX + this.getTypeName() + "-" + this.getId().toString();
  }

  /**
   * Retrieve a unique key to identify StorageItems Save Time in a key/value
   * storage
   * 
   * @return Retrieve a unique key for this storage item to store the time information. Based on the StorageItemKey + a suffix
   */
  @JsonIgnore
  public String getStorageItemTimeKey()
  {
    return this.getStorageItemIdKey() + STORAGEITEMTIMESUFFIX;
  }

  @JsonIgnore
  public static boolean isStorageItemKey(String key)
  {
    if (key == null) return false;
    if (key.startsWith(STORAGEITEMKEYPREFIX)) return true;
    return false;
  }

  @JsonIgnore
  public static boolean isStorageItemIdKey(String key)
  {
    if (key == null) return false;
    if (isStorageItemKey(key) && !key.endsWith(STORAGEITEMTIMESUFFIX)) return true;
    return false;
  }

  @JsonIgnore
  public static boolean isStorageItemTimeKey(String key)
  {
    if (key == null) return false;
    if (isStorageItemKey(key) && key.endsWith(STORAGEITEMTIMESUFFIX)) return true;
    return false;
  }

  /**
   * Retrieve a unique key to identify StorageItems for log entries
   * 
   * @return
   */
  @JsonIgnore
  public String getLogId()
  {
    return " item=" + getId() + " class=" + this.getTypeName();
  }

  private Integer version = 1;

  public void setVersion(Integer version)
  {
    this.version = version;
  }

  public Integer getVersion()
  {
    return version;
  }

  @JsonIgnore
  public String getTypeName()
  {
    return this.getClass().getCanonicalName();
  }

  @JsonIgnore
  public String getJsonFileName()
  {
    return this.getTypeName() + "-" + getId() + ".json";
  }

  /**
   * Used to convert the object value to a JsonValue. This will be used to store
   * the object in the local and remote storage.
   * 
   * @return
   */
  @JsonIgnore
  public abstract JSONValue toJson();

  /**
   * Used to convert the JSONValue object (as stored in local and remote
   * storage) back to the object itself.
   * 
   * @param storedItem
   *          The value which was stored in the LocalStore and should be
   *          transformed back into the object.
   */
  @JsonIgnore
  public abstract void fromJson(JSONValue json);

  /**
   * Convenience method to read the attributes of the StorageItem in and assign
   * them internally from a Json read object which is inherited from
   * StrorageItem
   * 
   * @param storageItem
   *          The item from which the values must be read and assign
   */
  @JsonIgnore
  public final void fromJson(StorageItem storageItem)
  {
    this.id = storageItem.id;
    this.version = storageItem.version;
    if (this.version == null) this.version = 0;
  }

  /**
   * Converts back from a json string to the object itself
   * 
   * @param jsonString
   */
  @JsonIgnore
  public void fromJson(String jsonString)
  {
    JSONValue val = JSONParser.parseStrict(jsonString);
    fromJson(val);
  }

  /**
   * Retrieves the current object state as stringified JSON value
   * 
   * @return The current objects state as JSON String
   */
  @JsonIgnore
  public String toString()
  {
    return toJson().toString();
  }

}
