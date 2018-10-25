package ca.joeltherrien.randomforest.competingrisk;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskGraySetsImpl;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponseWithCensorTime;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskSetsImpl;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCalculatingCompetingRiskSets {

    public List<CompetingRiskResponseWithCensorTime> generateData(){
        final List<CompetingRiskResponseWithCensorTime> data = new ArrayList<>();

        data.add(new CompetingRiskResponseWithCensorTime(1, 1, 3));
        data.add(new CompetingRiskResponseWithCensorTime(1, 1, 3));
        data.add(new CompetingRiskResponseWithCensorTime(0, 1, 1));
        data.add(new CompetingRiskResponseWithCensorTime(1, 2, 2.5));
        data.add(new CompetingRiskResponseWithCensorTime(2, 3, 4));
        data.add(new CompetingRiskResponseWithCensorTime(0, 3, 3));
        data.add(new CompetingRiskResponseWithCensorTime(1, 4, 4));
        data.add(new CompetingRiskResponseWithCensorTime(0, 5, 5));
        data.add(new CompetingRiskResponseWithCensorTime(2, 6, 7));

        return data;
    }

    @Test
    public void testCalculatingSets(){
        final List data = generateData();

        final CompetingRiskSetsImpl sets = CompetingRiskUtils.calculateSetsEfficiently(data, new int[]{1,2});

        final List<Double> times = sets.getEventTimes();
        assertEquals(5, times.size());

        // Times
        assertEquals(1.0, times.get(0).doubleValue());
        assertEquals(2.0, times.get(1).doubleValue());
        assertEquals(3.0, times.get(2).doubleValue());
        assertEquals(4.0, times.get(3).doubleValue());
        assertEquals(6.0, times.get(4).doubleValue());

        // Number of Events
        assertEquals(2, sets.getNumberOfEvents(1.0, 1));
        assertEquals(0, sets.getNumberOfEvents(1.0, 2));

        assertEquals(1, sets.getNumberOfEvents(2.0, 1));
        assertEquals(0, sets.getNumberOfEvents(2.0, 2));

        assertEquals(0, sets.getNumberOfEvents(3.0, 1));
        assertEquals(1, sets.getNumberOfEvents(3.0, 2));

        assertEquals(1, sets.getNumberOfEvents(4.0, 1));
        assertEquals(0, sets.getNumberOfEvents(4.0, 2));

        assertEquals(0, sets.getNumberOfEvents(6.0, 1));
        assertEquals(1, sets.getNumberOfEvents(6.0, 2));

        // Make sure it doesn't break for other times
        assertEquals(0, sets.getNumberOfEvents(5.5, 1));
        assertEquals(0, sets.getNumberOfEvents(5.5, 2));


        // Risk set
        assertEquals(9, sets.getRiskSet(1).evaluate(0.5));
        assertEquals(9, sets.getRiskSet(2).evaluate(0.5));

        assertEquals(9, sets.getRiskSet(1).evaluate(1.0));
        assertEquals(9, sets.getRiskSet(2).evaluate(1.0));

        assertEquals(6, sets.getRiskSet(1).evaluate(1.5));
        assertEquals(6, sets.getRiskSet(2).evaluate(1.5));

        assertEquals(6, sets.getRiskSet(1).evaluate(2.0));
        assertEquals(6, sets.getRiskSet(2).evaluate(2.0));

        assertEquals(5, sets.getRiskSet(1).evaluate(2.3));
        assertEquals(5, sets.getRiskSet(2).evaluate(2.3));

        assertEquals(5, sets.getRiskSet(1).evaluate(2.5));
        assertEquals(5, sets.getRiskSet(2).evaluate(2.5));

        assertEquals(5, sets.getRiskSet(1).evaluate(2.7));
        assertEquals(5, sets.getRiskSet(2).evaluate(2.7));

        assertEquals(5, sets.getRiskSet(1).evaluate(3.0));
        assertEquals(5, sets.getRiskSet(2).evaluate(3.0));

        assertEquals(3, sets.getRiskSet(1).evaluate(3.5));
        assertEquals(3, sets.getRiskSet(2).evaluate(3.5));

        assertEquals(3, sets.getRiskSet(1).evaluate(4.0));
        assertEquals(3, sets.getRiskSet(2).evaluate(4.0));

        assertEquals(2, sets.getRiskSet(1).evaluate(4.5));
        assertEquals(2, sets.getRiskSet(2).evaluate(4.5));

        assertEquals(2, sets.getRiskSet(1).evaluate(5.0));
        assertEquals(2, sets.getRiskSet(2).evaluate(5.0));

        assertEquals(1, sets.getRiskSet(1).evaluate(5.5));
        assertEquals(1, sets.getRiskSet(2).evaluate(5.5));

        assertEquals(1, sets.getRiskSet(1).evaluate(6.0));
        assertEquals(1, sets.getRiskSet(2).evaluate(6.0));

        assertEquals(0, sets.getRiskSet(1).evaluate(6.5));
        assertEquals(0, sets.getRiskSet(2).evaluate(6.5));

        assertEquals(0, sets.getRiskSet(1).evaluate(7.0));
        assertEquals(0, sets.getRiskSet(2).evaluate(7.0));

        assertEquals(0, sets.getRiskSet(1).evaluate(7.5));
        assertEquals(0, sets.getRiskSet(2).evaluate(7.5));

    }

    @Test
    public void testCalculatingGraySets(){
        final List<CompetingRiskResponseWithCensorTime> data = generateData();

        final CompetingRiskGraySetsImpl sets = CompetingRiskUtils.calculateGraySetsEfficiently(data, new int[]{1,2});

        final List<Double> times = sets.getEventTimes();
        assertEquals(5, times.size());

        // Times
        assertEquals(1.0, times.get(0).doubleValue());
        assertEquals(2.0, times.get(1).doubleValue());
        assertEquals(3.0, times.get(2).doubleValue());
        assertEquals(4.0, times.get(3).doubleValue());
        assertEquals(6.0, times.get(4).doubleValue());

        // Number of Events
        assertEquals(2, sets.getNumberOfEvents(1.0, 1));
        assertEquals(0, sets.getNumberOfEvents(1.0, 2));

        assertEquals(1, sets.getNumberOfEvents(2.0, 1));
        assertEquals(0, sets.getNumberOfEvents(2.0, 2));

        assertEquals(0, sets.getNumberOfEvents(3.0, 1));
        assertEquals(1, sets.getNumberOfEvents(3.0, 2));

        assertEquals(1, sets.getNumberOfEvents(4.0, 1));
        assertEquals(0, sets.getNumberOfEvents(4.0, 2));

        assertEquals(0, sets.getNumberOfEvents(6.0, 1));
        assertEquals(1, sets.getNumberOfEvents(6.0, 2));

        // Make sure it doesn't break for other times
        assertEquals(0, sets.getNumberOfEvents(5.5, 1));
        assertEquals(0, sets.getNumberOfEvents(5.5, 2));


        // Risk set
        assertEquals(9, sets.getRiskSet(1).evaluate(0.5));
        assertEquals(9, sets.getRiskSet(2).evaluate(0.5));

        assertEquals(9, sets.getRiskSet(1).evaluate(1.0));
        assertEquals(9, sets.getRiskSet(2).evaluate(1.0));

        assertEquals(6, sets.getRiskSet(1).evaluate(1.5));
        assertEquals(8, sets.getRiskSet(2).evaluate(1.5));

        assertEquals(6, sets.getRiskSet(1).evaluate(2.0));
        assertEquals(8, sets.getRiskSet(2).evaluate(2.0));

        assertEquals(5, sets.getRiskSet(1).evaluate(2.3));
        assertEquals(8, sets.getRiskSet(2).evaluate(2.3));

        assertEquals(5, sets.getRiskSet(1).evaluate(2.5));
        assertEquals(7, sets.getRiskSet(2).evaluate(2.5));

        assertEquals(5, sets.getRiskSet(1).evaluate(2.7));
        assertEquals(7, sets.getRiskSet(2).evaluate(2.7));

        assertEquals(5, sets.getRiskSet(1).evaluate(3.0));
        assertEquals(5, sets.getRiskSet(2).evaluate(3.0));

        assertEquals(4, sets.getRiskSet(1).evaluate(3.5));
        assertEquals(3, sets.getRiskSet(2).evaluate(3.5));

        assertEquals(3, sets.getRiskSet(1).evaluate(4.0));
        assertEquals(3, sets.getRiskSet(2).evaluate(4.0));

        assertEquals(2, sets.getRiskSet(1).evaluate(4.5));
        assertEquals(2, sets.getRiskSet(2).evaluate(4.5));

        assertEquals(2, sets.getRiskSet(1).evaluate(5.0));
        assertEquals(2, sets.getRiskSet(2).evaluate(5.0));

        assertEquals(1, sets.getRiskSet(1).evaluate(5.5));
        assertEquals(1, sets.getRiskSet(2).evaluate(5.5));

        assertEquals(1, sets.getRiskSet(1).evaluate(6.0));
        assertEquals(1, sets.getRiskSet(2).evaluate(6.0));

        assertEquals(1, sets.getRiskSet(1).evaluate(6.5));
        assertEquals(0, sets.getRiskSet(2).evaluate(6.5));

        assertEquals(0, sets.getRiskSet(1).evaluate(7.0));
        assertEquals(0, sets.getRiskSet(2).evaluate(7.0));

        assertEquals(0, sets.getRiskSet(1).evaluate(7.5));
        assertEquals(0, sets.getRiskSet(2).evaluate(7.5));

    }

}
