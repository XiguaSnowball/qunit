package com.qunar.base.qunit.preprocessor;

import com.qunar.base.qunit.model.KeyValueStore;
import com.qunar.base.qunit.util.MapUtils;
import com.qunar.base.qunit.util.PropertyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.*;

/**
 * User: zonghuang
 * Date: 8/15/13
 */
public class DataCaseProcessor {

    private final static String DEFAULT = "default";

    public final static Map<String, Map<String, Map<String, String>>> dataCasesMap = new HashMap<String, Map<String, Map<String, String>>>();

    public static void parseDataCases(Element element, Map<String, String> keyMap) {
        Map<String, String> attributeMap = getAttributeMap(element);
        Iterator iterator = element.elementIterator();
        Map<String, List<Map<String,String>>> defaultMap = null;
        Map<String, List<Map<String,String>>> orginCaseDataMap = null;
        Map<String, String> caseDataMap = null;
        while(iterator.hasNext()){
            Element row = (Element)iterator.next();
            if (DEFAULT.equals(row.getName())){
                defaultMap = getData(row);
            }else {
                orginCaseDataMap = getData(row);
                mergeMap(orginCaseDataMap, defaultMap, keyMap);
                caseDataMap = processData(orginCaseDataMap);

                Map<String, String> dataCaseAttributeMap = getAttributeMap(row);
                String id = dataCaseAttributeMap.get("id");
                String executor = attributeMap.get("executor");
                Map<String, Map<String, String>> caseMap = new HashMap<String, Map<String, String>>();
                caseMap.put(id, caseDataMap);

                Map<String, Map<String, String>> checkMap = dataCasesMap.get(executor);
                if (checkMap == null){
                    dataCasesMap.put(executor, caseMap);
                } else{
                    checkMap.putAll(caseMap);
                    dataCasesMap.put(executor, checkMap);
                }
            }
        }
    }

    private static Map<String, List<Map<String, String>>> mergeMap(Map<String, List<Map<String, String>>> caseMap, Map<String, List<Map<String, String>>> defaultMap, Map<String, String> keyMap){
        Iterator iterator = defaultMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, List<Map<String, String>>> entry = (Map.Entry<String, List<Map<String, String>>>) iterator.next();
            String key = null;
            if (keyMap != null){
                key = keyMap.get(entry.getKey());
            }
            if (key == null){
                mergeNoKeyList(caseMap, entry);
            } else {
                checkDuplicateKey(key, entry.getValue());
                mergeKeyList(caseMap, entry, key);
            }
        }

