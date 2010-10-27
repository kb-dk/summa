package dk.statsbiblioteket.summa.storage.api.filter;

import dk.statsbiblioteket.summa.storage.StorageTestBase;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;

import java.io.IOException;

import junit.framework.TestCase;

public class StorageValidationFilterTest extends TestCase {
    
    public void testConstrution() {
        try {
            // No storage running should result in an exception
            new StorageValidationFilter(StorageTestBase.createConf());
            fail("Exception is expected");
        } catch (IOException e) {
            // should happen
        } catch (Exception e) {
            fail("Not expected to throw exception when creating configuration");
        }
        try {
            Storage storage =
                     StorageFactory.createStorage(StorageTestBase.createConf());
            assertNotNull(storage);
            StorageValidationFilter storageValidationFilter = 
                      new StorageValidationFilter(StorageTestBase.createConf());
            // Expected
            assertNotNull(storageValidationFilter);
        } catch (IOException e) {
            fail("No exception is expected here");
        } catch (Exception e) {
            fail("Not expected to fail when creating configuratino");
        }
        
    }
}
