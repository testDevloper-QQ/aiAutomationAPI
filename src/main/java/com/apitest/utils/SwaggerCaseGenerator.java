package com.apitest.utils;

import com.apitest.data.SwaggerDataProvider;
import java.util.List;
import java.util.Map;

public class SwaggerCaseGenerator {
    public List<Map<String, Object>> generateCases(String docPath, String host) throws Exception {
        return SwaggerDataProvider.parse(docPath, host);
    }
}