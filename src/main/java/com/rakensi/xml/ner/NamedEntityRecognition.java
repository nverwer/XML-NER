package com.rakensi.xml.ner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.greenmercury.smax.Balancing;
import org.greenmercury.smax.SmaxDocument;
import org.greenmercury.smax.SmaxElement;
import org.greenmercury.smax.SmaxException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * A SMAX document transformer that inserts markup around named entities specified by a grammar.
 * This is used in the <code>ner:named-entity-recognition</code> function.
 *<p>
 * The transformer takes the following parameters:
 * <ul>
 *   <li>grammar A string or element or URL containing the grammar that specifies the named entities.
 *       When the grammar is a string, the named entities are defined by rules on separate lines of the form:
 *       <code>id &lt;- name1 name2 ...</code>
 *       or
 *       <code>id : name1 name2 ...</code>
 *       where the names are separated by tab characters.
 *       When the grammar is an element, it has the form
 *       <code>
 *         &lt;grammar>
 *           &lt;entity id="id">&lt;name>name1&lt;/name>...&lt;/entity>...
 *         &lt;/grammar>
 *       </code>
 *       When the grammar is a URL, it must point to a document containing the grammar as a string or XML.
 *   </li>
 *   <li>options A map with options. The following options are recognized:
 *     <ul>
 *       <li>word-chars Characters that are significant for matching an entity name. Default is "".
 *           Letters and digits are always significant, but characters like '.' and '-' are not.
 *           A sequence of non-significant characters and/or whitespace in a text will be treated as a single space during matching.
 *           This means that an entity name like "e.g." can only be recognized when '.' is in word-chars.
 *           Whitespace is ignored at the start and end of an entity name, and replaced by a single significant space in the middle.</li>
 *       <li>no-word-before Characters that may not immediately follow a word (next to letters and digits).
 *           They cannot follow the end of a match. Default is "".</li>
 *       <li>no-word-after Characters that may not immediately precede a word (next to letters and digits).
 *           Matches can only start on a letter or digit, and not after noWordAfter characters. Default is "".</li>
 *       <li>case-insensitive-min-length The minimum entity-length for case-insensitive matching.
 *           Text fragments larger than this will be scanned case-insensitively.
 *           This prevents short words to be recognized as abbreviations.
 *           Set to -1 to always match case-sensitive. Set to 0 to always match case-insensitive.
 *           Default is -1.</li>
 *       <li>fuzzy-min-length The minimum entity-length for fuzzy matching.
 *           Text fragments larger than this may contain characters that are not significant for matching.
 *           This prevents short words with noise to be recognized as abbreviations.
 *           Set to -1 to match exact. Set to 0 to match fuzzy.
 *           Default is -1.</li>
 *       <li>balancing The SMAX balancing strategy that is used when an element for a recognized entity is inserted.
 *           Default is "OUTER".</li>
 *       <li>match-element-name The name of the element that is inserted around matched text fragments.
 *           Default is 'fn:match'.
 *       <li>match-element-namespace-uri The namespace URI of the match element.
 *           This option must be present if the match-element-name contains a namespace prefix other than 'fn:'.
 *           If the namespace prefix in 'match-element-name' is 'fn:', the default is 'http://www.w3.org/2005/xpath-functions'.</li>
 *       <li>match-attribute The name of the attribute on the match element that will hold the id of the matching entity.
 *           Default is 'id'.</li>
 *     </ul>
 *   </li>
 * </ul>
 *<p>
 * Setting case-insensitive-min-length to 4 prevents the scanner from recognizing "THE" in "Do the right thing".
 * Setting fuzzy-min-length to 4 prevents the scanner from recognizing "C F" in "C.F. Gauss was a German mathematician".
 * With these settings, "R S V P" would be recognized in "Put an r.s.v.p. at the end", provided that '.' is not in word-chars.
 * Setting case-insensitive-min-length and fuzzy-min-length to 3 or less will recognize "THE" and "C F" in "c.f. the r.s.v.p.".
 *<p>
 * All sequences of whitespace characters will be treated like a single space,
 * both in the grammar input and the text that is scanned for named entities.
 *<p>
 * @see <a href="https://en.wikipedia.org/wiki/Named-entity_recognition">Wikipedia: Named Entity Recognition</a>
 * @author Rakensi
 */
public class NamedEntityRecognition
{

  // Where to log to.
  private Logger logger;

  // Match element.
  private String matchElementName;
  private String matchElementNamespaceUri;
  private String matchAttribute;

