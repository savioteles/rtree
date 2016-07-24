package utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;

public class ConvertUtils {

    public static String asHex(byte buf[]) {
        StringBuffer strbuf = new StringBuffer(buf.length * 2);
        int i;

        for (i = 0; i < buf.length; i++) {
            if ((buf[i] & 0xff) < 0x10)
                strbuf.append("0");

            strbuf.append(Long.toString(buf[i] & 0xff, 16));
        }

        return strbuf.toString();
    }

    public static short[] convertFromBytesToShort(byte[] bytes) {
        short[] shorts = new short[bytes.length / 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).asShortBuffer()
                .get(shorts);
        return shorts;
    }

    public static byte[] convertFromShortToBytes(short[] shortBuffer) {
        byte[] bytes = new byte[shortBuffer.length * 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).asShortBuffer()
                .put(shortBuffer);
        return bytes;
    }

    public static byte[] createHash(String data)
            throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] hash = digest.digest((data).getBytes());
        return hash;
    }

    public static String createHashAsHex(String data)
            throws NoSuchAlgorithmException {
        byte[] hash = createHash(data);
        return asHex(hash);
    }

    public static String formataTempo(long l) {
        if (l <= 0)
            return "0 ms";

        if (l == Long.MAX_VALUE)
            return "---";

        String time = "";
        String ss = (l / 1000) + "";

        int dias = (int) l / (24 * 60 * 60 * 1000);
        l = l % (24 * 60 * 60 * 1000);

        if (dias > 0)
            time = String.format(time + "%1$2s d ", dias);

        int horas = (int) l / (60 * 60 * 1000);
        l = l % (60 * 60 * 1000);

        if (horas > 0)
            time = String.format(time + "%1$2s h ", horas);

        int minutos = (int) l / (60 * 1000);
        l = l % (60 * 1000);

        if (minutos > 0)
            time = String.format(time + "%1$2s m ", minutos);

        int segundos = (int) l / 1000;
        l = l % 1000;

        if (segundos > 0)
            time = String.format(time + "%1$2s s ", segundos);

        if (l > 0)
            time = String.format(time + "%1$3s ms ", l);

        return time + "(" + ss + " s)";
    }

    public static String getFormatNumber(double number, int precision) {
        String pattern = "#.##";
        DecimalFormat df = null;

        if (precision >= 0 && precision <= 15) {
            pattern = "";
            for (int i = 0; i < precision; i++)
                pattern += "#";

            pattern = "#." + pattern;
        }
        try {
            df = new DecimalFormat(pattern);
        } catch (Exception e) {
            df = new DecimalFormat("#.##");
        }

        String s = "";

        if (df != null)
            s = df.format(number);

        return s;
    }

    /**
     * Retorna a classe númerica deste valor (Integer, Float ou Double). Se o
     * valor não for um número retorna nulo.
     *
     * @param number
     * @return
     */
    public static String getNumberClass(String number) {
        String numClass = null;

        try {
            if (number.contains("."))
                try {
                    Float.parseFloat(number);
                    numClass = Float.class.getName();
                } catch (NumberFormatException e) {
                    Double.parseDouble(number);
                    numClass = Double.class.getName();
                }
            else
                try {
                    Integer.parseInt(number);
                    numClass = Integer.class.getName();
                } catch (NumberFormatException e) {
                    Long.parseLong(number);
                    numClass = Long.class.getName();
                }
        } catch (Exception e) {
            return null;
        }

        return numClass;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(
                            s.charAt(i + 1), 16));
        return data;
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public static String string2Hexa(byte[] bytes) {
        StringBuilder s = new StringBuilder();
        for (byte b : bytes) {
            int parteAlta = ((b >> 4) & 0xf) << 4;
            int parteBaixa = b & 0xf;
            if (parteAlta == 0)
                s.append('0');
            s.append(Integer.toHexString(parteAlta | parteBaixa));
        }
        return s.toString();
    }
}
