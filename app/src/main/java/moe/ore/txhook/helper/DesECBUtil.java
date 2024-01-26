/*
 * English :
 *  The project is protected by the MPL open source agreement.
 * Open source agreement warning that prohibits deletion of project source code files.
 * The project is prohibited from acting in illegal areas.
 * All illegal activities arising from the use of this project are the responsibility of the second author, and the original author of the project is not responsible
 *
 *  中文：
 *  该项目由MPL开源协议保护。
 *  禁止删除项目源代码文件的开源协议警告内容。
 * 禁止使用该项目在非法领域行事。
 * 使用该项目产生的违法行为，由使用者或第二作者全责，原作者免责
 *
 * 日本语：
 * プロジェクトはMPLオープンソース契約によって保護されています。
 *  オープンソース契約プロジェクトソースコードファイルの削除を禁止する警告。
 * このプロジェクトは違法地域の演技を禁止しています。
 * このプロジェクトの使用から生じるすべての違法行為は、2番目の著者の責任であり、プロジェクトの元の著者は責任を負いません。
 *
 */

package moe.ore.txhook.helper;

import java.nio.ByteBuffer;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class DesECBUtil {
    /**
     * 加密数据
     * 注意：这里的数据长度只能为8的倍数
     */
    public static byte[] encryptDES(byte[] enc, String encryptKey) throws Exception {
        SecretKeySpec key = new SecretKeySpec(getKey(encryptKey), "DES");
        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(setStuff(enc));
    }

    /**
     * 自定义一个key
     */
    public static byte[] getKey(String keyRule) {
        Key key;
        byte[] keyByte = keyRule.getBytes();
        // 创建一个空的八位数组,默认情况下为0
        byte[] byteTemp = new byte[8];
        // 将用户指定的规则转换成八位数组
        for (int i = 0; i < byteTemp.length && i < keyByte.length; i++) {
            byteTemp[i] = keyByte[i];
        }
        key = new SecretKeySpec(byteTemp, "DES");
        return key.getEncoded();
    }

    /***
     * 解密数据
     */
    public static byte[] decryptDES(byte[] decrypt, String decryptKey) throws Exception {
        SecretKeySpec key = new SecretKeySpec(getKey(decryptKey), "DES");
        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(decrypt);
    }

    /***
     * 填充数据
     */
    public static byte[] setStuff(byte[] msg) {
        int i = msg.length % 8;
        if (i == 0) {
            return msg;
        } else {
            int a = 8 - i;
            StringBuilder sb = new StringBuilder();
            for (int s = 0; s < a; s++) {
                sb.append(" ");
            }
            byte[] data = sb.toString().getBytes();
            data = ByteBuffer.allocate(msg.length + data.length).put(msg).put(data).array();
            return data;
        }
    }
}
