package org.mjanowski;

import org.matsim.core.config.ReflectiveConfigGroup;

public class MySimConfig extends ReflectiveConfigGroup {

    public MySimConfig(String name) {
        super("mySimConfig");
    }

    public MySimConfig(String masterAddress, String masterPort, String workerAddress, int workersNumber) {
        super("mySimConfig");
        this.masterAddress = masterAddress;
        this.masterPort = masterPort;
        this.workerAddress = workerAddress;
        this.workersNumber = workersNumber;
    }

    private String masterAddress;
    private String masterPort;
    private String workerAddress;
    private int workersNumber;

    public String getMasterAddress() {
        return masterAddress;
    }

    public void setMasterAddress(String masterAddress) {
        this.masterAddress = masterAddress;
    }

    public String getMasterPort() {
        return masterPort;
    }

    public void setMasterPort(String masterPort) {
        this.masterPort = masterPort;
    }

    public int getWorkersNumber() {
        return workersNumber;
    }

    public void setWorkersNumber(int workersNumber) {
        this.workersNumber = workersNumber;
    }

    public String getWorkerAddress() {
        return workerAddress;
    }

    public void setWorkerAddress(String workerAddress) {
        this.workerAddress = workerAddress;
    }
}
