package moe.ore.txhook.helper;

public class IpUtil {
    public static int ip_to_int(String str) {
        byte[] bArr = new byte[4];
        try {
            String[] split = str.split("\\.");
            bArr[0] = (byte) (Integer.parseInt(split[0]) & 255);
            bArr[1] = (byte) (Integer.parseInt(split[1]) & 255);
            bArr[2] = (byte) (Integer.parseInt(split[2]) & 255);
            bArr[3] = (byte) (Integer.parseInt(split[3]) & 255);
            return (bArr[3] & 255) | ((bArr[2] << 8) & 65280) | ((bArr[1] << 16) & 16711680) | ((bArr[0] << 24) & -16777216);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static String int_to_ip(long j) {
        j = j & 4294967295L;
        StringBuilder builder = new StringBuilder(16);
        for (int i = 3; i >= 0; i--) {
            builder.append(255 & (j % 256));
            j /= 256;
            if (i != 0) {
                builder.append('.');
            }
        }
        return builder.toString();
    }


}
