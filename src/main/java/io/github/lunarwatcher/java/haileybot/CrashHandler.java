package io.github.lunarwatcher.java.haileybot;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
public class CrashHandler {
    private static final Logger logger = LoggerFactory.getLogger(CrashHandler.class);
    private static final List<String> errors = new ArrayList<>();
    private CrashHandler(){}

    public static void error(Throwable e){
        String base = e.toString();
        String reason = e.getLocalizedMessage();
        StringBuilder error = new StringBuilder(base + ": " + reason + "\n");
        for(StackTraceElement element : e.getStackTrace()){
            error.append(StringUtils.repeat(" ", 4)).append(element.toString());
        }
        errors.add(error.toString());
    }

    public static List<String> splitErrors(){
        List<String> errors = new ArrayList<>();
        StringBuilder builder = new StringBuilder();

        for(String e : CrashHandler.errors){
            if(builder.length() < 2000){
                if(builder.length() + e.length() + 4 < 2000){
                    builder.append(e).append("\n\n");
                }else{
                    errors.add(builder.toString());
                    builder = new StringBuilder();
                    builder.append(e).append("\n\n");
                }
            }else{
                logger.warn("Builder.length() > 2000. Dumping in logs...");
                logger.warn(builder.toString());
                builder = new StringBuilder();
            }
        }

        return errors;
    }
}