  // The minimum entity-lengths for case-insensitive or fuzzy matching.
  private int caseInsensitiveMinLength;
  private int fuzzyMinLength;

  // Special characters, see parameter description.
  private String wordChars;
  private String noWordBefore;
  private String noWordAfter;

  // Balancing strategy to use when inserting elements into a SMAX document.
  private Balancing balancing;

  // The TrieNER instance used for scanning.
  private TrieNER triener = null;

  // The (sub-)document that is being transformed.
  private SmaxDocument transformedDocument;

  // A SAX parser for parsing grammars from file.
  SAXParser saxParser = null;

  /**
   * This constructor compiles the named entities grammar from a String.
   */
  public NamedEntityRecognition(String grammar, Map<String, String> options, Logger logger)
  throws Exception
  {
    this(options, logger);
    if (grammar == null) grammar = "";
    readGrammar(grammar);
  }

  /**
   * This constructor compiles the named entities grammar from an Element.
   */
  public NamedEntityRecognition(Element grammar, Map<String, String> options, Logger logger)
  throws Exception
  {
    this(options, logger);
    readGrammar(grammar);
  }

  /**
   * This constructor compiles the named entities grammar from a URL.
   */
  public NamedEntityRecognition(URL grammar, Map<String, String> options, Logger logger)
  throws Exception
  {
    this(options, logger);
    readGrammar(grammar);
  }

  /**
   * Private constructor for common parameters.
   * @param matchElementTemplate
   * @param options
   * @param logger
   * @throws Exception
   */
  private NamedEntityRecognition(Map<String, String> options, Logger logger)
  throws Exception
  {
    this.logger = logger;
    this.caseInsensitiveMinLength = getOption(options, "case-insensitive-min-length", -1);
    this.fuzzyMinLength = getOption(options, "fuzzy-min-length", -1);
    this.wordChars = getOption(options, "word-chars", "");
    this.noWordBefore = getOption(options, "no-word-before", "");
    this.noWordAfter = getOption(options, "no-word-after", "");
    this.balancing = getOption(options, "balancing", Balancing.OUTER);
    this.matchElementName = getOption(options, "match-element-name", "fn:match");
    String defaultNamespaceUri = this.matchElementName.startsWith("fn:") ? "http://www.w3.org/2005/xpath-functions" : null;
    this.matchElementNamespaceUri = getOption(options, "match-element-namespace-uri", defaultNamespaceUri);
    this.matchAttribute = getOption(options, "match-attribute", "id");
    if (this.matchElementName.contains(":") && this.matchElementNamespaceUri == null) {
      throw new IllegalArgumentException("A match-element-namespace-uri must be defined for the match-element-name '"+this.matchElementName+"'");
    }
    initTrieNER();
  }

  private String getOption(Map<String, String> options, String key, String defaultValue) {
    return Optional.ofNullable(options.get(key)).orElse(defaultValue);
  }

  private int getOption(Map<String, String> options, String key, int defaultValue) {
    return Optional.ofNullable(options.get(key)).map(v -> Integer.parseInt(v)).orElse(defaultValue);
  }

  private Balancing getOption(Map<String, String> options, String key, Balancing defaultValue) {
    return Optional.ofNullable(options.get(key)).map(v -> Balancing.parseBalancing(v)).orElse(defaultValue);
  }

  private void initTrieNER()
  {
    triener = new TrieNER(wordChars, noWordBefore, noWordAfter, logger) {
      @Override
      public void match(CharSequence text, int start, int end, List<String> ids) {
        SmaxElement matchElement = new SmaxElement(matchElementNamespaceUri, matchElementName);
        try
        {
          matchElement.setAttribute(matchAttribute, String.join("\t", ids));
        }
        catch (SmaxException e)
        {
          throw new RuntimeException("This is not supposed to happen.", e);
        }
        transformedDocument.insertMarkup(matchElement, balancing, start, end);
      }
      @Override
      public void noMatch(CharSequence text, int start, int end) {
        // No action is needed.
      }
    };
  }

  private void readGrammar(String grammar) throws Exception
  {
    try (
      StringReader grammarStringReader = new StringReader(grammar);
      BufferedReader grammarReader = new BufferedReader(grammarStringReader);
    ) {
      readGrammar(grammarReader);
    } catch (IOException e) {
      throw new Exception(e);
    }
  }

