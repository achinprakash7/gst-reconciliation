package com.powerloom.entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReconciliationResponse {

    private Map<String, RowDataEntity> dataMap = new LinkedHashMap<>();
    private List<RowDataEntity> list = new ArrayList<>();

    public Map<String, RowDataEntity> getDataMap() { return dataMap; }
    public void setDataMap(Map<String, RowDataEntity> dataMap) { this.dataMap = dataMap; }

    public List<RowDataEntity> getList() { return list; }
    public void setList(List<RowDataEntity> list) { this.list = list; }
}