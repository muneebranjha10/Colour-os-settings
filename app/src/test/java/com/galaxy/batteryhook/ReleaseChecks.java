package com.galaxy.batteryhook;
import org.junit.Test;
import static org.junit.Assert.*;
public class ReleaseChecks {
    @Test public void parsesRealAsoc(){HealthReader.Reading r=new HealthReader.Reading();HealthReader.parseDump(r,"fg_asoc=90","test");assertEquals(90,r.main());}
    @Test public void keepsIndependentHealthIndicators(){HealthReader.Reading r=new HealthReader.Reading();HealthReader.parseDump(r,"fg_asoc=90\nmSavedBatteryBsoh: 95","test");assertEquals(90,r.main());assertEquals(95,r.bsoh);}
    @Test public void parsesBracketedSamsungUsage(){HealthReader.Reading r=new HealthReader.Reading();HealthReader.parseDump(r,"mSavedBatteryUsage: [79361]","test");HealthReader.resolveCycles(r);assertEquals(793,r.cycles);}
    @Test public void prefersNonzeroVendorCycle(){HealthReader.Reading r=new HealthReader.Reading();HealthReader.parseDump(r,"battery_cycle: 793\nmSavedBatteryUsage: [80000]","test");HealthReader.resolveCycles(r);assertEquals(793,r.cycles);}
    @Test public void missingAndInvalidValuesStayUnavailable(){HealthReader.Reading r=new HealthReader.Reading();HealthReader.parseDump(r,"fg_asoc: 999\nmSavedBatteryUsage: [-1]\nbattery_cycle: 0","test");HealthReader.resolveCycles(r);assertEquals(-1,r.main());assertEquals(-1,r.cycles);}
    @Test public void wattsDoesNotOverflow(){assertEquals(12.0,ReleaseRules.watts(3000000,4000),.00001);}
    @Test public void rejectsInvalidPowerSamples(){assertTrue(Double.isNaN(ReleaseRules.watts(-1,4000)));assertTrue(Double.isNaN(ReleaseRules.watts(1000000,100)));assertTrue(Double.isNaN(ReleaseRules.watts(Double.NaN,4000)));}
    @Test public void knownProtectionRangeOnly(){for(int n=80;n<=95;n+=5)assertTrue(ReleaseRules.supportsProtection(36,1,n));assertFalse(ReleaseRules.supportsProtection(35,1,80));assertFalse(ReleaseRules.supportsProtection(37,1,100));assertFalse(ReleaseRules.supportsProtection(37,4,90));}
    @Test public void unknownModelsNeverCallObfuscatedHelpers(){assertTrue(ReleaseRules.testedHelperFamily("SM-S911B",37));assertFalse(ReleaseRules.testedHelperFamily("SM-S928B",37));assertFalse(ReleaseRules.testedHelperFamily("SM-S911B",35));}
    @Test public void customSettingsBounds(){assertEquals(0,ReleaseRules.clampOption(-7));assertEquals(2,ReleaseRules.clampOption(100));assertEquals(1,ReleaseRules.clampOption(1));}
    @Test public void morphEndpointsAreExact(){assertEquals(0f,LiquidMotion.leading(0),0);assertEquals(0f,LiquidMotion.trailing(0),0);assertEquals(1f,LiquidMotion.leading(1),0);assertEquals(1f,LiquidMotion.trailing(1),0);}
    @Test public void leadingEdgePullsBeforeTrailingEdge(){for(int i=0;i<=100;i++){float p=i/100f;assertTrue(LiquidMotion.leading(p)>=LiquidMotion.trailing(p));}assertTrue(LiquidMotion.leading(.5f)-LiquidMotion.trailing(.5f)>.2f);}
    @Test public void morphIsMonotonicBothDirections(){float last=0;for(int i=0;i<=100;i++){float a=LiquidMotion.leading(i/100f);assertTrue(a>=last);assertEquals(2-2*a,LiquidMotion.mix(2,0,a),.00001);last=a;}}
    @Test public void neckAndDragAreBounded(){assertEquals(0f,LiquidMotion.neck(0,100,.25f),0);assertTrue(LiquidMotion.neck(3,100,.25f)<=25);assertTrue(LiquidMotion.dragTail(0,1)>0&&LiquidMotion.dragTail(0,1)<1);}
}
