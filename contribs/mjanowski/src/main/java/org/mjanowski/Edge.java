package org.mjanowski;

import java.util.Objects;

public class Edge {

    private final int nodeIntId;
    private long weight ;

    public Edge(int nodeIntId) {
        this.nodeIntId = nodeIntId;
        this.weight = 0;
    }

    public int getNodeIntId() {
        return nodeIntId;
    }

    public long getWeight() {
        return weight;
    }

    public void incrementWeight() {
        weight++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return nodeIntId == edge.nodeIntId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeIntId);
    }
}
