package org.gbif.txtree.parsed;

import org.gbif.nameparser.api.NameParser;
import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.ParseResult;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;

import java.util.ArrayList;
import java.util.List;

/**
 * A NameParser stand-in returning each of the three ParseResult variants on demand, so the mapping
 * into ParsedTreeNode can be tested without depending on a real parser implementation.
 *
 * <ul>
 *   <li>a name starting with {@code BOLD:} yields an Unparsable result</li>
 *   <li>a name ending in {@code " sp."} yields an Informal result</li>
 *   <li>anything else yields a Parsed result, always reporting rank UNRANKED</li>
 * </ul>
 */
class FakeNameParser implements NameParser {

  /** The code handed to each call, in call order. May contain nulls. */
  final List<NomCode> codesSeen = new ArrayList<>();

  @Override
  public ParseResult parse(String scientificName, String authorship, Rank rank, NomCode code) {
    codesSeen.add(code);
    if (scientificName.startsWith("BOLD:")) {
      return new ParseResult.Unparsable(NameType.PLACEHOLDER, null, scientificName);
    }
    if (scientificName.endsWith(" sp.")) {
      String genus = scientificName.substring(0, scientificName.indexOf(' '));
      return new ParseResult.Informal(genus, Rank.GENUS, Rank.SPECIES, null, null);
    }
    ParsedName pn = new ParsedName();
    pn.setType(NameType.SCIENTIFIC);
    pn.setRank(Rank.UNRANKED); // deliberately ignores the rank passed in
    pn.setUninomial(scientificName);
    return new ParseResult.Parsed(pn);
  }
}
