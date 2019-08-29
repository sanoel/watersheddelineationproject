package org.waterapps.lib;

public interface WmacListener {
    DemData getDemData();

    DemLoadUtils getDemLoadUtils();

    void onDemDataLoad();

    void setDemLoadUtils(DemLoadUtils demLoadUtils);
}
