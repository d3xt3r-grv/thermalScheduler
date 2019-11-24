package models;

public class Vm  {

    public int id;

    public int host;

    public double coefficient;

    public double minMips;

    public double maxMips;


    public Vm(int id, double coeff, int host, double maxMips, double minMips){
        this.id=id;
        this.coefficient=coeff;
        this.host=host;
        this.minMips=minMips;
        this.maxMips=maxMips;
    }

}