        return caseMap;
    }

    private static void mergeNoKeyList(Map<String, List<Map<String, String>>> caseMap, Map.Entry<String, List<Map<String, String>>> entry){
        List<Map<String, String>> caseList = caseMap.get(entry.getKey());
        if (caseList == null){
            caseMap.put(entry.getKey(), entry.getValue());
        } else {
            List<Map<String, String>> defaultList = entry.getValue();
            /*if (defaultList != null && defaultList.size() > 1){
                throw new RuntimeException("default中数据超过1条，请定义key");
            }*/
            caseMap.put(entry.getKey(), replaceList(caseList, defaultList));

        }
    }

    private static List<Map<String, String>> replaceList(List<Map<String, String>> caseList, List<Map<String, String>> defaultList){
        List<Map<String, String>> newCaseList = new ArrayList<Map<String, String>>();
        for (Map<String, String> cMap : caseList){
            for (Map<String, String> dMap : defaultList){
                Map<String, String> newMap = copyMap(dMap);
                replaceMap(newMap, cMap);
                newCaseList.add(newMap);
            }
        }
        return newCaseList;
    }

    private static void replaceMap(Map<String, String> newMap, Map<String, String> cMap){
        Iterator iterator = cMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, String> entry = (Map.Entry<String, String>) iterator.next();
            newMap.put(entry.getKey(), entry.getValue());
        }
    }

    private static void mergeKeyList(Map<String, List<Map<String, String>>> caseMap, Map.Entry<String, List<Map<String, String>>> entry, String key) {
        List<Map<String, String>> caseList = caseMap.get(entry.getKey());
        List<Map<String, String>> newCaseList = new ArrayList<Map<String, String>>();
        if (caseList == null) {
            caseMap.put(entry.getKey(), entry.getValue());
        } else {
            List<Map<String, String>> defaultList = entry.getValue();
            for (Map<String, String> dMap : defaultList) {
                boolean equal = false;
                for (Map<String, String> cMap : caseList) {
                    if (dMap.get(key) != null && dMap.get(key).equalsIgnoreCase(cMap.get(key))) {
                        Map<String, String> tempMap = copyMap(dMap);
                        replaceMap(tempMap, cMap);
                        newCaseList.add(tempMap);
                        equal = true;
                        break;
                    }
                }
                if (equal) continue;
                newCaseList.add(dMap);
            }
            newCaseList.addAll(getDiff(caseList, defaultList, key));
            caseMap.put(entry.getKey(), newCaseList);
        }
    }

    private static List<Map<String, String>> getDiff(List<Map<String, String>> caseMap, List<Map<String, String>> defaultMap, String key){
        List<Map<String, String>> diffList = new ArrayList<Map<String, String>>();
        for (Map<String, String> cMap : caseMap){
            boolean equal = false;
            for (Map<String, String> dMap : defaultMap){
                if (cMap.get(key) != null && cMap.get(key).equals(dMap.get(key))){
                    equal = true;
                    break;
                }
            }
            if (equal) continue;
            diffList.add(cMap);
        }
        return diffList;
    }

    private static void checkDuplicateKey(String key, List<Map<String, String>> dataList){
        if (CollectionUtils.isEmpty(dataList)){
            return ;
        }
        List<String> keyList = new ArrayList<String>();
        for (Map<String, String> aMap : dataList){
            String value = aMap.get(key);
            if (value == null) continue;
            if (keyList.contains(value)){
                throw new RuntimeException("存在相同的key:{}");
            } else {
                keyList.add(value);
            }
        }
    }

    private static Map<String, List<Map<String,String>>> getData(Element element){
        Iterator iterator = element.elementIterator();
        Map<String, List<Map<String, String>>> dataCaseMap = new HashMap<String, List<Map<String, String>>>();
        while (iterator.hasNext()){
            Element row = (Element)iterator.next();
            Map<String, String> trMap = processRow(row);
            List<Map<String, String>> listMap = dataCaseMap.get(row.getName());
            if (CollectionUtils.isEmpty(listMap)){
                listMap = new ArrayList<Map<String, String>>();
            }
            listMap.add(trMap);
            dataCaseMap.put(row.getName(), listMap);
        }

        return dataCaseMap;
    }

    private static Map<String, String> processData(Map<String, List<Map<String, String>>> dataCaseMap){
        Map<String, String> map = new HashMap<String, String>();
        Iterator iterator = dataCaseMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, List<Map<String, String>>> entry = (Map.Entry<String, List<Map<String, String>>>)iterator.next();
            map.putAll(parseList(entry.getKey(), entry.getValue()));
        }

        return map;
    }

    private static Map<String, String> parseList(String name, List<Map<String, String>> mapList){
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        if (CollectionUtils.isEmpty(mapList)){
            return null;
        }
        for (int i = 0; i < mapList.size(); i++){
            Map<String, String> orginMap = mapList.get(i);
            parseMap(orginMap, map, i, name);
        }
        checkNumber(map, mapList.size());

        return convertListToString(map);
    }

    private static void checkNumber(Map<String, List<String>> map, int count){
        Iterator iterator = map.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, List<String>> entry = (Map.Entry<String, List<String>>) iterator.next();
            List<String> valueList = entry.getValue();
            if (valueList.size() != count){
                addList(valueList, count);
            }
            map.put(entry.getKey(), valueList);
        }
    }

    private static void parseMap(Map<String, String> orginMap, Map<String, List<String>> map, int index, String name){
        Iterator iterator = orginMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, String> entry = (Map.Entry<String, String>) iterator.next();
            String key = name + "." + entry.getKey();
            List<String> valueList = map.get(key);
            if (valueList == null){
                valueList = new ArrayList<String>();
                addList(valueList, index);
            } else if (valueList.size() != index){
                addList(valueList, index);
            }
            valueList.add(entry.getValue());
            map.put(key, valueList);
        }
    }

    private static Map<String, String> convertListToString(Map<String, List<String>> listMap){
        Map<String, String> stringMap = new HashMap<String, String>();
        Iterator iterator = listMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, List<String>> entry = (Map.Entry<String, List<String>>) iterator.next();
            String value = StringUtils.join(entry.getValue(), PropertyUtils.getProperty("join_split_process_data_char", "%#"));
            stringMap.put(entry.getKey(), value);
        }
        return stringMap;

    }

    private static void addList(List<String> valueList, int index){
        while (index - valueList.size() > 0){
            valueList.add("[null]");
        }
    }

    private static Map<String, String> processRow(Element trRow) {
        Map<String, String> trMap = new HashMap<String, String>();
        Iterator iterator = trRow.elementIterator();
        while (iterator.hasNext()){
            Element element = (Element)iterator.next();
            trMap.put(element.getName(), element.getText());
        }
        trMap.putAll(getAttributeMap(trRow));

        return trMap;
    }

    private static Map<String, String> addPrefix(Map<String, String> original, String name){
        Iterator iterator = original.entrySet().iterator();
        Map<String, String> afterMap = new HashMap<String, String>();
        while (iterator.hasNext()){
            Map.Entry<String, String> entry = (Map.Entry<String, String>)iterator.next();
            afterMap.put(name + "." + entry.getKey(), entry.getValue());
        }

        return afterMap;
    }

    private static List<KeyValueStore> getAttribute(Element element) {
        List<KeyValueStore> attributes = new ArrayList<KeyValueStore>();
        Iterator iterator = element.attributeIterator();
        while (iterator.hasNext()) {
            Attribute attribute = (Attribute) iterator.next();
            String attributeName = attribute.getName();
            String attributeValue = attribute.getValue();
            attributes.add(new KeyValueStore(attributeName, attributeValue));
        }
        return attributes;
    }

    public static Map<String, String> getAttributeMap(Element element) {
        List<KeyValueStore> attribute = getAttribute(element);
        return convertListKeyValueToMap(attribute);
    }

    private static Map<String, String> convertListKeyValueToMap(List<KeyValueStore> list) {
        Map<String, String> map = new HashMap<String, String>();
        for (KeyValueStore kvs : list) {
            map.put(kvs.getName(), (String) kvs.getValue());
        }
        return map;
    }

    private static Map<String, String> copyMap(Map<String, String> srcMap){
        Map<String, String> destMap = new HashMap<String, String>();
        if (srcMap == null){
            return destMap;
        }
        Iterator iterator = srcMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, String> entry = (Map.Entry<String, String>) iterator.next();
            destMap.put(entry.getKey(), entry.getValue());
        }

        return  destMap;
    }
}