package com.crm.service.scoring;

import com.crm.dto.scoring.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LeadScoringEngineTest {
    private final LeadScoringEngine engine=new LeadScoringEngine();
    private ScoringRule rule(String id,String group,String field,String op,String value,int points,boolean active) {
        return new ScoringRule(id,group,id,field,op,value,points,"",active);
    }
    private ScoringConfig config(String aggregation,Integer floor,Integer cap,ScoringRule...rules) {
        return new ScoringConfig(1,70,40,aggregation,floor,cap,List.of(rules));
    }
    @Test void matchingIsCaseInsensitiveAndNumericBoundariesAreInclusive() {
        var config=config("SUM_ALL",0,null,rule("a","INDUSTRY","industry","EQUALS"," CNTT ",20,true),
                rule("b","COMPANY_SIZE","employeeCount","GREATER_THAN","100",20,true),
                rule("c","COMPANY_SIZE","employeeCount","LESS_THAN","100",10,true),
                rule("d","SOURCE","source","CONTAINS","website",20,true));
        var result=engine.calculate(Map.of("industry","cntt","employeeCount",100,"source","WEBSITE Form"),config);
        assertEquals(70,result.score());assertEquals("HOT",result.classification());assertEquals(4,result.breakdown().size());
        assertEquals(70,engine.calculate(Map.of("industry","cntt","employeeCount",100,"source","WEBSITE Form"),config).score());
    }
    @Test void groupMaximumPreventsStackingAndTieUsesFirstRule() {
        var config=config("MAX_PER_GROUP",null,null,rule("a","COMPANY_SIZE","employeeCount","GREATER_THAN","20",10,true),
                rule("b","COMPANY_SIZE","employeeCount","GREATER_THAN","100",20,true),
                rule("c","COMPANY_SIZE","employeeCount","GREATER_THAN","100",20,true));
        var result=engine.calculate(Map.of("employeeCount",180),config);
        assertEquals(20,result.score());assertEquals("b",result.breakdown().getFirst().ruleId());
    }
    @Test void missingNumericInputIsNotCoercedToZeroAndInactiveRulesNeverMatch() {
        var config=config("SUM_ALL",0,null,rule("a","COMPANY_SIZE","employeeCount","LESS_THAN","20",5,true),
                rule("b","SOURCE","source","CONTAINS","Website",90,false));
        assertEquals(0,engine.calculate(Map.of("source","Website"),config).score());
        assertEquals(0,engine.calculate(Map.of("employeeCount","unknown"),config).score());
        assertEquals(5,engine.calculate(Map.of("employeeCount",0),config).score());
    }
    @Test void negativePointsFloorAndCapAreExplicitAndRawScoreExplainsAdjustment() {
        var minus=rule("a","INTENT","recentActivity","CONTAINS","không phản hồi",-15,true);
        assertEquals(-15,engine.calculate(Map.of("recentActivity","Không phản hồi"),config("SUM_ALL",null,null,minus)).score());
        var floor=engine.calculate(Map.of("recentActivity","Không phản hồi"),config("SUM_ALL",0,null,minus));
        assertEquals(0,floor.score());assertEquals(-15,floor.rawScore());
        var plus=rule("b","SOURCE","source","EQUALS","Website",150,true);
        assertEquals(150,engine.calculate(Map.of("source","Website"),config("SUM_ALL",0,null,plus)).score());
        assertEquals(100,engine.calculate(Map.of("source","Website"),config("SUM_ALL",0,100,plus)).score());
    }
    @Test void warmAndHotThresholdsHaveNoOffByOne() {
        for(int points:List.of(39,40,69,70)) {
            var result=engine.calculate(Map.of("source","x"),config("SUM_ALL",0,null,rule("a","SOURCE","source","EQUALS","x",points,true)));
            assertEquals(points>=70?"HOT":points>=40?"WARM":"COLD",result.classification());
        }
    }
    @Test void unconfirmedOrInvalidBusinessPolicyCannotProduceAScore() {
        assertThrows(IllegalStateException.class,()->engine.calculate(Map.of(),new ScoringConfig(0,null,null,null,null,null,List.of())));
        assertThrows(IllegalArgumentException.class,()->engine.validate(new ScoringConfig(0,40,40,"SUM_ALL",0,null,List.of())));
        assertThrows(IllegalArgumentException.class,()->engine.validate(config("UNKNOWN",0,null)));
        assertThrows(IllegalArgumentException.class,()->engine.validate(config("SUM_ALL",0,null,rule("a","SOURCE","ownerUserId","EQUALS","x",5,true))));
        assertThrows(IllegalArgumentException.class,()->engine.validate(config("SUM_ALL",0,null,rule("a","SOURCE","source","GREATER_THAN","5",5,true))));
        assertThrows(IllegalArgumentException.class,()->engine.validate(config("SUM_ALL",0,null,rule("a","COMPANY_SIZE","employeeCount","LESS_THAN","bad",5,true))));
        var rule=rule("a","SOURCE","source","EQUALS","x",5,true);
        assertThrows(IllegalArgumentException.class,()->engine.validate(config("SUM_ALL",0,null,rule,rule)));
    }
}
