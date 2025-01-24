package com.rakensi.xml.ner.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.greenmercury.smax.SmaxDocument;
import org.greenmercury.smax.convert.XmlString;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import com.rakensi.xml.ner.Logger;
import com.rakensi.xml.ner.NamedEntityRecognition;

public class NamedEntityRecognitionWithinElementTest
{
  private static final org.junit.platform.commons.logging.Logger junitLogger = org.junit.platform.commons.logging.LoggerFactory.getLogger(NamedEntityRecognitionTest.class);
  private static final Logger logger = new Logger() {
    @Override
    public void info(String message)
    {
      junitLogger.info(() -> message);
    }
    @Override
    public void warning(String message)
    {
      junitLogger.warn(() -> message);
    }
    @Override
    public void error(String message)
    {
      junitLogger.error(() -> message);
    }
  };

  private String xmlGrammarString =
      "<grammar>\n" +
      "  <entity id=\"PET\"><name>PET</name><name>polyethylene</name><name>terephthalate</name></entity>\n" +
      "  <entity id=\"HDPE\"><name>HDPE</name><name>high-density polyethylene</name><name>polyethylene high-density</name></entity>\n" +
      "  <entity id=\"PVC\"><name>PVC</name><name>polyvinyl chloride</name><name>polyvinylchloride</name><name>vinyl</name><name>polyvinyl</name></entity>\n" +
      "</grammar>\n";

  @Test
  void test_MatchWithinElement_1() throws Exception
  {
    Element grammar = XmlString.toDomElement(xmlGrammarString);
    Map<String, String> options = new HashMap<String, String>();
    options.put("word-chars", "-");
    options.put("match-within-element", "m");
    NamedEntityRecognition ner = new NamedEntityRecognition(grammar, options, logger);
    SmaxDocument document = XmlString.toSmax("<r>What about <m>vinyl, high-density polyethylene,</m> and polyethylene?</r>");
    ner.scan(document);
    String output = XmlString.fromSmax(document).replaceAll("<\\?.*?\\?>", "").replaceAll("\\s*xmlns:.+?=\".*?\"", "");
    String expectedOutput = "<r>What about <m><fn:match id=\"PVC\">vinyl</fn:match>, <fn:match id=\"HDPE\">high-density polyethylene</fn:match>,</m> and polyethylene?</r>";
    assertEquals(expectedOutput, output);
  }

}
