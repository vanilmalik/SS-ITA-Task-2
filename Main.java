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
    private static final String PATH_TO_CONFIG = "C:\\Users\\DEDUSHKA DEDULYA\\IdeaProjects\\Task2\\src\\reflection\\config.xml";
    private static final String packageToScan = "reflection";

    public static void main(String[] args) {
        Set<Class<?>> classes = findAllClassesInPackages(packageToScan);

        List<Object> createdObjects = new ArrayList<>();
        for (Class<?> c : classes) {
            if (c.isAnnotationPresent(Reflected.class)) { //check if class is annotated
                if (isClassConfigured(c.getName())) { //check if class is in config file
                    createdObjects.add(createInstance(c.getName())); // create object and add to list
                }
            }
        }

        System.out.println("Print all created objects, which class with annotation @Reflected");
        for (Object obj : createdObjects) {
            System.out.println(obj);
        }
    }

    private static Set<Class<?>> findAllClassesInPackages(String packageToScan) {
        List<ClassLoader> classLoadersList = new LinkedList<>();
        classLoadersList.add(ClasspathHelper.contextClassLoader());
        classLoadersList.add(ClasspathHelper.staticClassLoader());

        //use Reflections lib to find all classes in all packages
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setScanners(new SubTypesScanner(false), new ResourcesScanner())
                .setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])))
                .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(packageToScan))));

        return reflections.getSubTypesOf(Object.class);
    }

    private static boolean isClassConfigured(String className) {
        try {
            File fXmlFile = new File(PATH_TO_CONFIG); // use static variable
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

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
            File inputFile = new File(PATH_TO_CONFIG);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document document = dBuilder.parse(inputFile);
            document.getDocumentElement().normalize();

            NodeList nodeBeanList = document.getElementsByTagName("bean");

            for (int temp = 0; temp < nodeBeanList.getLength(); temp++) {
                Node node = nodeBeanList.item(temp);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    classOfCreatedObject = Class.forName(eElement.getAttribute("class"));

                    Table<String, String, String> table = HashBasedTable.create(); //use google.common.collect.Table, like Map but for 3 vars
                    if (eElement.getAttribute("class").endsWith(className)) {
                        objectToCreate = classOfCreatedObject.newInstance();
                        Method[] methods = classOfCreatedObject.getMethods();

                        NodeList properties = eElement.getElementsByTagName("property");
                        for (int i = 0; i < properties.getLength(); i++) {
                            Node property = properties.item(i);
                            Element eElementP = (Element) property;
                            table.put(eElementP.getAttribute("name"), eElementP.getAttribute("value"),
                                    eElementP.getAttribute("type"));
                        }

                        Map<String, Map<String, String>> map = table.rowMap();
                        // iterate table, retrieve values and initialize object through setters
                        for (Map.Entry<String, Map<String, String>> outer : map.entrySet()) {
                            for (Map.Entry<String, String> inner : outer.getValue().entrySet()) {
                                for (Method m : methods) {
                                    if (m.getName().equals("set".concat(StringUtils.capitalize(outer.getKey())))) { //get setter for files. e.g field "name" - method "setName"
                                        parseParametersAndInvoke(objectToCreate, inner.getValue(), m, inner.getKey());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return objectToCreate;
    }

    // pass "type" from xml config file, parse to primitive and invoke setter
    private static void parseParametersAndInvoke (Object invoker, String type, Method m, String valueToParse) {
        try {
            if (type.equals("double") || type.equals("java.lang.Double")) {
                m.invoke(invoker, Double.parseDouble(valueToParse));
            } else if (type.equals("int") || type.equals("java.lang.Integer")) {
                m.invoke(invoker, Integer.parseInt(valueToParse));
            } else if (type.equals("long") || type.equals("java.lang.Long")) {
                m.invoke(invoker, Long.parseLong(valueToParse));
            } else if (type.equals("boolean") || type.equals("java.lang.Boolean")) {
                m.invoke(invoker, Boolean.parseBoolean(valueToParse));
            } else
                m.invoke(invoker, valueToParse); //if type of parameter is java.lang.String
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
