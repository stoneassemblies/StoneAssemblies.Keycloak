package stoneassemblies.keycoak.models;

import stoneassemblies.keycoak.constants.QueryTypes;

public class Query {
    private final String text;
    private final String queryType;

    public Query(String text, String queryType){
        this.text = text;
        this.queryType = queryType;
    }

    public Query(String text){
        this(text, QueryTypes.COMMAND_TEXT);
    }

    public String getText() {
        return text;
    }

    public String getQueryType() {
        return queryType;
    }
}
