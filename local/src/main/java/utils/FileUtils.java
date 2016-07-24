package utils;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;

public class FileUtils {

    public enum FileExtension {
        TAR_BZ,
        TAR_BZ2;
    }

    private static void addFileToZip(String parentPath, File file,
            ZipOutputStream zip, HashSet<String> exclude) throws Exception {

        if (file.isDirectory())
            addFolderToZip(parentPath, file, zip, exclude);
        else {
            byte[] buf = new byte[1024];
            int len;
            FileInputStream in = new FileInputStream(file);
            zip.putNextEntry(
                    new ZipEntry(parentPath + File.separator + file.getName()));
            while ((len = in.read(buf)) > 0)
                zip.write(buf, 0, len);
            in.close();
        }
    }

    private static void addFolderToZip(String parentPath, File folder,
            ZipOutputStream zip, HashSet<String> exclude) throws Exception {
        for (File child : folder.listFiles())
            if (exclude == null || !exclude.contains(child.getCanonicalPath()))
                addFileToZip(parentPath + File.separator + folder.getName(),
                        child, zip, exclude);
    }

    private static void copyDirectory(File sourceDir, File destDir)
            throws IOException {

        if (!destDir.exists())
            destDir.mkdir();
        else if (!destDir.isDirectory()) {
            destDir.delete();
            destDir.mkdir();
        }

        for (File sourceChild : sourceDir.listFiles())
            if (!sourceChild.getAbsolutePath()
                    .equals(destDir.getAbsolutePath()))
                copyFile(sourceChild, new File(destDir, sourceChild.getName()));
    }

    public static void copyFile(File source, File dest) throws IOException {

        if (source.isDirectory()) {
            copyDirectory(source, dest);
            return;
        }

        FileInputStream fIn = new FileInputStream(source);
        FileOutputStream fOut = new FileOutputStream(dest);

        try {

            FileChannel fIChan = fIn.getChannel();
            FileChannel fOChan = fOut.getChannel();

            long fSize = fIChan.size();

            MappedByteBuffer mBuf = fIChan.map(FileChannel.MapMode.READ_ONLY,
                    0, fSize);

            fOChan.write(mBuf); // this copies the file

            fIChan.close();
            fIn.close();

            fOChan.close();
            fOut.close();

            dest.setExecutable(source.canExecute());
            dest.setReadable(source.canRead());
            dest.setWritable(source.canWrite());

        } finally {
            try {
                fIn.close();
                fOut.close();
            } catch (Exception e) {
            }
        }
    }

    public static boolean deleteFile(File oFile) {
        if (oFile.isDirectory())
            for (File oFileCur : oFile.listFiles())
                deleteFile(oFileCur);
        return oFile.delete();
    }

    public static boolean deleteFile(String filePath) {
        return deleteFile(new File(filePath));
    }

    public static void downloadFile(URL source, File destination)
            throws IOException {
        org.apache.commons.io.FileUtils.copyURLToFile(source, destination);
    }

    public static boolean exists(String fileName) {
        File f = new File(fileName);
        return f.exists();
    }

    public static String getExtension(String path) throws Exception {
        int lastIndexOf = path.lastIndexOf(".");

        if (lastIndexOf == -1)
            throw new Exception("No extension on path '" + path + "'");

        return path.substring(lastIndexOf + 1, path.length());
    }

    public static String getURLFileBaseName(String url) {
        return FilenameUtils.getBaseName(url);
    }

    public static String getURLFileName(String url) {
        return FilenameUtils.getName(url);
    }

    public static boolean isFileEmpty(String fileName) {
        return new File(fileName).length() == 0;
    }

    public static void moveFile(File source, File dest) throws IOException {
        copyFile(source, dest);
        deleteFile(source);
    }

    private static String normalizePath(String path, String nameFile) {
        if (path.startsWith("." + File.separator))
            path = path.substring(path.indexOf(File.separator));

        if (path.endsWith(File.separator))
            path = path.substring(0, path.lastIndexOf(File.separator));

        int lastIndexOf = path.lastIndexOf(nameFile);
        path = lastIndexOf <= 0 ? "" : path.substring(0, lastIndexOf - 1);

        return path;
    }

    @SuppressWarnings("resource")
	public static String readFile(String filename) throws IOException {
        Scanner scanner = new Scanner(new File(filename)).useDelimiter("\\Z");
        String word = null;
        if (scanner.hasNext()) {
            word = scanner.next();
            scanner.close();
        }
        scanner.close();
        return word;
    }

