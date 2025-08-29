package uk.ac.ebi.biostudies.api.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.MultiValueMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DataTableColumnInfo {
    private static Logger LOGGER = LogManager.getLogger(DataTableColumnInfo.class.getName());
    private String name;
    private int index;
    private String dir;
    private int column;
    private String searchValue;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public static Map<Integer, DataTableColumnInfo> ParseDataTableRequest(MultiValueMap<String,String> dtRequest) throws Exception{
        Set<String> keySet = dtRequest.keySet();
        Map<Integer, DataTableColumnInfo> resultMap = new HashMap<>();
        Map<Integer, Integer> orderMap = new HashMap<>();
        DataTableColumnInfo colInfo;
        int colIndex = 0;
        int orderIndex = 0;
        Set<String[]> orderSet = new HashSet<>();
        for (String key : keySet) {
            if (key.contains("[name]") && key.contains("columns")) {
                colIndex = getArrayIndex(key);
                colInfo = resultMap.get(colIndex);
                if (colInfo == null) {
                    colInfo = new DataTableColumnInfo();
                    resultMap.put(colIndex, colInfo);
                }
                colInfo.column = colIndex;
                colInfo.name = dtRequest.getFirst(key);
            } else if (key.contains("order[") && key.contains("[column]")) {
                orderIndex = getArrayIndex(key);
                orderMap.put(orderIndex, Integer.parseInt(dtRequest.getFirst(key)));
            } else if (key.contains("order[") && key.contains("[dir]")) {
                String dir = dtRequest.getFirst(key);
                orderIndex = getArrayIndex(key);
                orderSet.add(new String[] {orderIndex+"", dir});
            } else if (key.contains("search]") && key.contains("value")) {
                String searchVal = dtRequest.getFirst(key);
                if (searchVal == null || searchVal.isEmpty()) {
                    continue;
                }
                colIndex = getArrayIndex(key);
                colInfo = resultMap.get(colIndex);
                if(colInfo == null) {
                    colInfo = new DataTableColumnInfo();
                    resultMap.put(colIndex, colInfo);
                }
                colInfo.setSearchValue(searchVal);
            }
        }
        for (String[] order : orderSet) {
            try {
                colIndex = (Integer) orderMap.get(Integer.valueOf(order[0]));
                colInfo = resultMap.get(colIndex);
                if (colInfo != null) {
                    colInfo.setDir(order[1]);
                }
            }catch (Exception e) {
                LOGGER.debug("order map parse exception", e);
            }
        }
        return resultMap;
    }

    private static  int getArrayIndex(String key){
        int begin = key.indexOf("[");
        int end = key.indexOf("]");
        String pval = key.substring(begin+1, end);
        return Integer.parseInt(pval);
    }


    public String getSearchValue() {
        return searchValue;
    }

    public void setSearchValue(String searchValue) {
        this.searchValue = searchValue;
    }
}
