package com.activiti.rest.service.api.disco;

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.activiti.engine.ActivitiException;
import org.w3c.dom.Document;

public class AbstractDiscoResource {

  protected String transformDocumentToString(Document document) {
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer transformer;
    try {
      transformer = tf.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      StringWriter writer = new StringWriter();
      transformer.transform(new DOMSource(document), new StreamResult(writer));
      return writer.getBuffer().toString();
        
    } catch (TransformerException e) {
      throw new ActivitiException("Error transforming xml document to string", e);
    }
  }
}
