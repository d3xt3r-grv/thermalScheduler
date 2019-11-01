package models;

import java.util.ArrayList;
import java.util.List;

public class Vm  {

    public int id;

    public double coefficient;

    public double bw;

    public double minMips;

    public double maxMips;

    public List<Event> timeline;

    public Vm(int id, double coeff, double bw, double maxMips, double minMips){
        this.id=id;
        this.coefficient=coeff;
        this.bw=bw;
        this.minMips=minMips;
        this.maxMips=maxMips;
        this.timeline= new ArrayList<>();
    }

}
