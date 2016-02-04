package ch.gbrain.gwtstorage.manager;

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
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.gbrain.gwtstorage.model.StorageInfo;
import ch.gbrain.gwtstorage.model.StorageResource;

import com.google.gwt.core.client.Callback;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Command;
import com.googlecode.gwtphonegap.client.PhoneGap;
import com.googlecode.gwtphonegap.client.file.EntryBase;
import com.googlecode.gwtphonegap.client.file.FileCallback;
import com.googlecode.gwtphonegap.client.file.FileEntry;
import com.googlecode.gwtphonegap.client.file.FileError;
import com.googlecode.gwtphonegap.client.file.FileObject;
import com.googlecode.gwtphonegap.client.file.Metadata;


/**
 * This Command class is used to schedule the collecting of the file information for one given resource stored in the cache and
 * referenced by the corresponding storageKey.
 * @author Markus
 * 
 *
 */
public class StorageInfoCollector implements Command
{
  private String storageKey;
  private String version;
  private String fileName;
  private String filePath;
  private String fileUrl;
  private Long fileSize;
  private Date lastModificationDate;
  private Callback<StorageInfo, FileError> callback;
  private FileEntry fileEntry;
  private StorageManager storageManager;
  private Logger logger;
  private PhoneGap phonegap;
  private Storage storage;

  /**
   * 
   * @param logger if null is given, no logging takes place.
   * @param storage
   * @param phonegap
   * @param storageKey
   * @param callback
   */
  public StorageInfoCollector(StorageManager storageManager, String storageKey, Callback<StorageInfo, FileError> callback)
  {
    this.storageManager = storageManager;
    this.logger = storageManager.getLogger();
    this.storage = storageManager.getLocalStorage();
    this.phonegap = storageManager.getPhonegap();
    this.storageKey = storageKey;
    this.callback = callback;
  }

  private String logBaseInfo()
  {
    return "CollectInfo : " + storageKey + " / ";
  }

 
  public void execute()
  {
    String versionKey = StorageResource.getResourceVersionKey(storageKey);
    version = storage.getItem(versionKey);
    fileUrl = storage.getItem(storageKey);
    // now resolve the file asynch
    phonegap.getFile().resolveLocalFileSystemURI(fileUrl, new FileCallback<EntryBase, FileError>()
    {
      @Override
      public void onSuccess(EntryBase entry)
      {
        if (logger!=null)logger.log(Level.INFO, logBaseInfo() + "ResolveLocalFileSystemUri success");
        fileEntry = entry.getAsFileEntry();
        fileEntry.getFile(new FileCallback<FileObject, FileError>()
        {
          @Override
          public void onSuccess(FileObject entry)
          {
            if (logger!=null)logger.log(Level.INFO, logBaseInfo() + "FileEntry located : " + entry.getFullPath() + " name:" + entry.getName());
            fileName = entry.getName();
            filePath = entry.getFullPath();
            fileSize = entry.size();
            try
            { // might throw an exception (unknown method in Phonegap .....
              // lastModificationDate = entry.getLastModifiedDate(); -> take
              // it from Metadata instead
            } catch (Exception ex)
            {
              if (logger!=null)logger.log(Level.FINEST, logBaseInfo() + "Failure in File Modification Date evaluation", ex);
            }
            fileEntry.getMetadata(new FileCallback<Metadata, FileError>()
            {
              @Override
              public void onSuccess(Metadata metadata)
              {
                if (logger!=null)logger.log(Level.INFO, logBaseInfo() + "Successful FileMetadata located");
                try
                {
                  lastModificationDate = metadata.getModificationTime();
                } catch (Exception ex)
                {
                  if (logger!=null)logger.log(Level.FINEST, logBaseInfo() + "Failure in Metadata Modification Date evaluation", ex);
                }
                invokeSuccessCallback();
              }

              @Override
              public void onFailure(FileError error)
              {
                if (logger!=null)logger.log(Level.WARNING, logBaseInfo() + "Failure cache FileEntry Metadata retrieval with error : " + error.toString());
                // anyhow signal success even if we don't have the Metadata, but we have the FileObject data already
                invokeSuccessCallback();
              }
            });
          }

          @Override
          public void onFailure(FileError error)
          {
            if (logger!=null)logger.log(Level.SEVERE, logBaseInfo() + "Failure cache FileEntry info retrieval with error : " + error.toString());
            callback.onFailure(error);
          }
        });
      }

      @Override
      public void onFailure(FileError error)
      {
        if (logger!=null)logger.log(Level.WARNING, logBaseInfo() + "Unable to locate cache File information with error : " + error.getErrorCode());
        if (callback != null)
        {
          callback.onFailure(error);
        }
      }
    });
  }

  private void invokeSuccessCallback()
  {
    StorageInfo info = new StorageInfo();
    info.setFileName(this.fileName);
    info.setFilePath(filePath);
    info.setFileUrl(fileUrl);
    info.setFileSize(fileSize);
    info.setLastModificationDate(lastModificationDate);
    info.setStorageKey(storageKey);
    info.setVersion(version);
    callback.onSuccess(info);
  }

}
