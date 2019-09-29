package reflection;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.sun.xml.internal.ws.util.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import reflection.annotation.Reflected;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class Main {
    private static final String PATH_TO_CONFIG = "src/resources/config.xml";
    private static final String packageToScan = "reflection";

    public static void main(String[] args) {
        List<Object> createdObjects = createAllInstances();

        System.out.println("Print all created objects, which class with annotation @Reflected");
        for (Object obj : createdObjects) {
            System.out.println(obj);
        }
    }

    private static List<Object> createAllInstances() {
        Set<Class<?>> classes = findAllClassesInPackages(packageToScan);

        List<Object> createdObjects = new ArrayList<>();
        for (Class<?> c : classes) {
            if (c.isAnnotationPresent(Reflected.class)) {
                if (isClassConfigured(c.getName())) {
                    createdObjects.add(createInstance(c.getName()));
                }
            }
        }
        return createdObjects;
    }

    private static Set<Class<?>> findAllClassesInPackages(String packageToScan) {
        List<ClassLoader> classLoadersList = new LinkedList<>();
        classLoadersList.add(ClasspathHelper.contextClassLoader());
        classLoadersList.add(ClasspathHelper.staticClassLoader());

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setScanners(new SubTypesScanner(false), new ResourcesScanner())
                .setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])))
                .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(packageToScan))));

        return reflections.getSubTypesOf(Object.class);
    }

    private static boolean isClassConfigured(String className) {
        try {
            Document doc = getDocument();

            NodeList nList = doc.getElementsByTagName("bean");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if ((eElement.getAttribute("class").endsWith(className))) {
                        return true;
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

            return false;
    }

    private static Object createInstance(String className) {
        Class<?> classOfCreatedObject;
        Object objectToCreate = null;
        try {
            Document document = getDocument();
            NodeList nodeBeanList = document.getElementsByTagName("bean");
            for (int currentBean = 0; currentBean < nodeBeanList.getLength(); currentBean++) {
                Node node = nodeBeanList.item(currentBean);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    classOfCreatedObject = Class.forName(eElement.getAttribute("class"));
                    if (eElement.getAttribute("class").endsWith(className)) {
                        objectToCreate = classOfCreatedObject.newInstance();
                        Method[] methods = classOfCreatedObject.getMethods();
                        NodeList properties = eElement.getElementsByTagName("property");
                        Table<String, String, String> table = HashBasedTable.create();
                        putAttributesToTable(table, properties);
                        iterateTable(objectToCreate, methods, table.rowMap());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return objectToCreate;
    }

    private static Document getDocument() throws ParserConfigurationException, SAXException, IOException {
        File inputFile = new File(PATH_TO_CONFIG);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document document = dBuilder.parse(inputFile);
        document.getDocumentElement().normalize();
        return document;
    }

    private static void putAttributesToTable(Table<String, String, String> table, NodeList properties) {
        for (int i = 0; i < properties.getLength(); i++) {
            Node property = properties.item(i);
            Element eElementP = (Element) property;
            table.put(eElementP.getAttribute("name"), eElementP.getAttribute("value"),
                    eElementP.getAttribute("type"));
        }
    }

    private static void iterateTable(Object objectToCreate, Method[] methods, Map<String, Map<String, String>> map) {
        for (Map.Entry<String, Map<String, String>> outer : map.entrySet()) {
            for (Map.Entry<String, String> inner : outer.getValue().entrySet()) {
                for (Method m : methods) {
                    if (m.getName().equals("set".concat(StringUtils.capitalize(outer.getKey())))) {
                        parseParametersAndInvoke(objectToCreate, inner.getValue(), m, inner.getKey());
                    }
                }
            }
        }
    }

    private static void parseParametersAndInvoke (Object invoker, String type, Method m, String valueToParse) {
        try {
            switch (type) {
                case "double":
                case "java.lang.Double":
                    m.invoke(invoker, Double.parseDouble(valueToParse));
                    break;
                case "int":
                case "java.lang.Integer":
                    m.invoke(invoker, Integer.parseInt(valueToParse));
                    break;
                case "long":
                case "java.lang.Long":
                    m.invoke(invoker, Long.parseLong(valueToParse));
                    break;
                case "boolean":
                case "java.lang.Boolean":
                    m.invoke(invoker, Boolean.parseBoolean(valueToParse));
                    break;
                default:
                    m.invoke(invoker, valueToParse);
                    break;
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
