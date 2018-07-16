package ca.joeltherrien.randomforest.responses.competingrisk;

import ca.joeltherrien.randomforest.DataLoader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVRecord;

@Data
public class CompetingRiskResponse {

    private final int delta;
    private final double u;

    public boolean isCensored(){
        return delta == 0;
    }


    @RequiredArgsConstructor
    public static class CompetingResponseLoader implements DataLoader.ResponseLoader<CompetingRiskResponse>{

        private final String deltaName;
        private final String uName;

        public CompetingResponseLoader(ObjectNode node){
            this.deltaName = node.get("delta").asText();
            this.uName = node.get("u").asText();
        }

        @Override
        public CompetingRiskResponse parse(CSVRecord record) {
            final int delta = Integer.parseInt(record.get(deltaName));
            final double u = Double.parseDouble(record.get(uName));

            return new CompetingRiskResponse(delta, u);
        }
    }

}
