# TextTree format

TextTree is a simple format to represent taxonomic trees using indented, plain text.
Each row in a TextTree represent a scientific name. 
Each name can include the authorship and should be given a rank following the name in angular brackets:

```
Abies alba Mill. [species]
```

All rank names are case insensitive, 
but must follow the [rank enumeration](https://github.com/gbif/name-parser/blob/master/name-parser-api/src/main/java/org/gbif/nameparser/api/Rank.java#L32) provided by the GBIF Name Parser.

The indentation level (strictly 2 spaces) and its upper rows
represent the classification:
```
Pinales [order]
  Pinaceae Spreng. [family]
    Abies [genus]
      Abies alba Mill. [species]
      Abies balsamea (L.) Mill. [species]
```

Synonyms are represented as direct, nested children that are prefixed by a `*` asterisk.
```
Pinales [order]
  Pinaceae Spreng. [family]
    Abies [genus]
      Abies alba Mill. [species]
        *Pinus picea L.
      Abies balsamea (L.) Mill. [species]
        *$Pinus balsamea L.
```
 
Basionyms can also be marked by prefixing the name with an additional `$` dollar symbol as in the `Pinus balsamea` example above.

Comments can be given after each name starting with a `#` symbol:
```
Pinales [order]
  Pinaceae Spreng. [family]
    Abies [genus]
      Abies alba Mill. [species]
        *Pinus picea L. [species]
      Abies balsamea (L.) Mill. [species]
        *$Pinus balsamea L. [species]   # this is the basionym of A. balsamea 
```


## Java package
The Java code provided allows to parse and print text trees. The [Tree class](src/main/java/org/gbif/txtree/Tree.java) offers 2 kind of parsed trees:

 1) A simple tree which keeps the parsed rank and the name incl authorship as it was given in a single string.
 2) A parsed tree which uses the [GBIF Name Parser](https://github.com/gbif/name-parser) to parse each name and provide in addition to the rank and name string
    also a ParsedName instance.
    
The Tree class also offers a simple `verify` method that checks if the given tree data is parsable. 
When parsing badly formatted trees the parser on purpose fails and does not try to read the remaining bits.
