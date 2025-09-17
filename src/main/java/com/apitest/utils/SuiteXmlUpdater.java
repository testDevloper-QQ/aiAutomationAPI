package com.apitest.utils;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.dom4j.io.OutputFormat;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class SuiteXmlUpdater {
    public static void addTestClassesToSuite(String suitePath, List<String> classNames, String packageName) throws Exception {
        SAXReader reader = new SAXReader();
        Document doc = reader.read(new File(suitePath));
        Element suite = doc.getRootElement();
        Element test = suite.element("test");
        Element classes = test.element("classes");
        for (String className : classNames) {
            boolean exists = false;
            for (Object obj : classes.elements("class")) {
                Element classElem = (Element) obj;
                if (classElem.attributeValue("name").equals(packageName + "." + className)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                Element classElem = DocumentHelper.createElement("class");
                classElem.addAttribute("name", packageName + "." + className);
                classes.add(classElem);
            }
        }
        OutputFormat format = OutputFormat.createPrettyPrint();
        try (FileWriter writer = new FileWriter(suitePath)) {
            XMLWriter xmlWriter = new XMLWriter(writer, format);
            xmlWriter.write(doc);
            xmlWriter.close();
        }
    }
}