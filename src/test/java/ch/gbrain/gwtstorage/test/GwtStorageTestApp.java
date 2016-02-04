package ch.gbrain.gwtstorage.test;

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


import ch.gbrain.gwtstorage.manager.StorageManager;
import ch.gbrain.gwtstorage.model.StorageItem;
import ch.gbrain.gwtstorage.test.model.TestItem;
import ch.gbrain.gwtstorage.test.model.TestItemCodec;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * GWT JUnit <b>integration</b> tests must extend GWTTestCase.
 * Using <code>"GwtTest*"</code> naming pattern exclude them from running with
 * surefire during the test phase.
 * 
 * If you run the tests using the Maven command line, you will have to 
 * navigate with your browser to a specific url given by Maven. 
 * See http://mojo.codehaus.org/gwt-maven-plugin/user-guide/testing.html 
 * for details.
 */
public class GwtStorageTestApp extends GWTTestCase 
{

  /**
   * Must refer to a valid module that sources this class.
   */
  public String getModuleName() 
  {
    return "ch.gbrain.gwtstorage.GwtStorageTestAppJUnit";
  }

  public void testStorageItemFromJsonStorageItem()
  {
    StorageItem item1=new TestItem();
    item1.setId("1");
    item1.setVersion(1);
    StorageItem item2=new TestItem();
    item2.fromJson(item1);
    assertTrue(item2.getId().equals(item1.getId()));
    assertTrue(item2.getVersion()==item1.getVersion());
  }

  public void testTestItemFromJsonStorageItem()
  {
    TestItem item1=new TestItem();
    item1.setId("1");
    item1.setVersion(1);
    TestItem item2=new TestItem();
    item2.fromJson(item1);
    assertTrue(item2.getId().equals(item1.getId()));
    assertTrue(item2.getVersion()==item1.getVersion());
  }

  public void testTestItemToAndFromRealJson()
  {
    TestItem item1=new TestItem();
    item1.setId("1");
    item1.setVersion(1);
    item1.setBoolValue(true);
    item1.setNumericValue(5);
    item1.setTextValue("TEST");
    TestItemCodec codec = GWT.create(TestItemCodec.class);
    JSONValue jval = codec.encode(item1);
    String json = jval.toString();
    TestItem item2 = codec.decode(json);
    assertTrue(item2.getId().equals(item1.getId()));
    assertTrue(item2.getVersion()==item1.getVersion());
    assertTrue(item2.getTextValue().equals(item1.getTextValue()));
    assertTrue(item2.getBoolValue()==item1.getBoolValue());
    assertTrue(item2.getNumericValue()==item1.getNumericValue());  
  }
  
  
  public void testCachePathEncryption()
  {
    String tmp = StorageManager.convertFilePathToFileName("content/filename.jpg");
    assertTrue(tmp.equals("content@@filename.jpg"));
    String res = StorageManager.extractFileNameFromCacheFile(tmp);
    assertTrue(res.equals("filename.jpg"));
  }
}
