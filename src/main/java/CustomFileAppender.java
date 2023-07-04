import org.apache.log4j.FileAppender;
import org.apache.log4j.spi.LoggingEvent;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CustomFileAppender extends FileAppender {

    @Override
    public void activateOptions() {
        if (fileName != null) {
            fileName = fileName.replace("%timestamp%", getFormattedTimestamp());
        }
        super.activateOptions();
    }

    private String getFormattedTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        return dateFormat.format(new Date());
    }

    @Override
    public void append(LoggingEvent event) {
        if (fileName != null) {
            fileName = fileName.replace("%timestamp%", getFormattedTimestamp());
        }
        super.append(event);
    }
}
