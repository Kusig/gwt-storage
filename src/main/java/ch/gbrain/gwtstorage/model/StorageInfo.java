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

import java.util.Date;

import ch.gbrain.gwtstorage.manager.StorageManager;

/**
 * Holds some informations about a cached StorageObject
 *
 */
public class StorageInfo
{

  private String storageKey;
  private String version;
  private String fileName;
  private String filePath;
  private String fileUrl;
  private Long fileSize;
  private Date lastModificationDate;

  public String getStorageKey()
  {
    return storageKey;
  }

  public void setStorageKey(String storageKey)
  {
    this.storageKey = storageKey;
  }

  public String getVersion()
  {
    return version;
  }

  public void setVersion(String version)
  {
    this.version = version;
  }

  public String getFileName()
  {
    return fileName;
  }
  
  /**
   * Cleans the filename from the cache path 
   * @return The clean file name without any transformed cache path
   */
  public String getCleanFileName()
  {
    return StorageManager.extractFileNameFromCacheFile(fileName);
  }
  

  public void setFileName(String fileName)
  {
    this.fileName = fileName;
  }

  public String getFilePath()
  {
    return filePath;
  }

  public void setFilePath(String filePath)
  {
    this.filePath = filePath;
  }

  public String getFileUrl()
  {
    return fileUrl;
  }

  public void setFileUrl(String fileUrl)
  {
    this.fileUrl = fileUrl;
  }

  public Date getLastModificationDate()
  {
    return lastModificationDate;
  }

  public void setLastModificationDate(Date lastModificationDate)
  {
    this.lastModificationDate = lastModificationDate;
  }

  public Long getFileSize()
  {
    return fileSize;
  }

  public void setFileSize(Long fileSize)
  {
    this.fileSize = fileSize;
  }

}
