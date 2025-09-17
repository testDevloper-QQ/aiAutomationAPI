package com.apitest.utils;

import com.apitest.data.PostmanDataProvider;
import java.util.List;
import java.util.Map;

public class PostmanCaseGenerator {
    public List<Map<String, Object>> generateCases(String docPath, String host) throws Exception {
        return PostmanDataProvider.parse(docPath, host);
    }
}