  private void readGrammar(URL grammar) throws Exception
  {
    // First, try to parse the document that the URL points to as XML.
    if (saxParser == null) {
      SAXParserFactory  factory = SAXParserFactory.newInstance();
      saxParser = factory.newSAXParser();
    }
    try {
      saxParser.parse(grammar.toString(), new GrammarSAXHandler(triener.getTrie()));
    } catch (SAXParseException spe) {
      // If the document is not XML, try to parse as text.
      try (
        InputStream grammarStream = grammar.openStream();
        InputStreamReader grammarStreamReader = new InputStreamReader(grammarStream);
        BufferedReader grammarReader = new BufferedReader(grammarStreamReader);
      ) {
        readGrammar(grammarReader);
      } catch (Exception e) {
        throw new Exception("The grammar URL "+grammar+" cannot be parsed as XML ("+spe.getMessage()+") or text ("+e.getMessage()+")", e);
      }
    }
  }

  private void readGrammar(BufferedReader grammarReader) throws Exception
  {
    String line;
    int lineNumber = 0;
    try {
      TrieScanner trie = triener.getTrie();
      while ((line = grammarReader.readLine()) != null) {
        ++lineNumber;
        line = line.trim();
        if (line.length() > 0) {
          String[] parts = line.split("\\s*(<-|:)\\s*", 2);
          if (parts.length != 2) {
            throw new Exception("Bad trie syntax in line "+lineNumber+": "+line+
                "\n\tEvery line must contain two parts separated by '<-' or ':'.");
          }
          if (parts[1].equals("")) {
            throw new Exception("Bad trie syntax in line "+lineNumber+": "+line+
                "\n\tThe second part of a rule must not be empty).");
          }
          String nttid = parts[0];
          parts = parts[1].split("\\t");
          for (int i = 0; i < parts.length; ++i) {
            String nntname = parts[i];
            trie.put(nntname, nttid);
          }
        }
      }
      logger.info("Trie: "+trie.nrKeys()+" keys, "+trie.sizeInBytes()/1048576+" megabytes");
    } catch (IOException e) {
      throw new Exception(e);
    }
  }

  /**
   * Parse a grammar represented by XML.
   * The names of the elements are not significant.
   * The elements below the root element must have on attribute, which is the id of an entity.
   * The elements 2 levels below the root element contain names for an entity in their text content.
   * @param grammar
   * @throws Exception
   */
  private void readGrammar(Element grammar) throws Exception
  {
    TrieScanner trie = triener.getTrie();
    NodeList entityNodes = grammar.getChildNodes();
    int entitiesCount = entityNodes.getLength();
    for (int entityIndex = 0; entityIndex < entitiesCount; ++entityIndex) {
      Node entityNode = entityNodes.item(entityIndex);
      if (entityNode instanceof Element) {
        NamedNodeMap entityAttributes = entityNode.getAttributes();
        if (entityAttributes.getLength() != 1) throw new Exception("The entity elements in a NER grammar must have exactly one attribute.");
        String entityId = entityAttributes.item(0).getNodeValue();
        NodeList nameNodes = entityNode.getChildNodes();
        int namesCount = nameNodes.getLength();
        for (int nameIndex = 0; nameIndex < namesCount; ++ nameIndex) {
          String name = nameNodes.item(nameIndex).getTextContent();
          trie.put(name, entityId);
        }
      }
    }
    logger.info("Trie: "+trie.nrKeys()+" keys, "+trie.sizeInBytes()/1048576+" megabytes");
  }

  public void scan(SmaxDocument document) {
    transformedDocument = document;
    triener.scan(document.getContent(), caseInsensitiveMinLength, fuzzyMinLength);
  }

  /**
   * A class that parses a grammar file and builds a trie.
   */
  private class GrammarSAXHandler extends org.xml.sax.helpers.DefaultHandler {
    private TrieScanner trie ;
    private int level = 0;
    private String entityId = null;
    private StringBuilder name = null;

    public GrammarSAXHandler(TrieScanner trie) {
      this.trie = trie;
    }

    @Override
    public void startDocument() throws SAXException
    {
      level = 0;
      name = new StringBuilder();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
      ++level;
      if (level == 2) {
        if (attributes.getLength() != 1) throw new SAXException("The entity elements in a NER grammar must have exactly one attribute.");
        entityId = attributes.getValue(0);
      } else if (level == 3) {
        name.setLength(0);
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
      if (level == 3) {
        trie.put(name.toString(), entityId);
      }
      --level;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException
    {
      if (level == 3) {
        name.append(ch, start, length);
      }
    }
  }

}
