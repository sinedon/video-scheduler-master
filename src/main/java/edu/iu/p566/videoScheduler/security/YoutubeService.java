package edu.iu.p566.videoScheduler.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class YoutubeService {

    public long getVideoDuration(String youtubeURL) {

        RestTemplate restTemplate = new RestTemplate();

        String html = restTemplate.getForObject(youtubeURL, String.class);

        Pattern pattern = Pattern.compile("\"approxDurationMs\":\"(\\d+)\"");
        Matcher matcher = pattern.matcher(html);

        if(matcher.find()) {
            long ms = Long.parseLong(matcher.group(1));
            return ms / 1000;
        }

        return 0;
    }
}