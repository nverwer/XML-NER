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

class NamedEntityRecognitionFailureTest
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

  private String removeProcessingInstructionsAndNamespaces(String xml)
  {
    return xml.replaceAll("<\\?.*?\\?>", "").replaceAll("\\s*xmlns:.+?=\".*?\"", "");
  }


  /**
   * Test case for https://github.com/nverwer/XML-NER/issues/1
   * This test succeeds.
   */
  @Test
  void test_NoProgress_1() throws Exception
  {
    String grammar = "BWBR0002656 <- Burgerlijk Wetboek Boek 1, Personen- en familierecht\tBurgerlijk Wetboek Boek 1\tNieuw Burgerlijk Wetboek Boek 1\tBW\tBW Boek 1\tBurgerlijk Wetboek" + "\n";
    Map<String, String> options = new HashMap<String, String>();
    options.put("word-chars", "-/() [].,;:'\""); // (space included; parentheses, comma, colon, etc. are all "word" characters)
    options.put("no-word-before", "-/");
    options.put("no-word-after", "-.");
    options.put("case-insensitive-min-length",  "7");
    options.put("fuzzy-min-length",  "8");
    NamedEntityRecognition ner = new NamedEntityRecognition(grammar, options, logger);
    SmaxDocument document = XmlString.toSmax("<r>x BW (omdat als het al een schuldvordering is in juridische zin, die niet toekomt aan de nalatenschappen, maar aan de verkoper: (...) x</r>");
    ner.scan(document);
    String output = removeProcessingInstructionsAndNamespaces(XmlString.fromSmax(document));
    String expectedOutput = "<r>x <fn:match id=\"BWBR0002656\">BW</fn:match> (omdat als het al een schuldvordering is in juridische zin, die niet toekomt aan de nalatenschappen, maar aan de verkoper: (...) x</r>";
    assertEquals(expectedOutput, output);
  }


  /**
   * Test case for https://github.com/nverwer/XML-NER/issues/1
   * This test fails before the fix for this issue.
   */
  @Test
  void test_NoProgress_2() throws Exception
  {
    String grammar = "id0 <- (..." + "\n";
    Map<String, String> options = new HashMap<String, String>();
    NamedEntityRecognition ner = new NamedEntityRecognition(grammar, options, logger);
    SmaxDocument document = XmlString.toSmax("<r>BW (a: (...)</r>");
    ner.scan(document);
    String output = removeProcessingInstructionsAndNamespaces(XmlString.fromSmax(document));
    String expectedOutput = "<r>BW (a: (...)</r>";
    assertEquals(expectedOutput, output);
  }

}
