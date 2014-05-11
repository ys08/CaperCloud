/*
 * Copyright 2006-2010 James Murty
 * Copyright 2014 Yang Shuai
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package capercloud.Exception;

/**
 * A standard exception, used for errors specific to the Synchronize application.
 *
 * @author Yang Shuai
 */
public class SynchronizeException extends Exception {

    public SynchronizeException() {
        super();
    }

    public SynchronizeException(String message) {
        super(message);
    }

    public SynchronizeException(String message, Throwable cause) {
        super(message, cause);
    }

    public SynchronizeException(Throwable cause) {
        super(cause);
    }

}

