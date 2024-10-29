# XML-NER

An implementation of Named Entity Recognition (NER) in XML documents.
This is used in extension modules for [eXist-db](https://github.com/nverwer/exist-ner-xar) and [BaseX](https://github.com/nverwer/basex-ner-xar).

The `named-entity-recognition` function performs NER on an XML document using *transparent* XML, as implemented by [SMAX](https://github.com/nverwer/SMAX).
This means that entity names with embedded XML elements will be recognized, and the markup inserted around the name will be well-formed XML.

For example, the XQuery

```
import module namespace ner = "http://rakensi.com/xquery/functions/ner";
let $grammar :=
  <grammar>
    <entity id="H2O"><name>water</name><name>H2O</name></entity>
    <entity id="CO2"><name>carbondioxide</name><name>CO2</name></entity>
  </grammar>
let $input :=
  <r>CO<sub>2</sub> dissolved in H<sub>2</sub>O forms a weak acid.</r>
let $ner-parse := ner:named-entity-recognition($grammar, map{'match-element-name' : 'chemical'})
return $ner-parse($input)
```

returns

```
<r><chemical id="CO2">CO<sub>2</sub></chemical> dissolved in <chemical id="H2O">H<sub>2</sub>O</chemical> forms a weak acid.</r>
```

## The `named-entity-recognition` function

```
ner:named-entity-recognition(
  $ner-grammar  as item(),
  $options  as map(*)?  := map{}
)  as  function(item()) as node()*

```

The `named-entity-recognition` function takes two parameters, a grammar and a `map` with options.

### `$ner-grammar`

A string or element or URL containing the grammar that specifies the named entities.

When the grammar is a string, the named entities are defined by rules on separate lines of the form:
```
id <- name1 name2 ...
```
or
```
id : name1 name2 ...
```
where the names are separated by tab characters.

When the grammar is an element, it has the form
```
<grammar>
  <entity id="id">
    <name>name1</name>
    <name>name2</name>
    ...
  </entity>
  ...
</grammar>
```

When the grammar is a URL, it must point to a document containing the grammar as a string or XML.

### `$options`

A map with options. The following options are recognized:

* `word-chars` Characters that are significant for matching an entity name. Default is `""`.
    Letters and digits are always significant, but characters like '.' and '-' are not.
    A sequence of non-significant characters and/or whitespace in a text will be treated as a single space during matching.
    This means that an entity name like "e.g." can only be recognized when '.' is in word-chars.
    Whitespace is ignored at the start and end of an entity name, and replaced by a single significant space in the middle.
* `no-word-before` Characters that may not immediately follow a word (next to letters and digits).
    They cannot follow the end of a match. Default is "".
* `no-word-after` Characters that may not immediately precede a word (next to letters and digits).
    Matches can only start on a letter or digit, and not after noWordAfter characters. Default is "".
* `case-insensitive-min-length` The minimum entity-length for case-insensitive matching.
    Text fragments larger than this will be scanned case-insensitively.
    This prevents short words to be recognized as abbreviations.
    Set to -1 to always match case-sensitive. Set to 0 to always match case-insensitive.
    Default is -1.
* `fuzzy-min-length` The minimum entity-length for fuzzy matching.
    Text fragments larger than this may contain characters that are not significant for matching.
    This prevents short words with noise to be recognized as abbreviations.
    Set to -1 to match exact. Set to 0 to match fuzzy.
    Default is -1.
* `balancing` The SMAX balancing strategy that is used when an element for a recognized entity is inserted.
    Default is "OUTER".
* `match-element-name` The name of the element that is inserted around matched text fragments.
    Default is 'fn:match'.
* `match-element-namespace-uri` The namespace URI of the match element.
    This option must be present if the match-element-name contains a namespace prefix other than 'fn:'.
    If the namespace prefix in 'match-element-name' is 'fn:', the default is 'http://www.w3.org/2005/xpath-functions'.
* `match-attribute` The name of the attribute on the match element that will hold the id of the matching entity.
    Default is 'id'.

Setting case-insensitive-min-length to 4 prevents the scanner from recognizing "THE" in "Do the right thing".

Setting fuzzy-min-length to 4 prevents the scanner from recognizing "C F" in "C.F. Gauss was a German mathematician".

With these settings, "R S V P" would be recognized in "Put an r.s.v.p. at the end", provided that '.' is not in word-chars.

Setting case-insensitive-min-length and fuzzy-min-length to 3 or less will recognize "THE" and "C F" in "c.f. the r.s.v.p.".

All sequences of whitespace characters will be treated like a single space, both in the grammar input and the text that is scanned for named entities.

### result

The function returns a `function(item()) as node()*`.
This corresponds to the `scan(SmaxDocument)` function in an instance of `com.rakensi.xml.ner.NamedEntityRecognition`.

This function is implemented by the extension packages for the various XQuery engines.
The `item()` parameter can be a `xs:string` or an XML `node()`.
If the parameter is an `element()`, the same element (possibly with additional nested elements for recognized entities) is returned.
If the parameter is a `xs:string` or a `node()`, the output is a sequence of text nodes and elements for recognized entities.

## Notes

See [Wikipedia: Named Entity Recognition](https://en.wikipedia.org/wiki/Named-entity_recognition).
