package org.schemaspy.model;

import nl.jqno.equalsverifier.internal.exceptions.AssertionException;
import org.junit.*;

import java.util.ArrayList;
import java.util.Random;

public class GenericTreeNodeTest {



    @Test
    public void WeightPropagationTest() throws AssertionException
    {
        Random rand = new Random();

        GenericTreeNode gtn1 = new GenericTreeNode(null,1,null,null);
        gtn1.setWeight(rand.nextInt(Integer.MAX_VALUE));
        GenericTreeNode gtn2 = new GenericTreeNode(null,2,null,gtn1);
        gtn2.setWeight(rand.nextInt(Integer.MAX_VALUE));
        GenericTreeNode gtn3 = new GenericTreeNode(null,3,null,gtn1);
        gtn3.setWeight(Integer.MAX_VALUE);

        gtn1.addChild(gtn2);
        gtn1.addChild(gtn3);

        gtn2.propagateWeight();
        gtn3.propagateWeight();

        Assert.assertEquals("Testing Weight propagation ",gtn1.getSubTreeWeight(),gtn2.getWeight()+gtn3.getWeight());

        gtn2.setWeight(10);
        gtn3.setWeight(10);

        gtn2.propagateWeight();
        gtn3.propagateWeight();

        gtn1.setSubTreeWeight(0);

        Assert.assertFalse(gtn1.getSubTreeWeight() == gtn2.getWeight()+gtn3.getWeight());

    }

    @Test
    public void SingleChangeBasedOnWeightShouldNotReturnNull() throws AssertionException
    {
        GenericTreeNode gtn1 = new GenericTreeNode(null,1,null,null);
        gtn1.setPotential_changes(new ArrayList<>());

        GenericTreeNode gtn2= new GenericTreeNode(null,2,null,gtn1);
        gtn2.setPotential_changes(new ArrayList<>());
        gtn2.setWeight(10);

        GenericTreeNode gtn3 = new GenericTreeNode(null,3,null,gtn1);
        gtn3.setPotential_changes(new ArrayList<>());
        gtn3.setWeight(10);

        SingleChange sg1 = new SingleChange(null,null,"1","2");
        SingleChange sg2 = new SingleChange(null,null,"1","3");
        SingleChange sg3 = new SingleChange(null,null,"hello","hella");
        SingleChange sg4 = new SingleChange(null,null,"f","t");

        gtn1.getPotential_changes().add(sg1);
        gtn2.getPotential_changes().add(sg2);
        gtn3.getPotential_changes().add(sg3);
        gtn3.getPotential_changes().add(sg4);

        Assert.assertNotNull(gtn1.singleChangeBasedOnWeight());
    }

}
