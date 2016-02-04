package ch.gbrain.gwtstorage.test.model;

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


import java.util.logging.Level;
import java.util.logging.Logger;

import ch.gbrain.gwtstorage.model.StorageItem;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONValue;


public class TestItem extends StorageItem
{

  private String    textValue;
  private Integer   numericValue;
  private Boolean   boolValue;

  public TestItem()
  {   
  }
  
  @JsonIgnore
  public TestItem(String id)
  {
    this.setId(id);
    this.setNumericValue(2);
    this.setBoolValue(Boolean.FALSE);
  }
  
  @Override
  public JSONValue toJson()
  {
    try
    {
      TestItemCodec codec = GWT.create(TestItemCodec.class);
      return codec.encode(this);
    }catch(Exception ex)
    {
      Logger.getGlobal().log(Level.WARNING, "Failure converting to Json", ex);
    }
    return null;
  }
  
  @Override
  public void fromJson(JSONValue json)
  {
    try
    {
      TestItemCodec codec = GWT.create(TestItemCodec.class);
      TestItem tmp =  codec.decode(json);
      if (tmp!=null)
      {
        this.fromJson(tmp);
        this.setTextValue(tmp.textValue);
        this.setBoolValue(tmp.boolValue);
        this.setNumericValue(tmp.numericValue);
      }
    }catch(Exception ex)
    {
      Logger.getGlobal().log(Level.SEVERE, "Failure converting from Json", ex);
    }
  }
  
  
  public String getTextValue()
  {
    return textValue;
  }

  public void setTextValue(String textValue)
  {
    this.textValue = textValue;
  }

  public Integer getNumericValue()
  {
    return numericValue;
  }

  public void setNumericValue(Integer numericValue)
  {
    this.numericValue = numericValue;
  }

  public Boolean getBoolValue()
  {
    return boolValue;
  }

  public void setBoolValue(Boolean boolValue)
  {
    this.boolValue = boolValue;
  }

   
}
