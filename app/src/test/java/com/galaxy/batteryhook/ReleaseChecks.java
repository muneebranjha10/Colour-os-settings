package com.galaxy.batteryhook;
import org.junit.Test;
import static org.junit.Assert.*;
public class ReleaseChecks {
    @Test public void parsesRealAsoc(){HealthReader.Reading r=new HealthReader.Reading();HealthReader.parseDump(r,"fg_asoc=90","test");assertEquals(90,r.main());}
    @Test public void keepsIndependentHealthIndicators(){HealthReader.Reading r=new HealthReader.Reading();HealthReader.parseDump(r,"fg_asoc=90\nmSavedBatteryBsoh: 95","test");assertEquals(90,r.main());assertEquals(95,r.bsoh);}
    @Test public void parsesBracketedSamsungUsage(){HealthReader.Reading r=new HealthReader.Reading();HealthReader.parseDump(r,"mSavedBatteryUsage: [79361]","test");HealthReader.resolveCycles(r);assertEquals(793,r.cycles);}
    @Test public void prefersNonzeroVendorCycle(){HealthReader.Reading r=new HealthReader.Reading();HealthReader.parseDump(r,"battery_cycle: 793\nmSavedBatteryUsage: [80000]","test");HealthReader.resolveCycles(r);assertEquals(793,r.cycles);}
    @Test public void missingAndInvalidValuesStayUnavailable(){HealthReader.Reading r=new HealthReader.Reading();HealthReader.parseDump(r,"fg_asoc: 999\nmSavedBatteryUsage: [-1]\nbattery_cycle: 0","test");HealthReader.resolveCycles(r);assertEquals(-1,r.main());assertEquals(-1,r.cycles);}
    @Test public void unverifiedTinyOrNonfiniteWattsAreNotPublished(){
        assertFalse(ReleaseRules.publishPower(.01,true,false,false));
        assertFalse(ReleaseRules.publishPower(.3,true,false,false));
        assertFalse(ReleaseRules.publishPower(5,true,false,true));
        assertFalse(ReleaseRules.publishPower(Double.NaN,true,false,false));
        assertFalse(ReleaseRules.publishPower(100,true,false,false));
        assertTrue(ReleaseRules.publishPower(3.5,true,false,false));
    }
    @Test public void fullBatteryCanReallyReportZeroWatts(){
        assertTrue(ReleaseRules.publishPower(0,false,true,false));
        assertFalse(ReleaseRules.publishPower(0,true,false,false));
    }
    @Test public void wattsDoesNotOverflow(){assertEquals(12.0,ReleaseRules.watts(3000000,4000),.00001);}
    @Test public void rejectsInvalidPowerSamples(){assertTrue(Double.isNaN(ReleaseRules.watts(-1,4000)));assertTrue(Double.isNaN(ReleaseRules.watts(1000000,100)));assertTrue(Double.isNaN(ReleaseRules.watts(Double.NaN,4000)));}
    @Test public void knownProtectionRangeOnly(){for(int n=80;n<=95;n+=5)assertTrue(ReleaseRules.supportsProtection(36,1,n));assertFalse(ReleaseRules.supportsProtection(35,1,80));assertFalse(ReleaseRules.supportsProtection(37,1,100));assertFalse(ReleaseRules.supportsProtection(37,4,90));}
    @Test public void directProfileSwitchUsesTwoFirmwareValues(){
        assertEquals(1,ReleaseRules.samsungProfileForLight());
        assertEquals(0,ReleaseRules.samsungProfileForStandard());
    }
    @Test public void standardIsNotOverclocking(){
        assertEquals(0,ReleaseRules.samsungProfileForStandard());
        assertEquals(1,ReleaseRules.samsungProfileForLight());
        assertNotEquals(ReleaseRules.samsungProfileForStandard(),ReleaseRules.samsungProfileForLight());
    }
    @Test public void samsungProfileProbeRequiresRealFirmwareSignals(){
        assertTrue(ReleaseRules.canProbeLightProfile("samsung","com.samsung.android.lool",37,1));
        assertTrue(ReleaseRules.canProbeLightProfile("SAMSUNG","com.samsung.android.lool",35,0));
        assertFalse(ReleaseRules.canProbeLightProfile("google","com.samsung.android.lool",37,1));
        assertFalse(ReleaseRules.canProbeLightProfile("samsung","com.android.settings",37,1));
        assertFalse(ReleaseRules.canProbeLightProfile("samsung","com.samsung.android.lool",37,-1));
        assertFalse(ReleaseRules.canProbeLightProfile("samsung","com.samsung.android.lool",38,1));
    }
    @Test public void saverHasPriorityOverChargingColor(){
        assertEquals(0xffffae13,ReleaseRules.batteryBarColor(false,true));
        assertEquals(0xffffae13,ReleaseRules.batteryBarColor(true,true));
        assertEquals(0xff198bfe,ReleaseRules.batteryBarColor(true,false));
        assertEquals(0xff22ce37,ReleaseRules.batteryBarColor(false,false));
    }
    @Test public void immediateProfileAnimationOnlyForRealTwoProfiles(){
        assertEquals(0,ReleaseRules.profileSlotForMode(0));
        assertEquals(1,ReleaseRules.profileSlotForMode(2));
        assertEquals(-1,ReleaseRules.profileSlotForMode(1));
        assertEquals(-1,ReleaseRules.profileSlotForMode(-1));
    }
    @Test public void nativeProfileToTileMapping(){
        assertEquals(0,ReleaseRules.modeForSamsungProfile(1));
        assertEquals(2,ReleaseRules.modeForSamsungProfile(0));
        assertEquals(-1,ReleaseRules.modeForSamsungProfile(-1));
        assertEquals(-1,ReleaseRules.modeForSamsungProfile(3));
    }
    @Test public void customSettingsBounds(){assertEquals(0,ReleaseRules.clampOption(-7));assertEquals(2,ReleaseRules.clampOption(100));assertEquals(1,ReleaseRules.clampOption(1));}
    @Test public void morphEndpointsAreExact(){assertEquals(0f,LiquidMotion.leading(0),0);assertEquals(0f,LiquidMotion.trailing(0),0);assertEquals(1f,LiquidMotion.leading(1),0);assertEquals(1f,LiquidMotion.trailing(1),0);}
    @Test public void leadingEdgePullsBeforeTrailingEdge(){for(int i=0;i<=100;i++){float p=i/100f;assertTrue(LiquidMotion.leading(p)>=LiquidMotion.trailing(p));}assertTrue(LiquidMotion.leading(.5f)-LiquidMotion.trailing(.5f)>.2f);}
    @Test public void morphIsMonotonicBothDirections(){float last=0;for(int i=0;i<=100;i++){float a=LiquidMotion.leading(i/100f);assertTrue(a>=last);assertEquals(2-2*a,LiquidMotion.mix(2,0,a),.00001);last=a;}}
    @Test public void neckAndDragAreBounded(){assertEquals(0f,LiquidMotion.neck(0,100,.25f),0);assertTrue(LiquidMotion.neck(3,100,.25f)<=25);assertTrue(LiquidMotion.dragTail(0,1)>0&&LiquidMotion.dragTail(0,1)<1);}
    @Test public void reducedMotionCapsDuration(){assertEquals(120,PageMotion.time(340,true));assertEquals(340,PageMotion.time(340,false));assertEquals(1,PageMotion.time(0,true));}
    @Test public void pageParallaxRestoresOriginalPosition(){assertEquals(0f,PageMotion.parallax(1080,true),0f);assertEquals(-37.8f,PageMotion.parallax(1080,false),.0001f);assertEquals(0f,PageMotion.parallax(0,false),0f);}
    @Test public void chargeLimitOnlySnapsToFourNativeValues(){assertEquals(80,GlassMath.limitForProgress(-1));assertEquals(80,GlassMath.limitForProgress(49));assertEquals(85,GlassMath.limitForProgress(50));assertEquals(90,GlassMath.limitForProgress(150));assertEquals(95,GlassMath.limitForProgress(1000));}
    @Test public void chargeLimitProgressRoundTrips(){for(int n=80;n<=95;n+=5)assertEquals(n,GlassMath.limitForProgress(GlassMath.progressForLimit(n)));}
    @Test public void sliderMirrorsRtl(){assertEquals(0f,GlassMath.thumbFraction(0,false),0);assertEquals(1f,GlassMath.thumbFraction(0,true),0);assertEquals(.5f,GlassMath.thumbFraction(150,true),0);assertEquals(0f,GlassMath.thumbFraction(300,true),0);}
    @Test public void springHasExactEndpointsAndBoundedLeadingEdge(){assertEquals(0f,GlassMath.railHead(0),0);assertEquals(1f,GlassMath.railHead(1),0);assertEquals(1f,GlassMath.railTail(1),0);for(int i=0;i<=100;i++)assertTrue(GlassMath.railHead(i/100f)<=1.035f);}
    @Test public void silverRemainsNeutralAndClassicIsRestrained(){for(int c:ColorOsPalette.body(0))assertTrue(ColorOsPalette.chroma(c)<=6);for(int c:ColorOsPalette.body(1))assertTrue(ColorOsPalette.chroma(c)<=40);assertTrue(ColorOsPalette.chroma(ColorOsPalette.body(2)[0])>40);assertEquals(0xff5aa866,ColorOsPalette.body(2)[0]);}
    @Test public void powerTintBlendHasCorrectEndpoints(){
        assertEquals(ColorOsPalette.ACCENT,ColorOsPalette.mix(ColorOsPalette.ACCENT,0xffffcc35,0f));
        assertEquals(0xffffcc35,ColorOsPalette.mix(ColorOsPalette.ACCENT,0xffffcc35,1f));
        assertEquals(0xffffcc35,ColorOsPalette.mix(ColorOsPalette.ACCENT,0xffffcc35,10f));
    }
    @Test public void accentRemainsSeparateFromNeutralGlass(){assertTrue(ColorOsPalette.chroma(ColorOsPalette.ACCENT)>80);}
    @Test public void healthExitIsShortAndBounded(){assertEquals(219,PageMotion.healthExit(230,false));assertEquals(100,PageMotion.healthExit(230,true));assertEquals(480,PageMotion.healthExit(1000,false));assertEquals(140,PageMotion.healthExit(20,false));}
    @Test public void colorOsMotionPresetAvoidsUnnecessarySlowdown(){assertEquals(360,PageMotion.duration(360,1,false));assertEquals(461,PageMotion.duration(360,2,false));assertEquals(274,PageMotion.duration(360,0,false));assertEquals(180,PageMotion.duration(360,1,true));}
    @Test public void glassRespondsImmediatelyAndSettles(){assertTrue(GlassMath.railHead(.1f)>.2f);float old=0;for(int i=0;i<=100;i++){float t=i/100f;assertTrue(GlassMath.railHead(t)>=old);assertTrue(GlassMath.railHead(t)>=GlassMath.railTail(t)-.00001f);old=GlassMath.railHead(t);}assertEquals(1f,GlassMath.railTail(1),0f);}
    @Test public void bracketedHealthFieldsStayDistinct(){HealthReader.Reading r=new HealthReader.Reading();HealthReader.parseDump(r,"mSavedBatteryAsoc: [91]\nmSavedBatteryBsoh: [95]","service");assertEquals(-1,r.asoc);assertEquals(91,r.savedAsoc);assertEquals(95,r.bsoh);r.api=93;assertEquals(93,r.main());}
    @Test public void liveSensorWinsWithoutInventingValue(){HealthReader.Reading r=new HealthReader.Reading();r.offerCycles(793,100,"sysfs");HealthReader.parseDump(r,"cycle_count: 800\nmSavedBatteryUsage: [90000]","service");HealthReader.resolveCycles(r);assertEquals(793,r.cycles);assertEquals("sysfs",r.cycleSource);}
    @Test public void newerReadCanReflectRealIncrement(){HealthReader.Reading a=new HealthReader.Reading(),b=new HealthReader.Reading();HealthReader.parseDump(a,"battery_cycle: 793","service");HealthReader.parseDump(b,"battery_cycle: 794","service");assertEquals(793,a.cycles);assertEquals(794,b.cycles);}
    @Test public void zeroAndAmbiguousArraysNotInvented(){HealthReader.Reading r=new HealthReader.Reading();HealthReader.parseDump(r,"mSavedBatteryAsoc: [90, 93]\ncycle_count: 0\nmSavedBatteryUsage: [0]","service");HealthReader.resolveCycles(r);assertEquals(-1,r.main());assertEquals(-1,r.cycles);}
    @Test public void standardCycleFieldIsAValidFallback(){HealthReader.Reading r=new HealthReader.Reading();HealthReader.parseDump(r,"cycle_count: [201]","service");assertEquals(201,r.cycles);}
    @Test public void glassLabelsFollowVisualPosition(){assertEquals(1f,GlassMath.coverage(1,1),0f);assertEquals(0f,GlassMath.coverage(-1,0),0f);assertEquals(0f,GlassMath.coverage(1,0),0f);assertEquals(.5f,GlassMath.coverage(.5f,0),0f);assertEquals(.5f,GlassMath.coverage(.5f,1),0f);}
    @Test public void labelColorTransferIsContinuous(){float prev=0;for(int i=0;i<=100;i++){float v=GlassMath.coverage(i/100f,1);assertTrue(v>=prev);assertTrue(v-prev<.02f);assertEquals(1f,v+GlassMath.coverage(i/100f,0),.00001f);prev=v;}}
    @Test public void elasticDragTailFollowsWithoutLeading(){float prior=0;for(int i=0;i<30;i++){float next=GlassMath.dragFollower(prior,1,16);assertTrue(next>=prior&&next<=1);prior=next;}assertTrue(prior>.999f);assertTrue(GlassMath.dragFollower(1,0,16)>=0);}
    @Test public void dragDeformationIsBounded(){assertEquals(1-GlassMath.followFraction(1),GlassMath.dragFollower(1,0,1),.00001f);assertEquals(GlassMath.followFraction(1),GlassMath.dragFollower(0,1,1),.00001f);assertEquals(0f,GlassMath.neckFraction(0),0);assertEquals(.10f,GlassMath.neckFraction(4),.00001f);}
    @Test public void elasticPulseReturnsToRest(){assertEquals(0f,GlassMath.elasticPulse(0),0);assertEquals(0f,GlassMath.elasticPulse(1),0);assertTrue(GlassMath.elasticPulse(.25f)>0);assertTrue(GlassMath.elasticPulse(.75f)<0);}
    @Test public void gestureResponseBeatsSlowOriginalGlass(){
        assertTrue(GlassMath.railHead(.15f)>.35f);
        assertTrue(GlassMath.railTail(.15f)>0);
        assertEquals(1f,GlassMath.railHead(1),0);
        assertEquals(1f,GlassMath.railTail(1),0);
        float lastHead=0,lastTail=0;
        for(int i=0;i<=100;i++){
            float t=i/100f;
            float head=GlassMath.railHead(t),tail=GlassMath.railTail(t);
            assertTrue(head>=tail);
            assertTrue(head>=lastHead);
            assertTrue(tail>=lastTail);
            lastHead=head;lastTail=tail;
        }
    }
    @Test public void followerUsesElapsedTime(){assertEquals(GlassMath.followFraction(32),1-(1-GlassMath.followFraction(16))*(1-GlassMath.followFraction(16)),.00001f);}
}
