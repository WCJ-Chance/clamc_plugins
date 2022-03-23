package com.tencent.bk.devops.utils;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.junit.Test;

import java.io.File;
import java.util.Iterator;
import java.util.List;

public class Dom4jTest {
    @Test
    public void test() {
        // 解析xml文件
        // 创建SAXReader的对象reader
        SAXReader reader = new SAXReader();
        try {
            int passCase = 0;
            int totalCase = 4;
            // 通过reader对象的read方法加载books.xml文件,获取docuemnt对象。
            Document document = reader.read(new File("C:\\Users\\13366\\Desktop\\output.xml"));
            // 通过document对象获取根节点bookstore
            Element elements = document.getRootElement();
            // 通过element对象的elementIterator方法获取迭代器
            Iterator rootIt = elements.elementIterator();
            while (rootIt.hasNext()) {
                Element element = (Element) rootIt.next();
                String elementName = element.getName();
                if ("suite".equals(elementName)) {
                    Iterator suitIt = element.elementIterator();
                    while (suitIt.hasNext()) {
                        Element suiteChildren = (Element) suitIt.next();
                        if ("status".equals(suiteChildren.getName())) {
                            List<Attribute> statusAttrs = suiteChildren.attributes();
                            for (Attribute attr : statusAttrs) {
                                if ("status".equals(attr.getName())) {
                                    if ("PASS".equals(attr.getValue())) {
                                        System.out.println("脚本测试通过");
                                    } else {
                                        System.out.println("脚本测试不通过");
                                    }
                                }
                            }
                        } else if ("test".equals(suiteChildren.getName())) {
                            totalCase++;
                            Iterator testIt = element.elementIterator();
                            while (testIt.hasNext()) {
                                Element testChildren = (Element) testIt.next();
                                if ("status".equals(testChildren.getName())) {
                                    List<Attribute> statusAttrs = testChildren.attributes();
                                    for (Attribute attr : statusAttrs) {
                                        if ("status".equals(attr.getName())) {
                                            if ("PASS".equals(attr.getValue())) {
                                                passCase++;
                                            } else {
                                                System.out.println("用例测试不通过");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            float passRate = ((float) passCase / (float) totalCase) * 100;
            System.out.println("test case pass rate is : " + passRate + "%");
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }
}
