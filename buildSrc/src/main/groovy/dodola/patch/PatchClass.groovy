package dodola.patch

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.zip.Adler32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

public class PatchClass {

    public static void process(String originDexPath, String ShellDexPath, String  originZipPath,String encryZipPath) {
        println("startDexShellWork")
        try {
            AESHelper helper = new AESHelper();
            File encryZipFile = new File(encryZipPath);
            File shellDexFile = new File(ShellDexPath);

            File orgDexFile = new File(originDexPath);

            File zipFile = new File(originZipPath);
            InputStream input = new FileInputStream(orgDexFile);
            ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));
            zipOut.putNextEntry(new ZipEntry(orgDexFile.getName()));
            int temp = 0;
            while ((temp = input.read()) != -1) {
                zipOut.write(temp);
            }
            input.close();
            zipOut.close();

            helper.encrypt(originZipPath, encryZipPath);
            byte[] payloadArray = readFileBytes(encryZipFile);
            byte[] unShellDexArray = readFileBytes(shellDexFile);
            int payloadLen = payloadArray.length;
            int unShellDexLen = unShellDexArray.length;
            int totalLen = payloadLen + unShellDexLen + 4;
            byte[] newdex = new byte[totalLen];
            System.arraycopy(unShellDexArray, 0, newdex, 0, unShellDexLen);
            System.arraycopy(payloadArray, 0, newdex, unShellDexLen, payloadLen);
            System.arraycopy(intToByte(payloadLen), 0, newdex, totalLen - 4, 4);
            fixFileSizeHeader(newdex);
            fixSHA1Header(newdex);
            fixCheckSumHeader(newdex);

            File file = new File(originDexPath);
            if (file.exists()) file.delete();

            file.createNewFile();
            FileOutputStream localFileOutputStream = new FileOutputStream(originDexPath);
            localFileOutputStream.write(newdex);
            localFileOutputStream.flush();
            localFileOutputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void fixCheckSumHeader(byte[] dexBytes) {
        Adler32 adler = new Adler32();
        adler.update(dexBytes, 12, dexBytes.length - 12);
        long value = adler.getValue();
        int va = (int) value;
        byte[] newcs = intToByte(va);
        byte[] recs = new byte[4];
        for (int i = 0; i < 4; i++) {
            recs[i] = newcs[newcs.length - 1 - i];
        }
        System.arraycopy(recs, 0, dexBytes, 8, 4);
    }


    public static byte[] intToByte(int number) {
        byte[] b = new byte[4];
        for (int i = 3; i >= 0; i--) {
            b[i] = (byte) (number % 256);
            number >>= 8;
        }
        return b;
    }

    private static void fixSHA1Header(byte[] dexBytes)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(dexBytes, 32, dexBytes.length - 32);
        byte[] newdt = md.digest();
        System.arraycopy(newdt, 0, dexBytes, 12, 20);
        String hexstr = "";
        for (int i = 0; i < newdt.length; i++) {
            hexstr += Integer.toString((newdt[i] & 0xff) + 0x100, 16)
                    .substring(1);
        }
    }

    private static void fixFileSizeHeader(byte[] dexBytes) {
        byte[] newfs = intToByte(dexBytes.length);
        byte[] refs = new byte[4];
        for (int i = 0; i < 4; i++) {
            refs[i] = newfs[newfs.length - 1 - i];
        }
        System.arraycopy(refs, 0, dexBytes, 32, 4);
    }


    private static byte[] readFileBytes(File file) throws IOException {
        byte[] arrayOfByte = new byte[1024];
        ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
        FileInputStream fis = new FileInputStream(file);
        while (true) {
            int i = fis.read(arrayOfByte);
            if (i != -1) {
                localByteArrayOutputStream.write(arrayOfByte, 0, i);
            } else {
                return localByteArrayOutputStream.toByteArray();
            }
        }
    }
}
