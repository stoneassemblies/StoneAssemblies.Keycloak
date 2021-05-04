package stoneassemblies.keycoak.constants;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class QueryTypes {
    public final static String STORED_PROCEDURE = "Stored Procedure";

    public final static String COMMAND_TEXT = "Command Text";

    public final static String[] ALL = new String[]{ STORED_PROCEDURE, COMMAND_TEXT };

    public static Collection<String> getAll(){
        return Arrays.stream(QueryTypes.ALL).collect(Collectors.toList());
    }
}
