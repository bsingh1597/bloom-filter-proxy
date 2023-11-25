package edu.dcs.proxyserverdcs.server;

import java.io.Serializable;
import java.util.BitSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageObject implements Serializable{
    private String portNumber;

    @JsonIgnore
    private BitSet bitSet;

    // Default constructor for deserialization
    public MessageObject() {
    }

    public MessageObject(String portNumber, BitSet bitSet) {
        this.portNumber = portNumber;
        this.bitSet = bitSet;
    }

    public String getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(String portNumber) {
        this.portNumber = portNumber;
    }

    public BitSet getBitSet() {
        return bitSet;
    }

    public void setBitSet(BitSet bitSet) {
        this.bitSet = bitSet;
    }
}
