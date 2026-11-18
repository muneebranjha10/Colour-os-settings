package com.galaxy.batteryhook;

final class BetaConsent {
    interface Store { boolean acknowledged(); boolean save(); }
    private final Store store;
    BetaConsent(Store store){this.store=store;}
    boolean needsNotice(){try{return !store.acknowledged();}catch(RuntimeException e){return true;}}
    boolean accept(){try{return store.save();}catch(RuntimeException e){return false;}}
}
