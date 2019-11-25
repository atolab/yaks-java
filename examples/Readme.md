# Yaks Java examples

## Build instructions

   ```bash
   mvn package
   ```

## Start instructions
   
   The jar file produces by the Maven build is standalone (i.e. it contains all the required dependencies).
   You can run any example Java class with such command:

   ```bash
   java -cp target/yaks-examples-<version>.jar <classname> <args>
   ```

## Examples description

### YAddStorage

   Add a storage in the Yaks service it's connected to.

   Usage:
   ```bash
   java -cp target/yaks-examples-<version>.jar YAddStorage [selector] [storage-id] [locator]
   ```
   where the optional arguments are:
   - **selector** :  the selector matching the keys (path) that have to be stored.  
                     Default value: `/demo/example/**`
   - **storage-id** : the storage identifier.  
                      Default value: `Demo` 
   - **locator** : the locator of the Yaks service to connect.  
                   Default value: none, meaning the Yaks service is found via multicast.

   Note that his example doesn't specify the Backend that Yaks has to use for storage creation.  
   Therefore, Yaks will automatically select the memory backend, meaning the storage will be in memory
   (i.e. not persistent).

### YPut

   Put a key/value into Yaks.  
   The key/value will be stored by all the storages with a selector that matches the key.
   It will also be received by all the matching subscribers (see [YSub](#YSub) below).  
   Note that if no storage and no subscriber are matching the key, the key/value will be dropped.
   Therefore, you probably should run [YAddStorage](#YAddStorage) and/or [YSub](#YSub) before YPut.

   Usage:
   ```bash
   java -cp target/yaks-examples-<version>.jar YPut [path] [value] [locator]
   ```
   where the optional arguments are:
   - **path** : the path used as a key for the value.  
                Default value: `/demo/example/yaks-java-put` 
   - **value** : the value (as a string).  
                Default value: `"Put from Yaks Java!"` 
   - **locator** : the locator of the Yaks service to connect.  
                   Default value: none, meaning the Yaks service is found via multicast.

### YGet

   Get a list of keys/values from Yaks.  
   The values will be retrieved from the Storages containing paths that match the specified selector.  
   The Eval functions (see [YEval](#YEval) below) registered with a path matching the selector
   will also be triggered.

   Usage:
   ```bash
   java -cp target/yaks-examples-<version>.jar YAddStorage [selector] [locator]
   ```
   where the optional arguments are:
   - **selector** : the selector that all replies shall match.  
                    Default value: `/demo/example/**` 
   - **locator** : the locator of the Yaks service to connect.  
                   Default value: none, meaning the Yaks service is found via multicast.


### YRemove

   Remove a key and its associated value from Yaks.  
   Any storage that store the key/value will drop it.  
   The subscribers with a selector matching the key will also receive a notification of this removal.

   Usage:
   ```bash
   java -cp target/yaks-examples-<version>.jar YRemove [path] [locator]
   ```
   where the optional arguments are:
   - **path** : the key to be removed.  
                Default value: `/demo/example/yaks-java-put` 
   - **locator** : the locator of the Yaks service to connect.  
                   Default value: none, meaning the Yaks service is found via multicast.

### YSub

   Register a subscriber with a selector.  
   The subscriber will be notified of each put/remove made on any path matching the selector,
   and will print this notification.

   Usage:
   ```bash
   java -cp target/yaks-examples-<version>.jar YSub [selector] [locator]
   ```
   where the optional arguments are:
   - **selector** : the subscription selector.  
                    Default value: `/demo/example/**` 
   - **locator** : the locator of the Yaks service to connect.  
                   Default value: none, meaning the Yaks service is found via multicast.

### YEval

   Register an evaluation function with a path.  
   This evaluation function will be triggered by each call to a get operation on Yaks 
   with a selector that matches the path. In this example, the function returns a string value.
   See the code for more details.

   Usage:
   ```bash
   java -cp target/yaks-examples-<version>.jar YSub [selector] [locator]
   ```
   where the optional arguments are:
   - **path** : the eval path.  
                Default value: `/demo/example/yaks-java-eval` 
   - **locator** : the locator of the Yaks service to connect.  
                   Default value: none, meaning the Yaks service is found via multicast.


### YPutThr & YSubThr

   Pub/Sub throughput test.
   This example allows to perform throughput measurements between a pubisher performing
   put operations and a subscriber receiving notifications of those put.
   Note that you can run this example with or without any storage.

   Publisher usage:
   ```bash
   java -cp target/yaks-examples-<version>.jar YPutThr [I|W]<payload-size> [locator]
   ```
   where the arguments are:
   - **[I|W]** : the way to allocate the java.util.ByteBuffer payload that will be put into Yaks.  
                 **I**: use a non-direct ByteBuffer (created via ByteBuffer.allocate())  
                 **W**: use a wrapped ByteBuffer (created via ByteBuffer.wrap())  
                 **unset**: by default use a direct ByteBuffer
   - **payload-size** : the size of the payload in bytes.  
   - **locator** : the locator of the Yaks service to connect.  
                   Default value: none, meaning the Yaks service is found via multicast.

   Subscriber usage:
   ```bash
   java -cp target/yaks-examples-<version>.jar YSubThr [locator]
   ```
   where the optional arguments are:
   - **locator** : the locator of the Yaks service to connect.  
                   Default value: none, meaning the Yaks service is found via multicast.
