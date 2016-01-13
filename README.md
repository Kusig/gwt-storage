# gwt-storage
GWT-Storage provides a layer above Phonegap file system and browsers local storage. 
It's main goal is to provide a local cache system for multimedia files used within a hybrid gwt phonegap application

## License
[Apache 2.0 License](https://github.com/Kusig/gwt-storage/blob/master/LICENSE)


## Maven
```xml
<dependency>
    <groupId>ch.gbrain</groupId>
    <artifactId>gwt-storage</artifactId>
    <version>0.0.1</version>
</dependency>    
```


## Configuration
The StorageManager must be configured initially once in order to retrieve the 
resources from the right location dependent of the runtime.

1. If running as normal Webapp in the browser, the resources will be read always from the apps context path, thus from the Webserver.
2. If running in Phonegap, it will read the resource from the given RemoteAppBaseUrl  + StorageUrl

```java
StorageManager storage = injector.getStorageManager();
storage.setRemoteAppBaseUrl("http://www.gbrain.ch/testapp/");
storage.setCacheDirectory("testapp");
storage.setStorageUrl("storage/v1/");
```

The Json files stored at this location must have the name according the objects name in Java following the ID of the object. 

Example:
ch.gbrain.app.model.DomainItem-1.json

It must be retrievable from this address (concatenated AppBaseUrl + StorageUrl + Filename):
http://www.gbrain.ch/testapp/storage/v1/ch.gbrain.app.model.DomainItem-1.json

This means that you have in your app a class DomainItem which is inherited from StorageItem, implementing the toJson/fromJson methods
and with the id attribute set to "1" when reading the item through the StorageManager.readStorageItem(..) method.



## JSON Resources
Inherit any JSON Resource from the base class StorageItem and implement toJson() and fromJson() method. See test cases model for an example.
Of course you need as well to create for each moel class the RestyGWT JSON-Encoder/Decoder which is (can be) used within the toJson() / fromJson() methods.

Example toJson:
```java
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
```

Example fromJson:
Here, the returned JSON object must be mapped into the object itself in order to be treated correctly further.
```java
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
```


## Retrieve JSON resources
This will retrieve the resource from the applications remote location 
if we run in phonegap, if we run in the browser, it will read always from 
apps context path.

```java
{
  ...
  storageManager.readStorageItem(jsonItem, true, 1, 3600, getLoadHandler()); 
  ...
}

/**
  * Creates and returns a Callback to react once the json resource was 
  * properly retrieved
  * 
  * @return
  */
private Callback<StorageItem, StorageError> getLoadHandler()
{
  return new Callback<StorageItem, StorageError>()
  {
    public void onSuccess(StorageItem newItem)
    {
      logger.log(Level.INFO, "Success reading the item=" + items.getLogId());
      ....
      
    }
    public void onFailure(StorageError error)
    {
      logger.log(Level.WARNING, "Failure reading the item=" + items.getLogId());
    }
  };
}
```

## Read JSON Resoures from LocalStorage


## Write JSON Resources to LocalStorage


## Retrieve media resources



## Resolve cached files URL reference
Once you have downloaded any media resources, they are stored in the local cache directory. In order to 
retrieve the URL to this stored resource, you could call the following method. It will check if the resource 
is available and give you back the right URL dependent on the underlying runtime (browser / phonegap).

```java
// preset with remote path in any case, will be overwritten later in the callback if possible
imagePanel.setUrl(storageManager.getRemoteResourceUrl(item.getImage())); 
// invoke the resolver
storageManager.retrieveResourceUrl(item.getImage(),item.getVersion(), new Callback<String,FileError>()
{
   @Override
   public void onSuccess(String url)
   {
     imagePanel.setUrl(url);
   }
   @Override
   public void onFailure(FileError error)
   {
     logger.log(Level.WARNING,"Unable to retrieve Image resource " + item.getImage());
   }
});
```
