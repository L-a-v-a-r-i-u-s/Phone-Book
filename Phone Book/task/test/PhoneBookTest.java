import org.hyperskill.hstest.stage.StageTest;
import org.hyperskill.hstest.testcase.CheckResult;
import org.hyperskill.hstest.testcase.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneBookTest extends StageTest {

    private long timeOnTestStart;

    @Override
    public List<TestCase> generate() {
        timeOnTestStart = System.currentTimeMillis();
        return Arrays.asList(
                new TestCase().setTimeLimit(30 * 60 * 1000)
        );
    }


    private CheckResult checkPhrases(String reply, String... phrases) {
        reply = reply.toLowerCase();
        for (String phrase : phrases) {
            if (!reply.contains(phrase.toLowerCase())) {
                return CheckResult.wrong("Not found the part `" + phrase + "` in your output.");
            }
        }
        return CheckResult.correct();
    }

    private List<String> findAll(String reply, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(reply);
        List<String> groups = new ArrayList<>();
        while (matcher.find()) {
            groups.add(matcher.group());
        }
        return groups;
    }

    private String timeRegex = "(\\d+)\\s*min.*?(\\d+)\\s*sec.*?(\\d+)\\s*ms";
    private Pattern timeRegexPattern = Pattern.compile(timeRegex);

    private long parseTimestamp(String timestamp) {
        Matcher matcher = timeRegexPattern.matcher(timestamp);
        if (!matcher.matches() || matcher.groupCount() < 3) {
            throw new IllegalStateException("???Not matches the line " + timestamp);
        }
        int min = Integer.parseInt(matcher.group(1));
        int sec = Integer.parseInt(matcher.group(2));
        int ms = Integer.parseInt(matcher.group(3));
        return ms + sec * 1000 + min * 1000 * 60;
    }


    @Override
    public CheckResult check(String reply, Object clue) {
        long realTime = System.currentTimeMillis() - timeOnTestStart;
        reply = reply.toLowerCase();
        CheckResult res = checkPhrases(reply,
                "found",
                "min.",
                "sec.",
                "ms.",
                "sorting time",
                "searching time",
                "linear search",
                "bubble sort",
                "jump search",
                "quick sort",
                "binary search",
                "hash table",
                "creating time"
        );
        if (!res.isCorrect()) {
            return res;
        }

        List<String> stat1 = findAll(reply, "500 / 500");
        List<String> stat2 = findAll(reply, "500/500");

        if (stat1.size() + stat2.size() < 4) {
            return CheckResult.wrong("Your output should contain 4 times the phrase `500 / 500`");
        }

        List<String> timestamps = findAll(reply, timeRegex);
        if (timestamps.size() != 10) {
            return CheckResult.wrong("Your output should contain 10 timer outputs, but found "
                    + timestamps.size());
        }
        // should not fail..
        long t1 = parseTimestamp(timestamps.get(0));
        long t2 = parseTimestamp(timestamps.get(1));
        long t3 = parseTimestamp(timestamps.get(2));
        long t4 = parseTimestamp(timestamps.get(3));
        // qsort
        long t5 = parseTimestamp(timestamps.get(4));
        long t6 = parseTimestamp(timestamps.get(5));
        long t7 = parseTimestamp(timestamps.get(6));
        // hash table
        long t8 = parseTimestamp(timestamps.get(7));
        long t9 = parseTimestamp(timestamps.get(8));
        long t10 = parseTimestamp(timestamps.get(9));

        if (Math.abs(t3 + t4 - t2) > 100) {
            return CheckResult.wrong("Your third and fourth timer outputs in total (bubble sorting and searching) " +
                    "should be equal to the second (total search time).");
        }
        if (Math.abs(t6 + t7 - t5) > 100) {
            return CheckResult.wrong("Your 6-th and 7-th timer outputs in total (qsort and searching) " +
                    "should be equal to the 5-th (total search time).");
        }
        if (Math.abs(t9 + t10 - t8) > 100) {
            return CheckResult.wrong("Your 9-th and 10-th timer outputs in total (creating hashtable and searching) " +
                    "should be equal to the 8-th (total search time).");
        }

        long estimatedTime = t1 + t2 + t5 + t8;
        if (realTime < 1000) {
            return CheckResult.wrong("Your program completes too fast. Faster than a second!");
        }

        if (Math.abs(estimatedTime - realTime) > estimatedTime * 0.3) {
            return CheckResult.wrong("Your estimated time is not similar to real time the program works. " +
                    "Real time: " + realTime + "ms, estimated time: " + estimatedTime + "ms");
        }

        if (t8 >= t5) {
            return CheckResult.wrong("Your hashtable works slower, than quick sort + binary search.");
        }
        return CheckResult.correct();
    }
}
