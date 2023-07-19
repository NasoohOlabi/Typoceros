import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Election {
    Map<String, Integer> votes = new HashMap<>();
    public final int population;
    public final String wordInQuestion;
    private int currentVotes = 0;
    private final Logger _logger = new Logger("Election");
    private Optional<String> winner = Optional.empty();

    public Election(int population, String wordInQuestion) {
        this.population = population;
        this.wordInQuestion = wordInQuestion;
        _logger.trace("#".repeat(25) + " new Election: " + wordInQuestion + " " + "#".repeat(25));
    }

    public void addVote(String word) {
        votes.put(word,
                votes.getOrDefault(word, 0) + 1);
        currentVotes++;
    }


    private Optional<String> calcWinner() {
        _logger.trace("calcWinner(): currentVotes=" + currentVotes);
        Map.Entry<String, Integer> firstCandidate = null;
        Map.Entry<String, Integer> secondCandidate = null;
        for (Map.Entry<String, Integer> entry :
                votes.entrySet()) {
            if (firstCandidate == null) {
                firstCandidate = entry;
            } else if (secondCandidate == null) {
                secondCandidate = entry;
            } else if (entry.getValue() > firstCandidate.getValue()) {
                secondCandidate = firstCandidate;
                firstCandidate = entry;
            } else if (entry.getValue() > secondCandidate.getValue()) {
                secondCandidate = entry;
            }
        }
        if (firstCandidate == null) {
            _logger.trace("No First Candidate probably not enough votes");
        } else if (secondCandidate == null) {
            _logger.trace("No Second Candidate firstCandidate: " + firstCandidate);
            return Optional.of(firstCandidate.getKey());
        } else {
            _logger.trace("firstCandidate: " + firstCandidate);
            _logger.trace("secondCandidate: " + secondCandidate);
            var isThereWinner = clearWinner(firstCandidate, secondCandidate);
            if (isThereWinner) {
                _logger.trace("Election winner: " + firstCandidate);
                return Optional.of(firstCandidate.getKey());
            } else {
                _logger.trace("Unfortunately There is no clear winner");
            }
        }
        return Optional.empty();
    }

    private boolean clearWinner(Map.Entry<String, Integer> firstCandidate, Map.Entry<String, Integer> secondCandidate) {
        double firstCandidatePercentage = (double) firstCandidate.getValue() / currentVotes;
        double secondCandidatePercentage = (double) secondCandidate.getValue() / currentVotes;
        _logger.trace(
                String.format(
                        "clearWinner: " +
                                "firstCandidatePercentage: %f - secondCandidatePercentage: %f" +
                                " > (Config.min_winning_percentage / 2): %f = %b"
                        , firstCandidatePercentage
                        , secondCandidatePercentage
                        , (Config.min_winning_percentage / 2)
                        , firstCandidatePercentage - secondCandidatePercentage > (Config.min_winning_percentage / 2)));
        return firstCandidatePercentage - secondCandidatePercentage > (Config.min_winning_percentage / 2);
    }

    public Optional<String> getWinner() {
        if (winner.isEmpty())
            winner = calcWinner();
        return winner;
    }

    public String report() {
        var copy = new HashMap<String, Double>();
        for (String key :
                votes.keySet()) {
            copy.put(key, (double) votes.get(key) / currentVotes);
        }
        var report = "Election Results: {wordInQuestion=" + wordInQuestion + "\tcurrentVotes=" + currentVotes + "\tWinner=" + getWinner() + "\tvotes:" + copy.toString() + "}";
        _logger.trace(report);
        return report;
    }

    public int getCurrentVotes() {
        return currentVotes;
    }

    public void setCurrentVotes(int currentVotes) {
        this.currentVotes = currentVotes;
    }
}
