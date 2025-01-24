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

class NamedEntityRecognitionTest
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

  @Test
  void test_LongestMatch() throws Exception
  {
    String grammar =
      "a <- a" + "\n" +
      "b <- b" + "\n" +
      "ab <- a b" + "\n";
    Map<String, String> options = new HashMap<String, String>();
    NamedEntityRecognition ner = new NamedEntityRecognition(grammar, options, logger);
    SmaxDocument document = XmlString.toSmax("<r>a a b a b b</r>");
    ner.scan(document);
    String output = XmlString.fromSmax(document).replaceAll("<\\?.*?\\?>", "").replaceAll("\\s*xmlns:.+?=\".*?\"", "");
    String expectedOutput = "<r><fn:match id=\"a\">a</fn:match> <fn:match id=\"ab\">a b</fn:match> <fn:match id=\"ab\">a b</fn:match> <fn:match id=\"b\">b</fn:match></r>";
    assertEquals(expectedOutput, output);
  }

  @Test
  void test_Separators() throws Exception
  {
    String grammar =
      "eg:1 <= e.g." + "\n" +
      "eg:2<=e g; eg" + "\n";
    Map<String, String> options = new HashMap<String, String>();
    options.put("word-chars", ".");
    options.put("entity-separator", "\\s*<=\\s*");
    options.put("name-separator", "\\s*;\\s*");
    NamedEntityRecognition ner = new NamedEntityRecognition(grammar, options, logger);
    SmaxDocument document = XmlString.toSmax("<r>e.g. e g eg</r>");
    ner.scan(document);
    String output = XmlString.fromSmax(document).replaceAll("<\\?.*?\\?>", "").replaceAll("\\s*xmlns:.+?=\".*?\"", "");
    String expectedOutput = "<r><fn:match id=\"eg:1\">e.g.</fn:match> <fn:match id=\"eg:2\">e g</fn:match> <fn:match id=\"eg:2\">eg</fn:match></r>";
    assertEquals(expectedOutput, output);
  }

  @Test
  void test_FuzzyMinLength() throws Exception
  {
    String grammar =
      "eg1 <- e.g." + "\n" +
      "eg2 <- e g" + "\n" +
      "eg3 <- eg" + "\n" +
      "1 <- A A\tB A" + "\n" +
      "2 <- A B\tB B" + "\n" +
      "3 <- A C\tB C" + "\n";
    Map<String, String> options = new HashMap<String, String>();
    options.put("fuzzy-min-length", "1");
    NamedEntityRecognition ner = new NamedEntityRecognition(grammar, options, logger);
    SmaxDocument document = XmlString.toSmax("<r>A   A  e.g. e g eg  B A B B B C</r>");
    ner.scan(document);
    String output = XmlString.fromSmax(document).replaceAll("<\\?.*?\\?>", "").replaceAll("\\s*xmlns:.+?=\".*?\"", "");
    String expectedOutput = "<r><fn:match id=\"1\">A   A</fn:match>  <fn:match id=\"eg2\">e.g</fn:match>. <fn:match id=\"eg2\">e g</fn:match> <fn:match id=\"eg1&#9;eg3\">eg</fn:match>  <fn:match id=\"1\">B A</fn:match> <fn:match id=\"2\">B B</fn:match> <fn:match id=\"3\">B C</fn:match></r>";
    assertEquals(expectedOutput, output);
  }

  private String grammar_the_cf =
    "THE <- THE" + "\n" +
    "CF <- C F" + "\n" + "\n";

  @Test
  void test_CaseInsensitiveMinLenth_FuzzyMinLength_4() throws Exception
  {
    Map<String, String> options = new HashMap<String, String>();
    options.put("fuzzy-min-length", "4");
    options.put("case-insensitive-min-length", "4");
    NamedEntityRecognition ner = new NamedEntityRecognition(grammar_the_cf, options, logger);
    // test case-insensitive-min-length
    SmaxDocument document = XmlString.toSmax("<r>Do the right thing.</r>");
    ner.scan(document);
    String output = XmlString.fromSmax(document).replaceAll("<\\?.*?\\?>", "").replaceAll("\\s*xmlns:.+?=\".*?\"", "");
    String expectedOutput = "<r>Do the right thing.</r>";
    assertEquals(expectedOutput, output);
    // test fuzzy-min-length
    document = XmlString.toSmax("<r>C.F. Gauss was a German mathematician.</r>");
    ner.scan(document);
    output = XmlString.fromSmax(document).replaceAll("<\\?.*?\\?>", "").replaceAll("\\s*xmlns:.+?=\".*?\"", "");
    expectedOutput = "<r>C.F. Gauss was a German mathematician.</r>";
    assertEquals(expectedOutput, output);
  }

  @Test
  void test_CaseInsensitiveMinLenth_FuzzyMinLength_3() throws Exception
  {
    Map<String, String> options = new HashMap<String, String>();
    options.put("fuzzy-min-length", "3");
    options.put("case-insensitive-min-length", "3");
    NamedEntityRecognition ner = new NamedEntityRecognition(grammar_the_cf, options, logger);
    // test case-insensitive-min-length
    SmaxDocument document = XmlString.toSmax("<r>Do the right thing.</r>");
    ner.scan(document);
    String output = XmlString.fromSmax(document).replaceAll("<\\?.*?\\?>", "").replaceAll("\\s*xmlns:.+?=\".*?\"", "");
    String expectedOutput = "<r>Do <fn:match id=\"THE\">the</fn:match> right thing.</r>";
    assertEquals(expectedOutput, output);
    // test fuzzy-min-length
    document = XmlString.toSmax("<r>C.F. Gauss was a German mathematician.</r>");
    ner.scan(document);
    output = XmlString.fromSmax(document).replaceAll("<\\?.*?\\?>", "").replaceAll("\\s*xmlns:.+?=\".*?\"", "");
    expectedOutput = "<r><fn:match id=\"CF\">C.F</fn:match>. Gauss was a German mathematician.</r>";
    assertEquals(expectedOutput, output);
  }

  @Test
  void test_WordChars() throws Exception
  {
    String grammar = "RSVP <- R S V P" + "\n";
    Map<String, String> options = new HashMap<String, String>();
    options.put("fuzzy-min-length", "4");
    options.put("case-insensitive-min-length", "4");
    NamedEntityRecognition ner = new NamedEntityRecognition(grammar, options, logger);
    // test without word-chars
    SmaxDocument document = XmlString.toSmax("<r>Put an r.s.v.p. at the end.</r>");
    ner.scan(document);
    String output = XmlString.fromSmax(document).replaceAll("<\\?.*?\\?>", "").replaceAll("\\s*xmlns:.+?=\".*?\"", "");
    String expectedOutput = "<r>Put an <fn:match id=\"RSVP\">r.s.v.p</fn:match>. at the end.</r>";
    assertEquals(expectedOutput, output);
    // test with word-chars
    options.put("word-chars", ".");
    ner = new NamedEntityRecognition(grammar, options, logger);
    document = XmlString.toSmax("<r>Put an r.s.v.p. at the end.</r>");
    ner.scan(document);
    output = XmlString.fromSmax(document).replaceAll("<\\?.*?\\?>", "").replaceAll("\\s*xmlns:.+?=\".*?\"", "");
    expectedOutput = "<r>Put an r.s.v.p. at the end.</r>";
    assertEquals(expectedOutput, output);
  }

  @Test
  void test_TransparentXML() throws Exception
  {
    String grammar = "RSVP <- RSVP" + "\n";
    Map<String, String> options = new HashMap<String, String>();
    NamedEntityRecognition ner = new NamedEntityRecognition(grammar, options, logger);
    // default balancing is OUTER
    SmaxDocument document = XmlString.toSmax("<r>R<i>SVP!</i></r>");
    ner.scan(document);
    String output = XmlString.fromSmax(document).replaceAll("<\\?.*?\\?>", "").replaceAll("\\s*xmlns:.+?=\".*?\"", "");
    String expectedOutput = "<r><fn:match id=\"RSVP\">R<i>SVP!</i></fn:match></r>";
    assertEquals(expectedOutput, output);
    // test balancing INNER
    options.put("balancing", "INNER");
    ner = new NamedEntityRecognition(grammar, options, logger);
    document = XmlString.toSmax("<r>R<i>SVP!</i></r>");
    ner.scan(document);
    output = XmlString.fromSmax(document).replaceAll("<\\?.*?\\?>", "").replaceAll("\\s*xmlns:.+?=\".*?\"", "");
    expectedOutput = "<r><fn:match id=\"RSVP\">R</fn:match><i>SVP!</i></r>";
    assertEquals(expectedOutput, output);
  }

  private String xmlGrammarString =
      "<grammar>\n" +
      "  <entity id=\"♳\"><name>PET</name><name>polyethylene</name><name>terephthalate</name></entity>\n" +
      "  <entity id=\"♴\"><name>HDPE</name><name>high-density polyethylene</name><name>polyethylene high-density</name></entity>\n" +
      "  <entity id=\"♵\"><name>PVC</name><name>polyvinyl chloride</name><name>polyvinylchloride</name><name>vinyl</name><name>polyvinyl</name></entity>\n" +
      "</grammar>\n";

  @Test
  void test_MatchElement_1() throws Exception
  {
    Element grammar = XmlString.toDomElement(xmlGrammarString);
    Map<String, String> options = new HashMap<String, String>();
    options.put("word-chars", "-");
    options.put("match-element-name", "ric");
    options.put("match-attribute", "symbol");
    NamedEntityRecognition ner = new NamedEntityRecognition(grammar, options, logger);
    SmaxDocument document = XmlString.toSmax("<r>RIC for vinyl and polyethylene</r>");
    ner.scan(document);
    String output = XmlString.fromSmax(document).replaceAll("<\\?.*?\\?>", "").replaceAll("\\s*xmlns:.+?=\".*?\"", "");
    String expectedOutput = "<r>RIC for <ric symbol=\"♵\">vinyl</ric> and <ric symbol=\"♳\">polyethylene</ric></r>";
    assertEquals(expectedOutput, output);
  }

  @Test
  void test_MatchElement_2() throws Exception
  {
    Element grammar = XmlString.toDomElement(xmlGrammarString);
    Map<String, String> options = new HashMap<String, String>();
    options.put("word-chars", "-");
    options.put("match-element-name", "fn:ric");
    options.put("match-attribute", "symbol");
    NamedEntityRecognition ner = new NamedEntityRecognition(grammar, options, logger);
    SmaxDocument document = XmlString.toSmax("<r>RIC for vinyl and polyethylene</r>");
    ner.scan(document);
    String output = XmlString.fromSmax(document).replaceAll("<\\?.*?\\?>", "").replaceAll("\\s*xmlns:.+?=\".*?\"", "");
    String expectedOutput = "<r>RIC for <fn:ric symbol=\"♵\">vinyl</fn:ric> and <fn:ric symbol=\"♳\">polyethylene</fn:ric></r>";
    assertEquals(expectedOutput, output);
  }

  @Test
  void test_MatchElement_3() throws Exception
  {
    Element grammar = XmlString.toDomElement(xmlGrammarString);
    Map<String, String> options = new HashMap<String, String>();
    options.put("word-chars", "-");
    options.put("match-element-name", "ric:image");
    options.put("match-element-namespace-uri", "https://en.wikipedia.org/wiki/Resin_identification_code");
    NamedEntityRecognition ner = new NamedEntityRecognition(grammar, options, logger);
    SmaxDocument document = XmlString.toSmax("<r>RIC for vinyl and polyethylene</r>");
    ner.scan(document);
    String output = XmlString.fromSmax(document).replaceAll("<\\?.*?\\?>", "").replaceAll("\\s*xmlns:.+?=\".*?\"", "");
    String expectedOutput = "<r>RIC for <ric:image id=\"♵\">vinyl</ric:image> and <ric:image id=\"♳\">polyethylene</ric:image></r>";
    assertEquals(expectedOutput, output);
  }

}
