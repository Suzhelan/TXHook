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

package moe.ore.tars;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import moe.ore.txhook.helper.KotlinExtKt;

public class UniPacket {
    static HashMap<String, byte[]> newCache__tempdata = null;
    static HashMap<String, HashMap<String, byte[]>> cache__tempdata = null;
    private final RequestPacket _package = new RequestPacket();
    private int version = 3;
    private int requestId = 0;
    private String servantName = "";
    private String funcName = "";
    private HashMap<String, byte[]> _newData = new HashMap<>();
    private HashMap<String, HashMap<String, byte[]>> _data = new HashMap<>();

    public static UniPacket decode(byte[] bytes) {
        return decode(bytes, 0);
    }

    public static UniPacket decode(byte[] bytes, int index) {
        UniPacket uniPacket = new UniPacket();
        TarsInputStream input = new TarsInputStream(bytes, index);
        uniPacket._package.readFrom(input);
        input = new TarsInputStream(uniPacket._package.buffer);
        uniPacket.funcName = uniPacket._package.getFuncName();
        uniPacket.servantName = uniPacket._package.getServantName();
        uniPacket.requestId = uniPacket._package.getRequestId();
        uniPacket.version = uniPacket._package.getVersion();
        if (uniPacket.version == 3) {
            if (newCache__tempdata == null) {
                newCache__tempdata = new HashMap<>();
                newCache__tempdata.put("", new byte[0]);
            }
            uniPacket._newData = (HashMap<String, byte[]>) input.read(newCache__tempdata, 0, false);
        } else {
            if (cache__tempdata == null) {
                cache__tempdata = new HashMap<>();
                HashMap<String, byte[]> map = new HashMap<>();
                map.put("", new byte[0]);
                cache__tempdata.put("", map);
            }
            uniPacket._data = (HashMap<String, HashMap<String, byte[]>>) input.read(cache__tempdata, 0, false);
        }
        return uniPacket;
    }

    public static String java2UniType(String packageName) {
        switch (packageName) {
            case "java.lang.Integer":
            case "int":
                return "int32";
            case "java.lang.Boolean":
            case "boolean":
                return "bool";
            case "java.lang.Byte":
            case "byte":
                return "char";
            case "java.lang.Double":
            case "double":
                return "double";
            case "java.lang.Float":
            case "float":
                return "float";
            case "java.lang.Long":
            case "long":
                return "int64";
            case "java.lang.Short":
            case "short":
                return "short";
            case "java.lang.Character":
                throw new IllegalArgumentException("can not support java.lang.Character");
            case "java.lang.String":
                return "string";
            case "java.util.List":
                return "list";
            case "java.util.Map":
                return "map";
            default:
                return packageName;
        }
    }

    public static String transTypeList(ArrayList<String> list) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size(); ++i) {
            list.set(i, java2UniType(list.get(i)));
        }
        Collections.reverse(list);
        for (int i = 0; i < list.size(); ++i) {
            String name = list.get(i);
            switch (name) {
                case "list":
                case "Array":
                    list.set(i - 1, "<" + list.get(i - 1));
                    list.set(0, list.get(0) + ">");
                    break;
                case "map":
                    list.set(i - 1, "<" + list.get(i - 1) + ",");
                    list.set(0, list.get(0) + ">");
                    break;
            }
        }
        Collections.reverse(list);
        for (String s : list) {
            builder.append(s);
        }
        return builder.toString();
    }

    public void put(@NotNull TarsBase base) {
        servantName = base.servantName();
        funcName = base.funcName();
        String req = base.reqName();
        if (servantName == null || servantName.isEmpty() || funcName == null || funcName.isEmpty() || req.isEmpty()) {
            KotlinExtKt.runtimeError("servant or func or req name is null", null);
        }
        this.put(req, base);
    }

    public <T extends TarsBase> void put(String mapName, T t) {
        if (mapName == null) {
            throw new IllegalArgumentException("put key can not is null");
        } else if (t == null) {
            throw new IllegalArgumentException("put value can not is null");
        } else if (t instanceof Set) {
            throw new IllegalArgumentException("can not support Set");
        } else {
            TarsOutputStream out = new TarsOutputStream();
            out.setServerEncoding(StandardCharsets.UTF_8);
            out.write(t, 0);
            byte[] data = out.toByteArray();
            if (this.version == 3) {
                this._newData.put(mapName, data);
            } else {
                TarsOutputStream stream = new TarsOutputStream();
                stream.write(t, 0);
                byte[] bytes = stream.toByteArray();
                HashMap<String, byte[]> map = new HashMap<>(1);
                ArrayList<String> list = new ArrayList<>(1);
                map.put(transTypeList(list), bytes);
                this._data.put(mapName, map);
            }
        }
    }

    public <T extends TarsBase> T findByClass(String mapName, T base) {
        TarsInputStream input = new TarsInputStream(find(mapName));
        return input.read(base, 0, true);
    }

    public byte[] find(String mapName) {
        if (this._newData.containsKey(mapName)) {
            return this._newData.get(mapName);
        } else {
            Map<String, byte[]> map = this._data.get(mapName);
            for (Map.Entry<String, byte[]> content : map.entrySet()) {
                return content.getValue();
            }
        }
        return null;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public byte[] encode() {
        TarsOutputStream output = new TarsOutputStream();
        if (this.version == 3) {
            output.write(this._newData, 0);
        } else {
            output.write(this._data, 0);
        }

        byte[] data = output.toByteArray();
        this._package.setVersion(3);
        this._package.setFuncName(this.funcName);
        this._package.setServantName(this.servantName);
        this._package.setContext(new HashMap<>());
        this._package.setStatus(new HashMap<>());
        this._package.setBuffer(data);
        this._package.setRequestId(requestId);
        return _package.toByteArray();
    }

    public String getServantName() {
        return servantName;
    }

    public void setServantName(String servantName) {
        this.servantName = servantName;
    }

    public String getFuncName() {
        return funcName;
    }

    public void setFuncName(String funcName) {
        this.funcName = funcName;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
