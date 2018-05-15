
package org.schemaspy.model;

import org.schemaspy.*;
import org.schemaspy.model.SingleChange;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.*;

public class GenericTree {

    private GenericTreeNode root;

    public GenericTree() {
        super();
    }

    public GenericTreeNode getRoot() {
        return this.root;
    }

    public void setRoot(GenericTreeNode root) {
        this.root = root;
    }

    public int getNumberOfNodes() {
        int numberOfNodes = 0;

        if(root != null) {
            numberOfNodes = auxiliaryGetNumberOfNodes(root) + 1; //1 for the root!
        }

        return numberOfNodes;
    }


    private int auxiliaryGetNumberOfNodes(GenericTreeNode node) {
        int numberOfNodes = node.getNumberOfChildren();

        for(GenericTreeNode child : node.getChildren()) {
            numberOfNodes += auxiliaryGetNumberOfNodes(child);
        }

        return numberOfNodes;
    }

    public GenericTreeNode find(Integer id) {
        if(root == null)
        {
          return null;
        }
        return auxiliaryFind(root, id);
    }

    private GenericTreeNode auxiliaryFind(GenericTreeNode currentNode, Integer id) {
        if (currentNode.getId().equals(id))
            return currentNode;
        if (! currentNode.hasChildren())
            return null;
        for (GenericTreeNode child : currentNode.getChildren())
        {
          GenericTreeNode returnNode = auxiliaryFind(child, id);
          if (null != returnNode)
             return returnNode;
        }
        System.out.println("null ici ");
        return null;
    }

    public boolean isEmpty() {
        return (root == null);
    }

    public List<GenericTreeNode> build(GenericTreeTraversalOrderEnum traversalOrder) {
        List<GenericTreeNode> returnList = null;

        if(root != null) {
            returnList = build(root, traversalOrder);
        }

        return returnList;
    }

    public List<GenericTreeNode> build(GenericTreeNode node, GenericTreeTraversalOrderEnum traversalOrder) {
        List<GenericTreeNode> traversalResult = new ArrayList<GenericTreeNode>();

        if(traversalOrder == GenericTreeTraversalOrderEnum.PRE_ORDER) {
            buildPreOrder(node, traversalResult);
        }

        else if(traversalOrder == GenericTreeTraversalOrderEnum.POST_ORDER) {
            buildPostOrder(node, traversalResult);
        }

        return traversalResult;
    }

    private void buildPreOrder(GenericTreeNode node, List<GenericTreeNode> traversalResult) {
        traversalResult.add(node);

        for(GenericTreeNode child : node.getChildren()) {
            buildPreOrder(child, traversalResult);
        }
    }

    private void buildPostOrder(GenericTreeNode node, List<GenericTreeNode> traversalResult) {
        for(GenericTreeNode child : node.getChildren()) {
            buildPostOrder(child, traversalResult);
        }

        traversalResult.add(node);
    }

    public Map<GenericTreeNode, Integer> buildWithDepth(GenericTreeTraversalOrderEnum traversalOrder) {
        Map<GenericTreeNode, Integer> returnMap = null;

        if(root != null) {
            returnMap = buildWithDepth(root, traversalOrder);
        }

        return returnMap;
    }

    public Map<GenericTreeNode, Integer> buildWithDepth(GenericTreeNode node, GenericTreeTraversalOrderEnum traversalOrder) {
        Map<GenericTreeNode, Integer> traversalResult = new LinkedHashMap<GenericTreeNode, Integer>();

        if(traversalOrder == GenericTreeTraversalOrderEnum.PRE_ORDER) {
            buildPreOrderWithDepth(node, traversalResult, 0);
        }

        else if(traversalOrder == GenericTreeTraversalOrderEnum.POST_ORDER) {
            buildPostOrderWithDepth(node, traversalResult, 0);
        }

        return traversalResult;
    }

    private void buildPreOrderWithDepth(GenericTreeNode node, Map<GenericTreeNode, Integer> traversalResult, int depth) {
        traversalResult.put(node, depth);

        for(GenericTreeNode child : node.getChildren()) {
            buildPreOrderWithDepth(child, traversalResult, depth + 1);
        }
    }

    private void buildPostOrderWithDepth(GenericTreeNode node, Map<GenericTreeNode, Integer> traversalResult, int depth) {
        for(GenericTreeNode child : node.getChildren()) {
            buildPostOrderWithDepth(child, traversalResult, depth + 1);
        }

        traversalResult.put(node, depth);
    }

    public String toString() {
        /*
        We're going to assume a pre-order traversal by default
         */

        String stringRepresentation = "";

        if(root != null) {
            stringRepresentation = build(GenericTreeTraversalOrderEnum.PRE_ORDER).toString();

        }

        return stringRepresentation;
    }

    public String toStringWithDepth() {
        /*
        We're going to assume a pre-order traversal by default
         */

        String stringRepresentation = "";

        if(root != null) {
            stringRepresentation = buildWithDepth(GenericTreeTraversalOrderEnum.PRE_ORDER).toString();
        }

        return stringRepresentation;
    }

    public Integer getLastId()
    {
      return this.getNumberOfNodes();
    }

    public GenericTreeNode getLastMutation()
    {
      return find(getLastId());
    }

    public void addToTree(GenericTreeNode currentMutation)
    {
      System.out.println(currentMutation);
      currentMutation.setParent(findFirstMutationWithout(root,currentMutation.getChosenChange()));
      currentMutation.getChosenChange().setAttachedToMutation(currentMutation);
      currentMutation.getParent().addChild(currentMutation);
    }

    public GenericTreeNode findFirstMutationWithout(GenericTreeNode mutation, SingleChange chosenChange)
    {
        int i,j;
        boolean noSonHasChosenChange = true;
        GenericTreeNode res = null;

        if(mutation.getChildren().isEmpty())
        {
          return mutation;
        }

        for(i = 0; i < mutation.getChildren().size(); i++)
        {
          if(mutation.getChildren().get(i).getChosenChange().compare(chosenChange))
            noSonHasChosenChange = false;
        }

        if(noSonHasChosenChange)
          return mutation;

        for(j = 0; j < mutation.getChildren().size(); j++)
        {
          res = findFirstMutationWithout(mutation.getChildren().get(j),chosenChange);
        }
        return res; // should never be null unless the algorithm is not looking for something precise
    }

    public ArrayList<GenericTreeNode> mutationsBasedOnWeight()
    {
      ArrayList<GenericTreeNode> mutationsBasedOnWeight = new ArrayList<GenericTreeNode>();
      GenericTreeNode currentMutation;

      System.out.println("nb nodes "+getNumberOfNodes());
      for(int i = 1; i <= getNumberOfNodes();i++)
      {
        currentMutation = find(i);
        System.out.println("ICI mutation N "+currentMutation.getId());
        for(int j = 0; j < currentMutation.getWeight();j++)
        {
          System.out.println("COOUCOU");
          System.out.println("added once "+currentMutation.getId());
          mutationsBasedOnWeight.add(find(i));
        }
      }
      System.out.println("Weight "+mutationsBasedOnWeight);
      return mutationsBasedOnWeight;
    }

    public GenericTreeNode pickMutationBasedOnWeight(ArrayList<GenericTreeNode> mutationsBasedOnWeight)
    {
      int randNumber = 0;
      Random rand = new Random();
      if(mutationsBasedOnWeight.size() > 0)
        randNumber = rand.nextInt(mutationsBasedOnWeight.size());

      return mutationsBasedOnWeight.get(randNumber);
    }
}
