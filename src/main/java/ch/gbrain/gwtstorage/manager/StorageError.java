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

import com.googlecode.gwtphonegap.client.file.FileError;
import com.googlecode.gwtphonegap.client.file.FileTransferError;
import com.googlecode.gwtphonegap.client.file.browser.FileErrorException;

public class StorageError extends FileErrorException
{

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public StorageError(FileError fileError, String reason)
  {
    super(fileError.getErrorCode());
    errorReason = reason;
  }

  public StorageError(FileError fileError)
  {
    super(fileError.getErrorCode());
  }

  public StorageError(int fileError)
  {
    super(fileError);
  }

  public StorageError(int fileError, String reason)
  {
    super(fileError);
    errorReason = reason;
  }

  public StorageError(FileTransferError error)
  {
    super(FileError.NOT_FOUND_ERR);
    this.errorReason = error.toString();
  }

  private String errorReason = "";

  public String getErrorReason()
  {
    return errorReason;
  }

}
