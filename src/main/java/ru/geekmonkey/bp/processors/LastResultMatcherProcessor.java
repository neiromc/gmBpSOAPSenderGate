package ru.geekmonkey.bp.processors;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import ru.geekmonkey.bp.Main;
import ru.geekmonkey.bp.models.ResponseResultList;
import ru.geekmonkey.bp.yaml.LastResultMatching;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

/**
 * Created by neiro on 26.07.16.
 */
public class LastResultMatcherProcessor {

    private Logger logger = Main.logger;
    private int stepId;
    private String data;
    private ResponseResultList dataLastList;
    private ArrayList<LastResultMatching> lastResultMatchingList;

    private String dataLast;

    public LastResultMatcherProcessor(final int stepId,
                                      final String data,
                                      final ResponseResultList dataLastList,
                                      final ArrayList<LastResultMatching> lastResultMatchingList)
    {
        this.stepId = stepId;
        this.data = data;
        this.dataLastList = dataLastList;
        this.lastResultMatchingList = lastResultMatchingList;
    }

    public String processingData() {
        logger.info("====== DATA MATCHER PROCESSOR ====== START ==");
        logger.info(String.format("step: %s, dataLastList.size: %s ",stepId, dataLastList.size()));

        for (LastResultMatching lrm : lastResultMatchingList) {
            dataLast = dataLastList.get(lrm.getValueFromStep - 1);

            logger.info(String.format("RESP DATA BEFORE: %s", dataLast));
            logger.info(String.format("REQ  DATA BEFORE: %s", data));
            logger.info(String.format("MatchingItem: getValueFromStep=%s, getValueFromTag=%s, setType=%s, setValueToField=%s",
                    lrm.getValueFromStep, lrm.getValueFromTag, lrm.setType, lrm.setValueToField)
            );

            setValueToData(lrm.getValueFromTag, lrm.setType, lrm.setValueToField);
        }

        logger.info(String.format("DATA AFTER: %s", data));
        logger.info("====== DATA MATCHER PROCESSOR ====== END ==");

        return data;
    }

    private void setValueToData(final String getTagValue,
                                  final String setType,
                                  final String setValue) {

        switch ( setType.toLowerCase().trim() ) {
            //Change TagValue in response to MACROS_NAME_VALUE
            case "macros"           : setValueToMacros(getTagValue, setValue);
                                      break;
            //Change TagValue in response to same VALUE
            case "tag"              : setValueToTag(getTagValue, setValue);
                                      break;
        }

    }

    private void setValueToMacros(final String tag,
                                    final String value)
    {
        Document docFrom = getDocument(dataLast);
        String getValue = docFrom.getElementsByTagName(tag).item(0).getTextContent();
        logger.info(String.format("====> [%s] swap to [%s]", value, getValue));


        if ( getValue != null ) {
            data = data.replaceAll(value, getValue);
        } else {
            logger.warn(String.format("Can't find <%s> value from XML for setValueToMacros method (getElementByTagName return null)", tag));
        }
    }

    private void setValueToTag(final String tag,
                                 final String value)
    {
        Document docFrom = getDocument(dataLast);
        String getValue = docFrom.getElementsByTagName(tag).item(0).getTextContent();
        logger.info(String.format("====> [%s] swap to [%s]", value, getValue));
        Document docTo = getDocument(data);

        if ( getValue != null ) {
            docTo.getElementsByTagName(value).item(0).setTextContent(getValue);
        } else {
            logger.warn(String.format("Can't find <%s> value from XML for setValueToTag method (getElementByTagName return null)", tag));
        }
        data = getDocumentText(docTo);
    }

    private Document getDocument(final String xmlData) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            return builder.parse(new InputSource(new StringReader(xmlData)));

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getDocumentText(Document doc) {
        String result = "";

        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            result = writer.getBuffer().toString();
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        return result;
    }

}
