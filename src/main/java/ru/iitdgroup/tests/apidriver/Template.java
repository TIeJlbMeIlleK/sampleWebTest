package ru.iitdgroup.tests.apidriver;

import javax.xml.bind.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Шаблон запроса, который парсит XML в определенный тип D (указывается в наследниках)
 *
 * @param <D> request root
 */
public abstract class Template<D> {

    private final static Path TEMPLATE_PATH = Paths.get("resources");
    private final JAXBContext jc;
    private final D data;

    @SuppressWarnings("unchecked")
    public Template(InputStream body) throws JAXBException {
        jc = JAXBContext.newInstance(getObjectFactoryClazz());
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        data = (D) unmarshaller.unmarshal(body);
    }

    /**
     * @param fileName путь до файла, относительно resource
     * @throws JAXBException
     * @throws IOException
     */
    public Template(String fileName) throws JAXBException, IOException {
        this(Files.newInputStream(TEMPLATE_PATH.resolve(Paths.get(fileName))));
    }

    @SuppressWarnings("unchecked")
    public D getData() {
        return (D) ((JAXBElement) data).getValue();
    }

    /**
     * Сгенерировать XML на основе данных в текущем объекте
     */
    public String marshal() throws JAXBException {
        StringWriter stringWriter = new StringWriter();
        marshal(stringWriter);
        return stringWriter.toString();
    }

    /**
     * @see Template#marshal()
     * @param writer объект писатель
     * @throws JAXBException
     */
    public void marshal(Writer writer) throws JAXBException {
        Marshaller marshaller = jc.createMarshaller();
        marshaller.marshal(data, writer);
    }

    @Override
    public String toString() {
        try {
            return marshal();
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Класс объекта фабрики, для построения объекта при парсинге XML
     * @return класс объекта фабрики
     */
    protected abstract Class getObjectFactoryClazz();

}
