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


import java.util.logging.Level;
import java.util.logging.Logger;

import ch.gbrain.gwtstorage.model.StorageResource;

import com.google.gwt.core.client.Callback;
import com.google.gwt.user.client.Command;
import com.googlecode.gwtphonegap.client.PhoneGap;
import com.googlecode.gwtphonegap.client.file.DirectoryEntry;
import com.googlecode.gwtphonegap.client.file.FileCallback;
import com.googlecode.gwtphonegap.client.file.FileDownloadCallback;
import com.googlecode.gwtphonegap.client.file.FileEntry;
import com.googlecode.gwtphonegap.client.file.FileTransfer;
import com.googlecode.gwtphonegap.client.file.FileTransferError;
import com.googlecode.gwtphonegap.client.file.FileTransferProgressEvent;

public class StorageResourceCollector implements Command
{
  private Logger logger;
  private StorageManager storageManager;
  private PhoneGap phonegap;
  private StorageResource storageResource;
  
  public StorageResourceCollector(StorageManager storageManager, StorageResource storageResource)
  {
    this.storageManager = storageManager;
    this.logger = storageManager.getLogger();
    this.phonegap = storageManager.getPhonegap();
    this.storageResource = storageResource;
  }
  
  @Override
  public void execute()
  {
    try
    {
      Boolean versionCheck = storageManager.checkResourceVersion(storageResource);
      if (versionCheck == null)
      {
        logger.log(Level.WARNING, "ResourceCacheReference retrieval no chache entry found : " + storageResource.getResourceUrl()+ " requestedVersion:" + storageResource.getVersion() + " -> invoke loading");
        downloadCacheResource(storageResource);
      } else if (versionCheck == true)
      {
        // it should be there already and version is ok
        logger.log(Level.INFO, "Successful ResourceCacheReference retrieval : " + storageResource.getResourceUrl());
        // check if it really exists but asynch
        String fileName = storageManager.convertFilePathToFileName(storageResource.getResourceUrl());
        storageManager.getLocalFileReference(storageManager.getCacheDirectory(), fileName, false, new FileCallback<FileEntry, StorageError>()
        {
          @Override
          public void onSuccess(FileEntry entry)
          {
            logger.log(Level.INFO, "Successful ResourceCacheFile retrieval : " + storageResource.getResourceUrl());
            // the cache is ok, file is there in right version, we don't have to
            // do something really.
            if (storageResource.getDownloadNotification()!=null)
            {
              storageResource.getDownloadNotification().onSuccess(entry);
            }
          }
          @Override
          public void onFailure(StorageError error)
          {
            logger.log(Level.SEVERE, "Failure ResourceCacheReference retrieval : " + storageResource.getResourceUrl() + " : " + error.toString());
            // nothing found, we must try to download again
            downloadCacheResource(storageResource);
          }
        });
      } else if (versionCheck == false)
      {
        // version doesn't match, invoke reload
        logger.log(Level.WARNING, "ResourceCacheReference retrieval version mismatch : " + storageResource.getResourceUrl() + " requestedVersion:" + storageResource.getVersion() + " -> invoke loading");
        downloadCacheResource(storageResource);
      }
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Exception checking ResourceCache", ex);
    }
    
  }

  
  /**
   * Download the given resource url and store it in the local cache Directory.
   * 
   * @param resource
   * @param destinationDir The URL of the destination Directoy
   */
  public void downloadCacheResource(final StorageResource resource)
  {
    try
    {
      if (resource == null) return;
      logger.log(Level.INFO, "downloadCacheResource " + resource.getResourceUrl() + " Version:" + resource.getVersion());
      storageManager.getCacheDirectoryEntry(new Callback<DirectoryEntry, StorageError>()
      {
        public void onSuccess(DirectoryEntry cacheDir)
        {
          try
          {
            FileTransfer fileTransfer = phonegap.getFile().createFileTransfer();
            String localFileName = storageManager.convertFilePathToFileName(resource.getResourceUrl());
            String sourceUrl = storageManager.getRemoteAppBaseUrl() + resource.getResourceUrl();
            String destUrl = cacheDir.toURL() + localFileName;
            // String destUrl =
            // "cdvfile://localhost/persistent/testapp/test.mp4";
            logger.log(Level.INFO, "downloadResource invoked for : " + sourceUrl + " to : " + destUrl);
            fileTransfer.download(sourceUrl, destUrl, getResourceDownloadHandler(resource));
          } catch (Exception lex)
          {
            logger.log(Level.SEVERE, "Exception in downloadCacheResource success handler", lex);
          }
        }

        public void onFailure(StorageError error)
        {
          logger.log(Level.WARNING, "Failed to download CacheResource for : " + resource.getResourceUrl());
          if (resource.getDownloadNotification()!=null)
          {
            resource.getDownloadNotification().onFailure(new TransferError(error.getErrorCode(),error.getErrorReason()));
          }
        }
      });
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Exception resourceDownload for : " + resource.getResourceUrl(), ex);
    }
  }

  /**
   * Creates and returns a Callback which treats the result for a url resource
   * retrieval The just downloaded resource is registered in the local storage
   * with the version for future cache handling
   * 
   * @return The callback which deals with the asynch result of the remote
   *         resource retrieval
   */
  private FileDownloadCallback getResourceDownloadHandler(final StorageResource resource)
  {
    return new FileDownloadCallback()
    {
      public void onSuccess(FileEntry fileEntry)
      {
        try
        {
          logger.log(Level.INFO, "FileDownload success " + fileEntry.getFullPath() + " for resource=" + resource.getResourceUrl() + " version=" + resource.getVersion());
          // register now in the storage the version for the cache checks in the
          // future
          storageManager.getLocalStorage().setItem(resource.getResourceIdKey(), fileEntry.toURL());
          storageManager.getLocalStorage().setItem(resource.getResourceVersionKey(), resource.getVersion().toString());
          //cacheCheckInProgress = false;
          if (resource.getDownloadNotification()!=null)
          {
            resource.getDownloadNotification().onSuccess(fileEntry);
          }
        } catch (Exception lex)
        {
          logger.log(Level.SEVERE, "Exception on cacheResource download success handler", lex);
        }
      }

      public void onProgress(FileTransferProgressEvent progress)
      {
        if (resource.getDownloadNotification()!=null)
        {
          resource.getDownloadNotification().onProgress(progress);
        }        
      }

      public void onFailure(FileTransferError error)
      {
        logger.log(Level.SEVERE, "FileDownload Failure " + error.toString() + " : " + resource.getResourceUrl());
        //cacheCheckInProgress = false;
        if (resource.getDownloadNotification()!=null)
        {
          resource.getDownloadNotification().onFailure(error);
        }
      }
    };
  }
  
  
  
}
