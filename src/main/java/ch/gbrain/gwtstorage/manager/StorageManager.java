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

import org.fusesource.restygwt.client.JsonCallback;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.Resource;

import ch.gbrain.gwtstorage.model.StorageInfo;
import ch.gbrain.gwtstorage.model.StorageItem;
import ch.gbrain.gwtstorage.model.StorageResource;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.i18n.shared.DateTimeFormat.PredefinedFormat;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.storage.client.Storage;
import com.googlecode.gwtphonegap.client.PhoneGap;
import com.googlecode.gwtphonegap.client.connection.Connection;
import com.googlecode.gwtphonegap.client.file.DirectoryEntry;
import com.googlecode.gwtphonegap.client.file.EntryBase;
import com.googlecode.gwtphonegap.client.file.FileCallback;
import com.googlecode.gwtphonegap.client.file.FileDownloadCallback;
import com.googlecode.gwtphonegap.client.file.FileEntry;
import com.googlecode.gwtphonegap.client.file.FileError;
import com.googlecode.gwtphonegap.client.file.FileReader;
import com.googlecode.gwtphonegap.client.file.FileSystem;
import com.googlecode.gwtphonegap.client.file.FileWriter;
import com.googlecode.gwtphonegap.client.file.Flags;
import com.googlecode.gwtphonegap.client.file.ReaderCallback;
import com.googlecode.gwtphonegap.client.file.WriterCallback;

/**
 * This class deals about writing and reading objects from Type StorageItem and
 * as well loading application resource files (eg. video files) from the
 * applications home base (server base). This is useful when running the App in
 * the Web but as well in a Phonegap container as we could store the files
 * locally (cache) when running in the phonegap container.
 */
public class StorageManager
{

  /**
   * The remote base url of the application
   */
  private String remoteAppBaseUrl = "http://www.exmaple.com/myapp/";

  public String getRemoteAppBaseUrl()
  {
    return remoteAppBaseUrl;
  }

  public void setRemoteAppBaseUrl(String baseUrl)
  {
    this.remoteAppBaseUrl = baseUrl;
    logger.log(Level.INFO, "SetRemoteAppBaseUrl:" + baseUrl);
  }

  /**
   * The default storage url in the application. This is the relative location
   * from the applications base directory.
   */
  private String storageUrl = "storage/v1/";

  public String getStorageUrl()
  {
    return storageUrl;
  }

  private String getLocalStorageUrl()
  {
    return storageUrl;
  }

  private String getRemoteStorageUrl()
  {
    return getRemoteAppBaseUrl() + storageUrl;
  }

  public void setStorageUrl(String storageUrl)
  {
    this.storageUrl = storageUrl;
    logger.log(Level.INFO, "SetStorageUrl:" + storageUrl);
  }

  /**
   * The directory we are going to create locally on the mobile device as cache
   * directory when running in the phonegap container.
   */
  private String cacheDirectory = "myApp";

  public String getCacheDirectory()
  {
    return cacheDirectory;
  }

  public void setCacheDirectory(String cacheDirectory)
  {
    if (this.cacheDirectory.equals(cacheDirectory)) return; // nothing to do it
                                                            // is the same as
                                                            // before
    this.cacheDirectory = cacheDirectory;
    this.cacheDirectoryEntry = null;
    logger.log(Level.INFO, "SetCacheDirectory:" + cacheDirectory);
  }

  /**
   * Needs to be enabled to perform any caching at all.
   */
  private boolean cacheEnabled = true;

  public void setCacheEnabled(boolean cacheEnabled)
  {
    this.cacheEnabled = cacheEnabled;
  }

  public boolean getCacheEnabled()
  {
    return this.cacheEnabled;
  }

  /**
   * If enabled, the download of the big resource files for local caching (eg.
   * videos) is only invoked if we are in a wlan network connected. Else it is
   * taken from backend url always by need.
   */
  private boolean wlanEnabled = true;

  public void setWlanEnabled(boolean wlanEnabled)
  {
    this.wlanEnabled = wlanEnabled;
  }

  public boolean getWlanEnabled()
  {
    return this.wlanEnabled;
  }

  private Boolean lastCachingState = null;

  private void logResourceCachingState(boolean state, String msg)
  {
    if (lastCachingState == null || lastCachingState != state)
    {
      logger.log(Level.INFO, msg);
    }
    lastCachingState = state;
  }

  /**
   * Evaluates if resource caching is currently enabled at all.
   * 
   * @return true if resouces shall be downloaded actually.
   */
  private boolean isResourceCachingEnabled()
  {
    if (!phonegap.isPhoneGapDevice()) return false;
    if (!this.getCacheEnabled())
    {
      logResourceCachingState(false, "ResourceCaching is disabled");
      return false;
    }
    if (this.getWlanEnabled())
    {
      // check connection state
      if (phonegap.isPhoneGapDevice() && phonegap.getConnection().getType().equalsIgnoreCase(Connection.WIFI))
      {
        logResourceCachingState(true, "Wlan requested and available, ResourceCaching enabled");
        return true;
      }
      logResourceCachingState(false, "Wlan requested but not available, ResourceCaching disabled");
      return false;
    }
    logResourceCachingState(true, "ResourceCaching enabled");
    return true;
  }

