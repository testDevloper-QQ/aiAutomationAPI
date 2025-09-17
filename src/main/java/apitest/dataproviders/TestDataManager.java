package apitest.dataproviders;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.annotations.DataProvider;

import java.io.InputStream;
import java.util.*;

/**
 * 测试数据管理器
 * 统一管理测试数据的读取和参数化
 */
public class TestDataManager {
    
    private static final String EXCEL_FILE_PATH = "testdata/test/module_testdata/file_testdata.xlsx";
    
    /**
     * 获取测试数据
     */
    @DataProvider(name = "apiTestData")
    public static Object[][] getApiTestData() {
        try (InputStream is = TestDataManager.class.getClassLoader().getResourceAsStream(EXCEL_FILE_PATH)) {
            if (is == null) {
                // 如果Excel文件不存在，返回YAML数据
                return getYamlTestData();
            }
            
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);
            
            // 获取标题行
            Row headerRow = sheet.getRow(0);
            Map<String, Integer> headers = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                headers.put(headerRow.getCell(i).getStringCellValue(), i);
            }
            
            // 读取数据
            List<Object[]> testData = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                Map<String, String> rowData = new HashMap<>();
                
                for (Map.Entry<String, Integer> entry : headers.entrySet()) {
                    Cell cell = row.getCell(entry.getValue());
                    String value = cell != null ? cell.toString() : "";
                    rowData.put(entry.getKey(), value);
                }
                
                testData.add(new Object[]{rowData});
            }
            
            return testData.toArray(new Object[0][]);
            
        } catch (Exception e) {
            // 如果Excel读取失败，返回YAML数据
            return getYamlTestData();
        }
    }
    
    /**
     * 从YAML获取测试数据
     */
    private static Object[][] getYamlTestData() {
        // 这里应该从YAML文件中读取测试数据
        // 为了简化，返回一个示例数据
        Map<String, String> testData = new HashMap<>();
        testData.put("TestCaseID", "TC001");
        testData.put("APIDescription", "文件上传测试");
        testData.put("Method", "POST");
        testData.put("Host", "localhost:8080");
        testData.put("Path", "/sdk/storage/upload/v1");
        testData.put("ExpectedResult", "200");
        
        return new Object[][]{{testData}};
    }
    
    /**
     * 获取模块测试数据
     */
    public static Map<String, Object> getModuleTestData(String moduleName) {
        try {
            // 从YAML文件中读取模块测试数据
            String yamlPath = "testdata/test/module_testdata/" + moduleName + "_testdata.yml";
            InputStream is = TestDataManager.class.getClassLoader().getResourceAsStream(yamlPath);
            
            if (is == null) {
                throw new RuntimeException("文件未找到: " + yamlPath);
            }
            byte[] bytes = new byte[is.available()];
            is.read(bytes);
            String content = new String(bytes);
            return JSON.parseObject(content, Map.class);
            
        } catch (Exception e) {
            // 忽略异常，返回空Map
        }
        
        return new HashMap<>();
    }
    
    /**
     * 获取环境变量
     */
    public static String getEnvironmentVariable(String key) {
        String value = System.getenv(key);
        if (value == null) {
            value = System.getProperty(key);
        }
        return value;
    }
    
    /**
     * 替换变量占位符
     */
    public static String replaceVariables(String template, Map<String, Object> variables) {
        if (template == null || variables == null) {
            return template;
        }
        
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        
        return result;
    }
}