    public static byte[] readFileBytes(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.readAllBytes(path);
    }

    public static void renameFile(File oldFile, File newFile)
            throws IOException {
        if (oldFile.isDirectory()) {
            newFile.mkdirs();
            for (File sourceChild : oldFile.listFiles())
                copyFile(sourceChild, new File(newFile.getCanonicalPath() + "/"
                        + sourceChild.getName()));
        } else
            copyFile(oldFile, newFile);

        deleteFile(oldFile);
    }

    public static void unzipFile(byte[] dataInput, String outputFolder)
            throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(dataInput);
        unzipFile(bais, outputFolder);
    }

    private static void unzipFile(InputStream input, String outputFolder)
            throws IOException {
        byte[] buffer = new byte[1024];

        File folder = new File(outputFolder);

        if (!folder.exists())
            folder.mkdir();
        else if (!folder.isDirectory())
            throw new IOException("Output folder is file.");

        ZipInputStream zis = new ZipInputStream(input);
        ZipEntry ze = zis.getNextEntry();

        while (ze != null) {

            String fileName = ze.getName();
            File newFile = new File(outputFolder + File.separator + fileName);
            new File(newFile.getParent()).mkdirs();

            FileOutputStream fos = new FileOutputStream(newFile);

            int len;
            while ((len = zis.read(buffer)) > 0)
                fos.write(buffer, 0, len);

            fos.close();
            ze = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();
    }

    public static void unzipFile(String inputzipfileName, String outputFolder)
            throws IOException {
        FileInputStream input = new FileInputStream(inputzipfileName);
        unzipFile(input, outputFolder);
    }

    private static File validateFile(String path) throws FileNotFoundException,
            IOException {
        File file = new File(path);

        if (!file.exists())
            throw new FileNotFoundException(file.getName());

        if (file.isDirectory() && file.list().length == 0)
            throw new FileNotFoundException("Directory " + path + " is empty");

        if (file.isFile() && file.length() == 0)
            throw new FileNotFoundException("File " + path + " is empty");

        file = new File(file.getCanonicalPath());
        return file;
    }

    public static void writeFile(String fileName, byte[] content,
            boolean update) {
        FileOutputStream writer = null;

        try {
            File file = new File(fileName);

            if (!file.exists() || update) {
                writer = new FileOutputStream(file);
                writer.write(content);
            }

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException e) {
            }
        }
    }

    public static void writeFile(String fileName, String content,
            boolean update) {
        BufferedWriter writer = null;

        try {
            File file = new File(fileName);

            if (!file.exists() || update) {
                writer = new BufferedWriter(new FileWriter(file));
                writer.write(content);
            }

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException e) {
            }
        }
    }

    public static byte[] zipFile(String fileName) throws Exception {
        List<String> files = new ArrayList<String>();
        files.add(fileName);
        return zipFiles(files);
    }

    public static void zipFile(String fileName, String outputzipfileName)
            throws Exception {
        File f = new File(outputzipfileName);
        FileOutputStream baos = new FileOutputStream(f);

        HashSet<String> set = new HashSet<String>();
        set.add(f.getCanonicalPath());

        List<String> files = new ArrayList<String>();
        files.add(fileName);
        zipFiles(files, baos, set);
    }

    private static void zipFile(String path, ZipOutputStream zip,
            HashSet<String> exclude) throws Exception {
        File file = validateFile(path);
        path = file.getName();
        path = normalizePath(path, file.getName());

        addFileToZip(path, file, zip, exclude);
    }

    public static byte[] zipFiles(List<String> files) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        zipFiles(files, baos, null);

        return baos.toByteArray();
    }

    private static void zipFiles(List<String> files, OutputStream baos,
            HashSet<String> exclude) throws Exception {
        ZipOutputStream zip = new ZipOutputStream(baos);
        zipFiles(files, zip, exclude);
        zip.flush();
        zip.close();
    }

    private static void zipFiles(List<String> files, ZipOutputStream zip,
            HashSet<String> exclude) throws Exception {
        for (String fileName : files)
            if (fileName.endsWith(File.separator + "*")) {
                String pathName = fileName.substring(0,
                        fileName.lastIndexOf('*'));
                File file = validateFile(pathName);
                if (file.isDirectory()) {
                    List<String> list = new ArrayList<String>();
                    for (File child : file.listFiles())
                        list.add(pathName + File.separator + child.getName());
                    zipFiles(list, zip, exclude);
                } else
                    throw new Exception("path format error: " + fileName);
            } else
                zipFile(fileName, zip, exclude);
    }
}
