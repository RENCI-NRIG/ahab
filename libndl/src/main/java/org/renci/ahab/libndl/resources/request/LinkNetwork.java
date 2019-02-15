package org.renci.ahab.libndl.resources.request;

import org.renci.ahab.libndl.SliceGraph;

public class LinkNetwork extends Network {
    protected String label = null;
    protected long bandwidth;

    public LinkNetwork(SliceGraph sliceGraph, String name) {
        super(sliceGraph, name);
    }

    public void setLabel(String l) {
        if ((l != null) && l.length() > 0)
            label = l;
        else
            label = null;
    }

    public String getLabel() {
        return label;
    }

    public void setBandwidth(long b) {
        this.getNDLModel().setBandwidth(this, b);
    }

    public Long getBandwidth() {
        return this.getNDLModel().getBandwidth(this);
    }

    public Interface stitch(RequestResource r){
        Interface stitch = null;
        if (r instanceof Node){
            stitch = ((Node)r).stitch(this);
        } else {
            System.out.println("Error: Cannot stitch link to " + r.getClass().getName());
            return null;
        }
        return stitch;
    }

    public Interface stitch(RequestResource r, RequestResource depend){
        Interface stitch = null;
        if (r instanceof ComputeNode){
            stitch = ((ComputeNode)r).stitch(this, depend);
        }
        else if (r instanceof Node){
            stitch = ((Node)r).stitch(this);
        } else {
            System.out.println("Error: Cannot stitch link to " + r.getClass().getName());
            return null;
        }
        return stitch;
    }

    @Override
    public String getPrintText() {
        return null;
    }

    @Override
    public void delete() {
        sliceGraph.deleteResource(this);
    }
}
