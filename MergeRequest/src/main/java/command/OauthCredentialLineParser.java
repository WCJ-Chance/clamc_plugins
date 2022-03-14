package command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OauthCredentialLineParser implements LineParser {
    private static final Logger logger = LoggerFactory.getLogger(OauthCredentialLineParser.class);

    @Override
    public String onParseLine(String line) {
        if (line.contains("http://oauth2:")) {
            Pattern pattern = Pattern.compile("oauth2:(\\w+)@");
            Matcher matcher = pattern.matcher(line);
            String replace = matcher.replaceAll("");
            logger.info("Parse the line from $line to $replace");
            return replace;
        }
        return line;
    }
}
