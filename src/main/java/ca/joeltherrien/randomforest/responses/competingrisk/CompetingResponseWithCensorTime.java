package ca.joeltherrien.randomforest.responses.competingrisk;

import ca.joeltherrien.randomforest.DataLoader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVRecord;

/**
 * See Ishwaran paper on splitting rule modelled after Gray's test. This requires that we know the censor times.
 *
 */
@Data
public class CompetingResponseWithCensorTime extends CompetingResponse{
    private final double c;

    public CompetingResponseWithCensorTime(int delta, double u, double c) {
        super(delta, u);
        this.c = c;
    }

    @RequiredArgsConstructor
    public static class CompetingResponseWithCensorTimeLoader implements DataLoader.ResponseLoader<CompetingResponseWithCensorTime>{

        private final String deltaName;
        private final String uName;
        private final String cName;

        public CompetingResponseWithCensorTimeLoader(ObjectNode node){
            this.deltaName = node.get("delta").asText();
            this.uName = node.get("u").asText();
            this.cName = node.get("c").asText();
        }

        @Override
        public CompetingResponseWithCensorTime parse(CSVRecord record) {
            final int delta = Integer.parseInt(record.get(deltaName));
            final double u = Double.parseDouble(record.get(uName));
            final double c = Double.parseDouble(record.get(cName));

            return new CompetingResponseWithCensorTime(delta, u, c);
        }
    }
}
