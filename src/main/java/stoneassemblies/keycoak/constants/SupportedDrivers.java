package stoneassemblies.keycoak.constants;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class SupportedDrivers {
    public final static String MS_SQL_SERVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    public final static String[] ALL = new String[]{ MS_SQL_SERVER };

    public static Collection<String> getAll(){
        return Arrays.stream(SupportedDrivers.ALL).collect(Collectors.toList());
    }
}