  /**
   * Evaluates if Caching is possible at all on the given client device at the
   * moment of evaluation.
   * 
   * @return True if we have access to the local file system and therefore are able to provide a local resource caching.
   */
  public boolean isResourceCachingPossible()
  {
    if (!phonegap.isPhoneGapDevice()) return false;
    return true;
  }

  private PhoneGap phonegap;
  public PhoneGap getPhonegap()
  {
    return phonegap;
  }
  
  private Logger logger;
  public  Logger getLogger()
  {
    return logger;
  }

  private Storage localStorage = null;

  /**
   * Default constructor setting up the StorageManager all inclusive Logger and
   * Phonegap references are setup locally
   */
  public StorageManager()
  {
    this(null, null);
  }

  /**
   * Constructor allowing to inject the Phonegap reference and a Logger
   * 
   * @param phonegap Give the phonegap reference you have already. Give null if
   *          you want it to be treated here automatically.
   * @param logger Give a logger reference or null if you want it to be treated
   *          here locally.
   */
  public StorageManager(PhoneGap phonegap, Logger logger)
  {
    if (phonegap != null)
    {
      this.phonegap = phonegap;
    } else
    {
      this.phonegap = GWT.create(PhoneGap.class);
    }
    if (logger != null)
    {
      this.logger = logger;
    } else
    {
      this.logger = Logger.getLogger("StorageManager");
    }
    getLocalStorage();
    this.getFileSystem(null);
  }

  /**
   * Retrieve a reference to the local Storage (Browsers HTML5 key-value store)
   * 
   * @return The local storage or null if not supported
   */
  public Storage getLocalStorage()
  {
    if (localStorage == null)
    {
      localStorage = Storage.getLocalStorageIfSupported();
      if (localStorage == null)
      {
        logger.log(Level.SEVERE, "No LocalStorage available!!!!!!!!!!!!!");
      }
    }
    return localStorage;
  }

  /****************************************************************************************************************
   * Read / Write StorageItem to private local HTML5 storage
   ****************************************************************************************************************/

  /**
   * Write the given item to the local HTML5 storage.
   * 
   * @param item The object to be serialized and stored under the ID within the
   *          local storage
   * @return true if the write operation succeeded
   */
  public boolean writeStorageItemToLocalStorage(StorageItem item)
  {
    if (item == null) return false;
    try
    {
      JSONValue json = item.toJson();
      getLocalStorage().setItem(item.getStorageItemIdKey(), json.toString());
      writeStorageItemStorageTimeToLocalStorage(item);
      logger.log(Level.INFO, "Local StorageItem written" + item.getLogId());
      return true;
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Failure local write" + item.getLogId(), ex);
    }
    return false;
  }

  /**
   * Read the item from the local html5 storage.
   * 
   * @param item The item to be read from the local storage with the given ID as
   *          key.
   * @return false if the read operation failed or nothing is found.
   */
  public boolean readStorageItemFromLocalStorage(StorageItem item)
  {
    return readStorageItemFromLocalStorage(item, 0, 0);
  }

  /**
   * Read the item from the local html5 storage and take the cacheTime in
   * account
   * 
   * @param item
   * @param expectedVersion The minimum item version, not checked if <=0
   * @param cacheTime If the item was stored longer than the cacheTime, it isn't
   *          accepted, not checked if <=0
   * @return false if the read operation failed or nothing is found, the version
   *         wasn't ok or the cacheTime elapsed already
   */
  private boolean readStorageItemFromLocalStorage(StorageItem item, int expectedVersion, int cacheTime)
  {
    if (item == null) return false;
    try
    {
      String val = getLocalStorage().getItem(item.getStorageItemIdKey());
      if (val != null)
      {
        logger.log(Level.INFO, "Local StorageItem found" + item.getLogId());
        item.fromJson(val);
        // check if the version is ok
        if (expectedVersion > 0)
        {
          if (!checkStorageItemVersion(expectedVersion, item.getVersion()))
          {
            logger.log(Level.INFO, "Local StorageItem version mismatch" + item.getLogId());
            return false;
          }
        }
        // check if cache is valid
        if (cacheTime > 0)
        {
          Date storeTime = readStorageItemStorageTimeFromLocalStorage(item);
          if (storeTime != null)
          { // there was a time available, so compare it
            Date nowTime = new Date();
            if (nowTime.getTime() - (cacheTime * 1000) > storeTime.getTime())
            { // elapsed
              logger.log(Level.INFO, "Local StorageItem time elapsed" + item.getLogId());
              return false;
            }
          }
        }
        logger.log(Level.INFO, "Local readStorageItem complete" + item.getLogId());
        return true;
      } else
      {
        logger.log(Level.INFO, "Local readStorageItem not found" + item.getLogId());
      }
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Exception local readStorageItem" + item.getLogId(), ex);
    }
    return false;
  }

