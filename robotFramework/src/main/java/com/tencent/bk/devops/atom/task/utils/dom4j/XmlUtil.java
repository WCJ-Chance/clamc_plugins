package com.tencent.bk.devops.atom.task.utils.dom4j;

import com.tencent.bk.devops.atom.common.Status;
import com.tencent.bk.devops.atom.pojo.AtomResult;
import kotlin.Pair;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.Iterator;
import java.util.List;

public class XmlUtil {

    private static final SAXReader READER = new SAXReader();

    public static Pair<Integer, Integer> getTestCaseStatus(String xmlPath, AtomResult result) {
        int passNum = 0;
        int totalNum = 0;
        Document document = null;
        // 通过reader对象的read方法加载books.xml文件,获取docuemnt对象。
        try {
            document = READER.read(new File(xmlPath));
        } catch (DocumentException e) {
            result.setStatus(Status.failure);
            result.setMessage("读取xml文件失败：" + xmlPath);
            return null;
        }
        assert document != null;
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
                    if ("test".equals(suiteChildren.getName())) {
                        totalNum++;
                        Iterator testIt = element.elementIterator();
                        while (testIt.hasNext()) {
                            Element testChildren = (Element) testIt.next();
                            if ("status".equals(testChildren.getName())) {
                                List<Attribute> statusAttrs = testChildren.attributes();
                                for (Attribute attr : statusAttrs) {
                                    if ("status".equals(attr.getName())) {
                                        if ("PASS".equals(attr.getValue())) {
                                            passNum++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return new Pair<>(passNum, totalNum);
    }
}
