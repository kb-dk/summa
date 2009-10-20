import java.io.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Test some threading issues related to suspicious
 * exceptions. See http://sourceforge.net/apps/trac/summa/ticket/65
 */
public class UnzipThread extends Thread {

    /**********************************************
     * MAIN METHODS AND TEST SETUP CODE BELOW HERE
     **********************************************/

    /**
     * Async unzipping. Closing zip entries in alt. threads
     * @param args
     * @throws Exception
     */
    public static void main (String[] args) throws Exception {
        System.out.println("Building zip file for test");
        File zipFile = createZipFile();
        System.out.println("Zip file ready: " + zipFile);

        for (int i = 0; i < 10; i++) {
           new UnzipThread(zipFile).start();
        }
    }

    /**
     * Alternative test that simply handles the zip entries
     * in a very ugly manner
     * @param args
     * @throws Exception
     */
    public static void main2 (String[] args) throws Exception {
        System.out.println("Building zip file for test");
        File zipFile = createZipFile();
        System.out.println("Zip file ready: " + zipFile);

        byte[] buf = new byte[1024];
        ZipInputStream unzip = new ZipInputStream(
                new FileInputStream(zipFile));

        while (unzip.getNextEntry() != null) {
            int numRead, entrySize = 0;
            unzip.read(new byte[10]);
            unzip.closeEntry();
            int len = unzip.read(buf);
            System.out.println("Read " + len + " bytes after entry");
            while ((numRead = unzip.read(buf)) != -1) {
                entrySize += numRead;
            }


        }
        unzip.close();
    }

    public static File createZipFile() throws IOException {
        File file = File.createTempFile("unzip-thread-test-", ".zip");
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file));

        addZipEntry(zip, "test1.txt", 1024);
        addZipEntry(zip, "test2.txt", 10 * 1024 * 1024);
        addZipEntry(zip, "test3.txt", 10 * 1024 * 1024);
        addZipEntry(zip, "test4.txt", 1);
        addZipEntry(zip, "test5.txt", 1);
        addZipEntry(zip, "test6.txt", 1024 * 1024);

        zip.closeEntry();
        zip.close();

        return file;
    }

    public static void addZipEntry(
            ZipOutputStream zip, String name, int byteSize) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        for (int j = 0; j < byteSize/74 + 1; j++) {
            for (int i = 48; i < 123; i++) {
                zip.write(i);
            }
            zip.write('\n');
        }
    }

    /**********************************************
     * ACTUAL CLASS IMPL BELOW HERE
     **********************************************/

    public static final AsyncZipEntryCloser ASYNC_CLOSE_SERVICE;

    static {
        ASYNC_CLOSE_SERVICE = new AsyncZipEntryCloser();
        ASYNC_CLOSE_SERVICE.start();
    }

    File zipFile;

    public UnzipThread (File zipFile) {
        this.zipFile = zipFile;
    }

    public void run() {
        System.out.println("Thread " + getName() + " starting");

        try {
            byte[] buf = new byte[1024];
            for (int i = 0; i < 10; i++) {
                ZipInputStream unzip = new ZipInputStream(
                        new FileInputStream(zipFile));

                while (unzip.getNextEntry() != null) {
                    int numRead, entrySize = 0;
                    while ((numRead = unzip.read(buf)) != -1) {
                        entrySize += numRead;
                    }
                    ASYNC_CLOSE_SERVICE.awaitAsyncCloseEntry(unzip);
                }
                unzip.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Thread " + getName() + " done");
    }

    public static class AsyncZipEntryCloser extends Thread {

        BlockingQueue<ZipInputStream> queue =
                new ArrayBlockingQueue<ZipInputStream>(10);

        public AsyncZipEntryCloser() {
            setName(getClass().getSimpleName());
            setDaemon(true);
        }

        public void awaitAsyncCloseEntry(ZipInputStream unzip) {
            try {
                synchronized (unzip) {
                    queue.put(unzip);
                    unzip.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            System.out.println("Starting thread " + getName());

            try {
                while (true) {
                    ZipInputStream unzip = queue.take();
                    synchronized (unzip) {
                        unzip.closeEntry();
                        unzip.notifyAll();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Stopping thread " + getName());
        }
    }

}
