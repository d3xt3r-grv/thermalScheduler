package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Solution {

    public double time;

    public double energy;

    public List<Map<Task,Vm>> solution;

    public Solution(){
        this.solution=new ArrayList<>();
        this.time=Double.MAX_VALUE;
        this.energy=Double.MAX_VALUE;
    }
}
