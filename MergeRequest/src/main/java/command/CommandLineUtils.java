package command;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CommandLineUtils {
    private static final Logger logger = LoggerFactory.getLogger(CommandLineUtils.class);

    private static final List<OauthCredentialLineParser> lineParser = new ArrayList<>();

    static {
        lineParser.add(new OauthCredentialLineParser());
    }

    public static String execute(String command, File workspace, String prefix) throws IOException {
        StringBuilder result = new StringBuilder();

        CommandLine cmdLine = CommandLine.parse(command);
        CommandLineExecutor executor = new CommandLineExecutor();
        if (workspace != null) {
            executor.setWorkingDirectory(workspace);
        }

        LogOutputStream outputStream = new LogOutputStream() {
            @Override
            public void processLine(String line, int level) {
                if (line == null) {
                    return;
                }
                String tmpLine = prefix + line;

                for (OauthCredentialLineParser it : lineParser) {
                    tmpLine = it.onParseLine(tmpLine);
                }
                result.append(tmpLine).append("\n");
            }
        };

        LogOutputStream errorStream = new LogOutputStream() {
            @Override
            public void processLine(String line, int level){
                if (line == null) {
                    return;
                }

                String tmpLine = prefix + line;

                for (OauthCredentialLineParser it : lineParser) {
                    tmpLine = it.onParseLine(tmpLine);
                }
                result.append(tmpLine).append("\n");

            }
        };
        executor.setStreamHandler(new PumpStreamHandler(outputStream, errorStream));
        try {
            int exitCode = executor.execute(cmdLine);
            if (exitCode != 0) {
                throw new RuntimeException("$prefix Script command execution failed with exit code($exitCode)");
            }
        } catch (Throwable t){
            logger.warn("Fail to execute the command($command)", t);
            throw t;
        }
        return result.toString();
    }

}