  /**
   * Write the items Date/Time store value to html5 storage for later usage in
   * relation to the cache time
   * 
   * @param item The storage time for the given item (ID) is written to the
   *          key-value HTML5 storage.
   * @return false if the read operation failed or nothing is found.
   */
  private void writeStorageItemStorageTimeToLocalStorage(StorageItem item)
  {
    if (item == null || item.getStorageItemTimeKey() == null) return;
    try
    {
      String saveTime = DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_FULL).format(new Date());
      getLocalStorage().setItem(item.getStorageItemTimeKey(), saveTime);
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Exception local writeStorageItem time" + item.getLogId(), ex);
    }
  }

  /**
   * Read the items Date/Time store value from html5 storage.
   * 
   * @param item The time when a certain StorageItem was written to the HTML5
   *          key-value storage is read.
   * @return null if the read operation failed or nothing is found.
   */
  private Date readStorageItemStorageTimeFromLocalStorage(StorageItem item)
  {
    if (item == null || item.getStorageItemTimeKey() == null) return null;
    try
    {
      String val = getLocalStorage().getItem(item.getStorageItemTimeKey());
      if (val != null)
      {
        return DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_FULL).parse(val);
      }
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Exception local readStorageItem time" + item.getLogId(), ex);
    }
    return null;
  }

  /**
   * Remove all StorageItem related keys from the LocalStorage, thus clear the
   * cached json objects and references etc.
   */
  public void clearStorageItems()
  {
    try
    {
      Storage storage = this.getLocalStorage();
      if (storage == null) return;
      Integer len = storage.getLength();
      int index = 0;
      for (int i = 0; i < len; i++)
      {
        String key = storage.key(index);
        if (StorageItem.isStorageItemKey(key))
        {
          logger.log(Level.INFO, "Remove cached StorageItem:" + key);
          storage.removeItem(key);
        } else
        {
          index++;
        }
      }
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Execption clearing StorageItems", ex);
    }
  }

  /****************************************************************************************************************
   * Read / Write StorageItems to local Files
   ****************************************************************************************************************/

  /**
   * Write the item to a local file
   * 
   * @param item The StorageItem to be stored to the file system within the
   *          defined cache directory
   * @param callback Is called once the asynch action completed or failed.
   * @return false if the asynchronous action invocation failed.
   */
  public boolean writeStorageItemToLocalFile(final StorageItem item, final Callback<StorageItem, StorageError> callback)
  {
    if (item == null) return false;
    try
    {
      logger.log(Level.INFO, "local writeStorageItem invoked " + item.toString());
      return getLocalFileReference(getCacheDirectory(), item.getJsonFileName(), true, new FileCallback<FileEntry, StorageError>()
      {
        @Override
        public void onSuccess(FileEntry entry)
        {
          logger.log(Level.INFO, "local writeStorageItem FileEntry successfully retrieved" + item.getLogId());
          // store the file content
          writeStorageItemToLocalFile(entry, item, callback);
        }

        @Override
        public void onFailure(StorageError error)
        {
          logger.log(Level.SEVERE, "Failure local writeStorageItem FileSystem creation" + item.getLogId() + " " + error.toString());
        }
      });
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Exception local writeStorageItem " + item.getLogId(), ex);
      if (callback != null)
      {
        callback.onFailure(new StorageError(FileError.ABORT_ERR));
      }
    }
    return false;
  }

  /**
   * Write the item to the local fileentry asynchronous
   * 
   * @param fileEntry
   * @param item
   * @param callback Is called once the asynch action completed or failed
   * @return false if the asynchronous action invocation failed.
   */
  private boolean writeStorageItemToLocalFile(FileEntry fileEntry, final StorageItem item, final Callback<StorageItem, StorageError> callback)
  {
    if (item == null) return false;
    try
    {
      logger.log(Level.INFO, "writeStorageItem to local file invoked" + item.getLogId());
      fileEntry.createWriter(new FileCallback<FileWriter, FileError>()
      {
        @Override
        public void onSuccess(FileWriter writer)
        {
          writer.setOnWriteEndCallback(new WriterCallback<FileWriter>()
          {
            @Override
            public void onCallback(FileWriter result)
            {
              // file written
              logger.log(Level.INFO, "writeToLocalFile successfully written" + item.getLogId());
              if (callback != null)
              {
                callback.onSuccess(item);
              }
            }
          });
          writer.setOnErrorCallback(new WriterCallback<FileWriter>()
          {
            @Override
            public void onCallback(FileWriter result)
            {
              // Error while writing file
              logger.log(Level.SEVERE, "Failure file write StorageItem" + item.getLogId() + " : " + result.toString());
              if (callback != null)
              {
                callback.onFailure(new StorageError(result.getError()));
              }
            }
          });
          JSONValue json = item.toJson();
          writer.write(json.toString());
        }

        @Override
        public void onFailure(FileError error)
        {
          // can not create writer
          logger.log(Level.SEVERE, "Failure file writer creation StorageItem" + item.getLogId() + " : " + error.toString());
          if (callback != null)
          {
            callback.onFailure(new StorageError(error));
          }
        }
      });
      return true;
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Exception file write StorageItem" + item.toString(), ex);
      if (callback != null)
      {
        callback.onFailure(new StorageError(FileError.ABORT_ERR));
      }
    }
    return false;
  }

  /**
   * Read the item from the Local File storage
   * 
   * @param item The StorageItem (or inherited objects) to be read from the
   *          local cache file system location.
   * @param callback Is called once the asynch action completed or failed
   * @return false if the asynchronous action invocation failed.
   */
  public boolean readStorageItemFromLocalFile(final StorageItem item, final Callback<StorageItem, StorageError> callback)
  {
    if (item == null) return false;
    try
    {
      // get the file reference
      return getLocalFileReference(getCacheDirectory(), item.getJsonFileName(), false, new FileCallback<FileEntry, StorageError>()
      {
        @Override
        public void onSuccess(FileEntry entry)
        {
          logger.log(Level.INFO, "StorageItem File successfully retrieved" + item.getLogId());
          readStorageItemFromLocalFile(entry, item, callback);
        }

        @Override
        public void onFailure(StorageError error)
        {
          logger.log(Level.SEVERE, "Failure LocalFileReference retrieval" + item.getLogId() + " : " + error.toString());
          if (callback != null)
          {
            callback.onFailure(error);
          }
        }
      });
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Exception file write StorageItem" + item.getLogId(), ex);
      if (callback != null)
      {
        callback.onFailure(new StorageError(FileError.ABORT_ERR));
      }
    }
    return false;
  }

  /**
   * Read the StorageItem from the given FileEntry and refresh the given
   * CommonView
   * 
   * @param fileEntry
   * @param item
   * @param callback Is called once the asynch action completed or failed
   * @return false if the asynchronous action invocation failed.
   */
  private boolean readStorageItemFromLocalFile(FileEntry fileEntry, final StorageItem item, final Callback<StorageItem, StorageError> callback)
  {
    if (item == null) return false;
    try
    {
      // logger.log(Level.INFO,"readStorageItem from local file invoked" +
      // item.getLogId());
      FileReader reader = phonegap.getFile().createReader();
      reader.setOnloadCallback(new ReaderCallback<FileReader>()
      {
        @Override
        public void onCallback(FileReader result)
        {
          String json = result.getResult();
          // do something with the content
          item.fromJson(json);
          logger.log(Level.INFO, "readStorageItem from local file load completed for item" + item.getLogId());
          if (callback != null)
          {
            callback.onSuccess(item);
          }
        }
      });
      reader.setOnErrorCallback(new ReaderCallback<FileReader>()
      {
        @Override
        public void onCallback(FileReader result)
        {
          // error while reading file...
          logger.log(Level.SEVERE, "Error StorageItem file writer reading" + item.getLogId() + " : " + result.toString());
          if (callback != null)
          {
            callback.onFailure(new StorageError(result.getError()));
          }
        }
      });
      return true;
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Exception file read StorageItem" + item.getLogId(), ex);
      if (callback != null)
      {
        callback.onFailure(new StorageError(FileError.ABORT_ERR));
      }
    }
    return false;
  }

  /**
   * Our reference to the file system
   */
  private FileSystem fileSystem = null;

  /**
   * Retrieve the FileEntry Reference on the local filesystem of the device if
   * running in a local container eg. Phonegap
   * 
   * @param directory
   * @param filename
   * @param callback is called once the asynch action completed or failed
   * @return false if the asynchronous action invocation failed.
   */
  private boolean getFileSystem(final FileCallback<FileSystem, StorageError> callback)
  {
    try
    {
      if (!phonegap.isPhoneGapDevice()) return false;
      if (fileSystem != null)
      {
        if (callback != null)
        {
          callback.onSuccess(fileSystem);
        }
        return true;
      }
      logger.log(Level.INFO, "getFileReference - Request Local File System");
      phonegap.getFile().requestFileSystem(FileSystem.LocalFileSystem_PERSISTENT, 0, new FileCallback<FileSystem, FileError>()
      {
        @Override
        public void onSuccess(FileSystem entry)
        {
          logger.log(Level.INFO, "FileSystem retrieved");
          fileSystem = entry;
          if (callback != null)
          {
            callback.onSuccess(fileSystem);
          }
        }

        @Override
        public void onFailure(FileError error)
        {
          logger.log(Level.SEVERE, "Failure filesystem retrieval " + error.toString());
          if (callback != null)
          {
            callback.onFailure(new StorageError(error));
          }
        }
      });
      return true;
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "General failure FileSystem retrieval", ex);
      if (callback != null)
      {
        callback.onFailure(new StorageError(FileError.ABORT_ERR));
      }
    }
    return false;
  }

  
  /**
   * Retrieve the FileEntry Reference on the local filesystem of the device if
   * running in a local container eg. Phonegap
   * 
   * @param directory
   * @param filename
   * @param callback is called once the asynch action completed or failed
   * @return false if the asynchronous action invocation failed.
   */
  public boolean getLocalFileReference(final String directory, final String filename, final boolean create, final FileCallback<FileEntry, StorageError> callback)
  {
    try
    {
      if (!phonegap.isPhoneGapDevice()) return false;
      getLocalDirectoryEntry(directory,new FileCallback<DirectoryEntry, StorageError>()
      {
        @Override
        public void onSuccess(DirectoryEntry directoryEntry)
        {
          directoryEntry.getFile(filename, new Flags(create, false), new FileCallback<FileEntry, FileError>()
          {
            @Override
            public void onSuccess(FileEntry entry)
            {
              logger.log(Level.INFO, "getLocalFileReference - File retrieved : " + filename);
              if (callback != null)
              {
                callback.onSuccess(entry);
              }
            }
            @Override
            public void onFailure(FileError error)
            {
              logger.log(Level.SEVERE, "Failure file retrieval " + filename + " " + error.toString());
              if (callback != null)
              {
                callback.onFailure(new StorageError(error));
              }
            }
          });
        }
        @Override
        public void onFailure(StorageError error)
        {
          logger.log(Level.SEVERE, "Failure filesystem retrieval " + error.toString());
          callback.onFailure(error);
        }
      });
      return true;
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "General failure directory/file creator", ex);
      if (callback != null)
      {
        callback.onFailure(new StorageError(FileError.ABORT_ERR));
      }
    }
    return false;
  }

  
  private String lastLocalDirectory = null;
  private DirectoryEntry lastLocalDirectoryEntry = null;
  /**
   * Retrieve the FileEntry Reference on the local filesystem of the device if
   * running in a local container eg. Phonegap
   * 
   * @param directory The directory which we want to get a reference for. It will be created if it doesn't exist yet. 
   *                  It is based on the Filesystem reference.
   * @param callback is called once the asynch action completed or failed
   * @return false if the asynchronous action invocation failed.
   */
  private boolean getLocalDirectoryEntry(final String directory, final FileCallback<DirectoryEntry, StorageError> callback)
  {
    try
    {
      if (!phonegap.isPhoneGapDevice()) return false;
      if (lastLocalDirectory!=null && lastLocalDirectoryEntry !=null)
      {
        if (directory.equals(lastLocalDirectory))
        {
          if (callback != null)
          {
            callback.onSuccess(lastLocalDirectoryEntry);
          }
          return true;
        }
      }
      getFileSystem(new FileCallback<FileSystem, StorageError>()
      {
        @Override
        public void onSuccess(FileSystem entry)
        {
          logger.log(Level.INFO, "getLocalDirectoryEntry - FileSystem retrieved");
          final DirectoryEntry root = entry.getRoot();
          root.getDirectory(directory, new Flags(true, false), new FileCallback<DirectoryEntry, FileError>()
          {
            @Override
            public void onSuccess(final DirectoryEntry dirEntry)
            {
              logger.log(Level.INFO, "getLocalDirectoryEntry - Directory retrieved : " + directory);
              lastLocalDirectory = directory;
              lastLocalDirectoryEntry = dirEntry;
              if (callback != null)
              {
                callback.onSuccess(dirEntry);
              }
            }

            @Override
            public void onFailure(FileError error)
            {
              logger.log(Level.SEVERE, "Failure directory retrieval " + directory + " : " + error.toString() + " : " + error.getErrorCode());
              if (callback != null)
              {
                callback.onFailure(new StorageError(error));
              }
            }
          });
        }

        @Override
        public void onFailure(StorageError error)
        {
          logger.log(Level.SEVERE, "Failure filesystem retrieval " + error.toString() + " : " + error.getErrorCode());
          if (callback != null)
          {
            callback.onFailure(error);
          }
        }
      });
      return true;
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Exception in getLocalDirectory : " + directory, ex);
      if (callback != null)
      {
        callback.onFailure(new StorageError(FileError.ABORT_ERR));
      }
    }
    return false;
  }

  /****************************************************************************************************************
   * Read StorageItem from URL path
   ****************************************************************************************************************/

  /**
   * Retrieve the item from the storage. First try local storage, if the version
   * and the validTime are valid, it is returned. Else it tries to retrieve it
   * from the remote backend. If found, it is cached locally for fast access.
   * 
   * @param item The item to be read by ID from 1. the cache, 2. localAppPath 3.
   *          remoteAppPath in this priority order
   * @param useCache If true, the system will first try to retrieve the value
   *          from the local cache before it reads the same from the
   *          applications path
   * @param validTime The maximum age in seconds of the cache to be accepted as
   *          a valid item value, if elapsed it will try to read from the
   *          applications path / If <= 0 don't care
   * @param expectedVersion The versionNumber which must be available in the
   *          cache to be a valid cache item. If <=0 don't care.
   */
  public boolean readStorageItem(final StorageItem item, boolean useCache, int expectedVersion, int validTime, final Callback<StorageItem, StorageError> callback)
  {
    try
    {
      logger.log(Level.INFO, "readStorageItem" + item.getLogId());
      if (useCache && this.getCacheEnabled())
      { // retrieve the item first from local storage cache
        if (this.readStorageItemFromLocalStorage(item, expectedVersion, validTime))
        { // found it valid in the cache
          callback.onSuccess(item);
          return true;
        }
      }
      // didn't found a matching item in the cache yet or version mismatch or
      // cache time elapsed
      if (phonegap.isPhoneGapDevice())
      {
        // we run in a locally installed app and want to retrieve now the value
        // from the given backend
        return this.readStorageItemFromRemoteApplication(item, callback);
      } else
      { // in the case of web app, load it from the applications relative base path
        // this is automatically from the backend server where the app was
        // loaded from
        return this.readStorageItemFromLocalApplication(item, callback);
      }
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Exception readStorageItem" + item.getLogId(), ex);
      if (callback != null)
      {
        callback.onFailure(new StorageError(FileError.ABORT_ERR));
      }
    }
    return false;
  }

  /**
   * Read the item asynch from the applications own source server with the given
   * credentials and base path eg.
   * /storage/v1/ch.gbrain.testapp.model.items-1.json Note: We just give the
   * local relative url which means: - If running as local application (eg.
   * started locally in Browser) it will load from local base - If running as
   * web application it will load from the servers base - If running as Phonegap
   * app it will load from the local installed base
   * 
   * @param item
   * @param callback is called once the asynch action completed or failed
   * @return false if the asynchronous action invocation failed.
   */
  public boolean readStorageItemFromLocalApplication(final StorageItem item, final Callback<StorageItem, StorageError> callback)
  {
    // we run in the web directly and therefore we read it directly from the
    // application relative storage in the Webapp itself
    logger.log(Level.INFO, "Read StorageItem from local applications base" + item.getLogId());
    return readStorageItemFromUrl(this.getLocalStorageUrl(), item, getReadStorageItemHandler(item, callback, null));
    // for testing in browser use this. But Chrome must run without security to
    // work
    // return readFromUrl(this.appRemoteStorageUrl,item,callback);
  }

  /**
   * Compare the given versions.
   * 
   * @param expectedVersion The version we do expect at least / if <=0, we don't
   *          care about versions, it is always ok
   * @param realVersion The real version of the item
   * @return true if the realversion >= expectedVersion
   */
  private boolean checkStorageItemVersion(int expectedVersion, int realVersion)
  {
    if (expectedVersion <= 0) return true; // the version doesn't care
    if (realVersion >= expectedVersion) return true;
    return false;
  }

  /**
   * Read the item asynch from the configured remote application base
   * 
   * @param item
   * @param callback is called once the asynch action completed or failed
   * @return false if the asynchronous action invocation failed.
   */
  public boolean readStorageItemFromRemoteApplication(final StorageItem item, final Callback<StorageItem, StorageError> callback)
  {
    logger.log(Level.INFO, "Read StorageItem from remote application base" + item.getLogId());
    return readStorageItemFromUrl(this.getRemoteStorageUrl(), item, getReadStorageItemHandler(item, callback, this.getLocalStorageUrl()));
  }

  /**
   * Creates and returns a Callback which treats the result for a url Item
   * retrieval
   * 
   * @param item The StorageItem (or a inheriting object) which must be read
   *          (filled in with the retrieved data)
   * @param callback The final resp. initial callback to be notified of the
   *          result
   * @param fallBack A URL to which a further request must be done if the call
   *          fails
   * @return The callback which deals with the asynch result of the remote item
   *         retrieval
   */
  private Callback<StorageItem, StorageError> getReadStorageItemHandler(final StorageItem item, final Callback<StorageItem, StorageError> callback, final String fallbackUrl)
  {
    return new Callback<StorageItem, StorageError>()
    {
      public void onSuccess(StorageItem newItem)
      { // loading succeeded
        // store it in the cache
        logger.log(Level.INFO, "Completed read item from url" + item.getLogId());
        writeStorageItemToLocalStorage(newItem);
        callback.onSuccess(newItem);
      }

      public void onFailure(StorageError error)
      {
        logger.log(Level.WARNING, "Failure url loading" + item.getLogId());
        // nothing found, check if we must retrieve it from a remote location
        if (fallbackUrl != null && !fallbackUrl.isEmpty())
        {
          readStorageItemFromUrl(fallbackUrl, item, getReadStorageItemHandler(item, callback, null));
        } else
        {
          callback.onFailure(error);
        }
      }
    };
  }

  /**
   * Read the JSON item asynch from the given url and the
   * standard name of the item eg. ch.gbrain.testapp.model.items-1.json
   * 
   * @param url The url to read from eg. for local application relative path
   *          "storage/v1/" eg. for remote location
   *          "http://host.domain.ch/testapp/storage/v1/"
   * @param item
   * @param callback is called once the asynch action completed or failed
   * @return false if the asynchronous action invocation failed and no callback will be invoked
   */
  public boolean readStorageItemFromUrl(String url, final StorageItem item, final Callback<StorageItem, StorageError> callback)
  {
    if (item == null) return false;
    try
    {
      Resource resource = new Resource(url + item.getJsonFileName() + "?noCache=" + new Date().getTime());
      Method method = resource.get();
      /**
       * if (username.isEmpty()) { method = resource.get(); }else { method =
       * resource.get().user(username).password(password); }
       */
      logger.log(Level.INFO, "Read from url:" + method.builder.getUrl());
      method.send(new JsonCallback()
      {
        public void onSuccess(Method method, JSONValue response)
        {
          logger.log(Level.INFO, "Read from url success");
          if (response != null)
          {
            try
            {
              logger.log(Level.INFO, "Successfully url read" + item.getLogId());
              item.fromJson(response);
              if (callback != null)
              {
                callback.onSuccess(item);
              }
            } catch (Exception ex)
            {
              logger.log(Level.SEVERE, "Failure url read" + item.getLogId(), ex);
            }
          }
        }

        public void onFailure(Method method, Throwable exception)
        {
          logger.log(Level.WARNING, "Failure url read" + item.getLogId(), exception);
          if (callback != null)
          {
            callback.onFailure(new StorageError(FileError.NOT_READABLE_ERR, exception.getMessage()));
          }
        }
      });
      logger.log(Level.INFO, "Read from url call complete");
      return true;
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Error url read" + item.getLogId(), ex);
      if (callback != null)
      {
        callback.onFailure(new StorageError(FileError.ABORT_ERR));
      }
    }
    return false;
  }

  /****************************************************************************************************************
   * Read Resource from URL path
   ****************************************************************************************************************/

  /**
   * Retrieve the url of the resource on the remote server based on the current
   * runtime environement
   * 
   * @param relativeUrl relative url of the resource according the app base
   * @return The url pointing to the resource dependent on if it is running in
   *         Phonegap container or Webbrowser
   *
   */
  public String getRemoteResourceUrl(String relativeUrl)
  {
    if (phonegap.isPhoneGapDevice())
    {
      return getRemoteAppBaseUrl() + relativeUrl;
    } else
    {
      return relativeUrl;
    }
  }

  /**
   * Evaluates in case of the runtime (browser/phonegap) the full url where a
   * resource must be retrieved from. In case of Phonegap, it will check if we
   * have the resource already locally stored in the cache and return a url
   * pointing to this one instead.
   * 
   * @param relativeUrl of the resource as it is available in the application
   *          itself.
   * @param version Check if the version of the stored resource equals. Not
   *          checked if version=0
   * @return true if the retrieval was invoked successfully, means you could expect a callback, false otherwise.
   */
  public boolean retrieveResourceUrl(final String relativeUrl, Integer version, final Callback<String, FileError> callback)
  {
    try
    {
      if (relativeUrl == null || relativeUrl.isEmpty())
      {
        if (callback != null)
        {
          logger.log(Level.INFO, "Web ResourceCacheReference retrieval impossible with invalid URL : " + relativeUrl);
          callback.onFailure(new StorageError(FileError.SYNTAX_ERR, "Invalid Url given : " + relativeUrl));
        }
        return false;
      }
      if (!phonegap.isPhoneGapDevice())
      {
        if (callback != null)
        {
          logger.log(Level.INFO, "Web ResourceCacheReference retrieval : " + relativeUrl);
          callback.onSuccess(relativeUrl);
        }
        return true;
      }
      // check if we have a cached resource (eg. with a corresponding cache item
      // in the storage)
      StorageResource resource = new StorageResource(relativeUrl, version, null);
      Boolean checkVersion = checkResourceVersion(resource);
      if (checkVersion == null)
      {
        logger.log(Level.INFO, "No resource cache item found for : " + relativeUrl + " / version:" + version);
        if (callback != null)
        {
          callback.onFailure(new StorageError(FileError.NOT_FOUND_ERR, "No resource cache item found"));
        }
      } else if (checkVersion == true)
      {
        // it should be there already and version is ok
        logger.log(Level.INFO, "Successful ResourceCacheReference retrieval : " + relativeUrl + " / version=" + version);
        getCacheDirectoryEntry(new Callback<DirectoryEntry, StorageError>()
        {
          public void onSuccess(DirectoryEntry dirEntry)
          {
            if (callback != null)
            {
              String localResourceUrl = dirEntry.toURL() + "/" + convertFilePathToFileName(relativeUrl);
              logger.log(Level.INFO, "Successful ResourceCacheUrl evaluation : " + localResourceUrl);
              callback.onSuccess(localResourceUrl);
            }
          }

          public void onFailure(StorageError error)
          {
            logger.log(Level.WARNING, "Failure in ResourceCacheUrl evaluation : " + relativeUrl + " error:" + error.getErrorCode());
            if (callback != null)
            {
              callback.onFailure(error);
            }
          }
        });
        return true;
      } else
      {
        logger.log(Level.INFO, "No matching resource cache item found for : " + relativeUrl + "version:" + version);
      }
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Exception resourceUrl evaluation for : " + relativeUrl, ex);
    }
    return false;
  }

  
  private static String CACHEFILEPATHDELIMITER = "@@";
  /**
   * Create from a url a proper filename which could be stored in the filesystem
   * 
   * @param filePath
   * @return The filename with all problematic characters replaced with working
   *         ones.
   */
  public static String convertFilePathToFileName(String filePath)
  {
    return filePath.replace("/", CACHEFILEPATHDELIMITER);
  }

  public static String extractFileNameFromCacheFile(String cachedFileName)
  {
    try
    {
      int pos = cachedFileName.indexOf(CACHEFILEPATHDELIMITER);
      if (pos>=0)
      {
        return cachedFileName.substring(pos+2);
      }
    }catch(Exception ex)
    {
      //
    }
    return cachedFileName;
  }

  /**
   * Check if the version registered in the cache does match this resources
   * version
   * 
   * @return true if the version matches the cache, false if not. If there was
   *         no cache yet, returns null
   */
  protected Boolean checkResourceVersion(StorageResource resource)
  {
    try
    {
      // check if we have a cached resource (eg. with a corresponding cache item
      // in the storage)
      String cachedResourceVersion = getLocalStorage().getItem(resource.getResourceVersionKey());
      if (cachedResourceVersion != null)
      {
        Integer cachedVersion = Integer.parseInt(cachedResourceVersion);
        Integer resourceVersion = resource.getVersion();
        if (resourceVersion == null || resourceVersion == 0 || cachedVersion == resourceVersion)
        {
          return true;
        }
        logger.log(Level.WARNING, "Resource version mismatch:" + resource.getResourceUrl() + " version:" + resource.getVersion() + " cachedVersion:" + cachedResourceVersion);
        return false;
      }
      // there was obviously no cache
      return null;
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Exception checking resource version:" + resource.getResourceUrl() + " version:" + resource.getVersion(), ex);
    }
    // something went wrong, we have not found a compatible version therefore
    return false;
  }

  /**
   * Remove all ResourceItem related keys from the LocalStorage and as well
   * related resource files, ClearCache
   */
  public void clearResourceItems()
  {
    try
    {
      Storage storage = this.getLocalStorage();
      if (storage == null) return;
      Integer len = storage.getLength();
      Integer index = 0;
      for (int i = 0; i < len; i++)
      {
        String key = storage.key(index);
        if (StorageResource.isResourceIdKey(key))
        {
          logger.log(Level.INFO, "Remove cached ResourceId : " + key);
          final String fullFileUrl = storage.getItem(key);
          storage.removeItem(key);
          // now remove the corresponding file asynch
          phonegap.getFile().resolveLocalFileSystemURI(fullFileUrl, new FileCallback<EntryBase, FileError>()
          {
            @Override
            public void onSuccess(EntryBase entry)
            {
              try
              {
                logger.log(Level.INFO, "Remove resource file:" + entry.getAsFileEntry().getFullPath());
                entry.getAsFileEntry().remove(new FileCallback<Boolean, FileError>()
                {
                  @Override
                  public void onSuccess(Boolean entry)
                  {
                    logger.log(Level.INFO, "Successfully deleted file:" + fullFileUrl);
                  }

                  @Override
                  public void onFailure(FileError error)
                  {
                    logger.log(Level.WARNING, "Unable to delete File:" + fullFileUrl + " error:" + error.getErrorCode());
                  }
                });
              } catch (Exception successEx)
              {
                logger.log(Level.WARNING, "Remove resource file failed:" + entry.getAsFileEntry().getFullPath(), successEx);
              }
            }

            @Override
            public void onFailure(FileError error)
            {
              logger.log(Level.WARNING, "Unable to locate File for deletion:" + fullFileUrl + " error:" + error.getErrorCode());
            }
          });
        } else if (StorageResource.isResourceVersionKey(key))
        {
          logger.log(Level.INFO, "Remove cached ResourceVersion : " + key);
          storage.removeItem(key);
        } else
        {
          index++;
        }
      }
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Execption clearing Resources", ex);
    }
  }

  private DirectoryEntry cacheDirectoryEntry = null;

  public boolean getCacheDirectoryEntry(final Callback<DirectoryEntry, StorageError> callback)
  {
    try
    {
      if (cacheDirectoryEntry != null && callback != null)
      {
        callback.onSuccess(cacheDirectoryEntry);
        return true;
      }
      if (!phonegap.isPhoneGapDevice()) return false;
      String cacheDir = getCacheDirectory();
      getLocalDirectoryEntry(cacheDir, new FileCallback<DirectoryEntry, StorageError>()
      {
        @Override
        public void onSuccess(DirectoryEntry entry)
        {
          logger.log(Level.INFO, "CacheDirectory successfully retrieved with path:" + entry.getFullPath());
          cacheDirectoryEntry = entry;
          if (callback != null)
          {
            callback.onSuccess(entry);
          }
        }

        @Override
        public void onFailure(StorageError error)
        {
          logger.log(Level.SEVERE, "Failure Cache FileSystem Directory retrieval" + " : " + error.toString());
          // stop the whole stuff, it doesn't work at all, we don't continue
          // here. Caching will not work therefore
          if (callback != null)
          {
            callback.onFailure(error);
          }
        }
      });
      return true;
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Exception Cache FileSystem Directory retrieval", ex);
    }
    return false;
  }


  /**
   * Check first if the resource with the given url and version is already
   * present, if not try to download the same in a sequential way asynchronously
   * 
   * @param relativeUrl The relative url to the resource from the apps base path
   * @param version The requested resource version
   * @param callback Callback called once the resource was downloaded / or is available local at all
   * @return false if no resource retrieval is invoked really and therefore, no downloadNotification callback will happen.
   */
  public boolean addResourceToCache(final String relativeUrl, final Integer version, final FileDownloadCallback downloadNotification)
  {
    try
    {
      if (!this.isResourceCachingEnabled()) return false;
      if (relativeUrl == null || relativeUrl.isEmpty()) return false;
      StorageResource resource = new StorageResource(relativeUrl, version, downloadNotification);
      StorageResourceCollector collector = new StorageResourceCollector(this,resource);
      Scheduler.get().scheduleDeferred(collector);
      return true;
    }catch(Exception ex)
    {
      logger.log(Level.SEVERE, "Exception adding ResourceToCache", ex);
    }
    return false;
  }
  
  
  /**
   * Clear all cached items - key/value pairs in the LocalStorage - Related
   * files in the cache directory
   */
  public void clearStorage()
  {
    try
    {
      this.clearResourceItems();
      this.clearStorageItems();
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Exception on Cache clearing", ex);
    }
  }

  /**
   * Retrieve all ResourceItems related keys from the LocalStorage
   * 
   * @return The number of resources which will be evaluated and for which
   *         callbacks have to be expected
   */
  public int getAllCachedResourceItems(final Callback<StorageInfo, FileError> callback)
  {
    int resCtr=0;
    try
    {
      if (!this.isResourceCachingEnabled()) return 0;
      if (callback == null) return 0;
      logger.log(Level.INFO, "getAllCachedResourceItems");
      Storage storage = this.getLocalStorage();
      int len = storage.getLength();
      for (int i = 0; i < len; i++)
      {
        String key = storage.key(i);
        if (StorageResource.isResourceIdKey(key))
        {
          logger.log(Level.INFO, "Read cached Resource : " + key);
          StorageInfoCollector collector = new StorageInfoCollector(this,key,callback);
          Scheduler.get().scheduleDeferred(collector);
          resCtr++;
        }
      }
      return resCtr;
    } catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Execption reading all cached Resources", ex);
    }
    return resCtr;
  }


}
