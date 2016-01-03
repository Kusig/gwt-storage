# gwt-storage
GWT-Storage provides a layer above Phonegap file system and browsers local storage. 
It's main goal is to provide a local cache system for multimedia files used within a hybrid gwt phonegap application

## License
[Apache 2.0 License](https://github.com/GwtMaterialDesign/gwt-material/blob/master/LICENSE)


## Maven
```xml
<dependency>
    <groupId>ch.gbrain</groupId>
    <artifactId>gwt-storage</artifactId>
    <version>0.0.1</version>
</dependency>    
```


## JSON Resources
Inherit any JSON Resource from the base class StorageItem and implement toJson() and fromJson() method. See test cases
Of course you need as well to create for each moel class the RestyGWT JSON-Encoder/Decoder which is (can be) used within the toJson() / fromJson() methods.









