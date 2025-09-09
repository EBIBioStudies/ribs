package uk.ac.ebi.biostudies.service.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.api.util.analyzer.AccessFieldAnalyzer;
import uk.ac.ebi.biostudies.auth.Session;
import uk.ac.ebi.biostudies.auth.User;

/** Created by ehsan on 14/03/2017. */
@Service
public class SecurityQueryBuilder {

  private final Logger logger = LogManager.getLogger(SecurityQueryBuilder.class.getName());

  public Query applySecurity(Query originalQuery) throws Throwable {
    return applySecurity(originalQuery, null);
  }

  public Query applySecurity(Query originalQuery, String seckey) throws Throwable {
    QueryParser queryParser = new QueryParser(Constants.Fields.ACCESS, new AccessFieldAnalyzer());
    queryParser.setSplitOnWhitespace(true);
    BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
    StringBuilder securityClause = new StringBuilder();
    User currentUser = Session.getCurrentUser();
    if (currentUser != null && currentUser.isSuperUser()) return originalQuery;

    int counter = 0;
    if (currentUser != null
        && currentUser.getAllow() != null
        && currentUser.getAllow().length > 0) {
      securityClause.append(String.join(" OR ", currentUser.getAllow()));
    } else {
      securityClause.append(Constants.PUBLIC);
    }
    Query allowQuery = queryParser.parse(securityClause.toString());

    // allow if there's a secret key present
    if (seckey != null && seckey != "") {
      QueryParser secretQueryParser =
          new QueryParser(Constants.Fields.SECRET_KEY, new KeywordAnalyzer());
      secretQueryParser.setSplitOnWhitespace(true);
      Query secretKeyQuery = secretQueryParser.parse(seckey);

      BooleanQuery booleanQuery =
          new BooleanQuery.Builder()
              .add(secretKeyQuery, BooleanClause.Occur.SHOULD)
              .add(allowQuery, BooleanClause.Occur.SHOULD)
              .build();
      queryBuilder.add(booleanQuery, BooleanClause.Occur.MUST);
    } else {
      queryBuilder.add(allowQuery, BooleanClause.Occur.MUST);
    }

    counter = 0;
    securityClause = new StringBuilder();
    if (currentUser != null && currentUser.getDeny() != null && currentUser.getDeny().length > 0) {
      String.join(" OR ", currentUser.getAllow());
      Query denyQuery = queryParser.parse(securityClause.toString());
      queryBuilder.add(denyQuery, BooleanClause.Occur.MUST_NOT);
    }

    Query finalQuery = null;
    if (originalQuery != null) {
      queryBuilder.add(originalQuery, BooleanClause.Occur.MUST);
      finalQuery = queryBuilder.build();
    }

    logger.trace("security query: {}", finalQuery);

    return finalQuery;
  }
}
