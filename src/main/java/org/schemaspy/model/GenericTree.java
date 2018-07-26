
package org.schemaspy.model;

import java.util.ArrayList;

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

        if (root != null) {
            numberOfNodes = auxiliaryGetNumberOfNodes(root) + 1; //1 for the root!
        }

        return numberOfNodes;
    }


    private int auxiliaryGetNumberOfNodes(GenericTreeNode node)
    {
        int numberOfNodes = node.getNumberOfChildren();

        for (GenericTreeNode child : node.getChildren())
        {
            numberOfNodes += auxiliaryGetNumberOfNodes(child);
        }

        return numberOfNodes;
    }

    //finds a node in the tree recursivly. used as testing and code ease purposes. should not be sued in loop to much.
    public GenericTreeNode find(Integer id)
    {
        if (root == null)
           return null;

        return auxiliaryFind(root, id);
    }

    private GenericTreeNode auxiliaryFind(GenericTreeNode currentNode, Integer id)
    {
        if (currentNode.getId().equals(id))
            return currentNode;
        if (!currentNode.hasChildren())
            return null;
        for (GenericTreeNode child : currentNode.getChildren())
        {
            GenericTreeNode returnNode = auxiliaryFind(child, id);
            if (null != returnNode)
                return returnNode;
        }
        return null;
    }

    public boolean isEmpty() {
        return (root == null);
    }

    public Integer getLastId() {
        return this.getNumberOfNodes();
    }

    public GenericTreeNode getLastMutation() {
        return find(getLastId());
    }

    public void addToTree(GenericTreeNode currentMutation)
    {
        currentMutation.getParent().addChild(currentMutation);
    }

    //finds first mutation that hasnt explored the singleChange. not to be used any more as the picking patern.
    public GenericTreeNode findFirstMutationWithout(GenericTreeNode mutation, SingleChange chosenChange)
    {
        boolean noSonHasChosenChange = true;
        GenericTreeNode res = null;

        if (mutation.getChildren().isEmpty())
            return mutation;

        for(GenericTreeNode child : mutation.getChildren())
        {
            if (child.getChosenChange().compare(chosenChange))
                noSonHasChosenChange = false;
        }
        if (noSonHasChosenChange)
            return mutation;

        for(GenericTreeNode child : mutation.getChildren())
        {
            res = findFirstMutationWithout(child, chosenChange);
        }
        return res; // should never be null unless the algorithm is not looking for something precise
    }

    public int checkMaxDepth(GenericTreeNode root)
    {
        int res = 0;

        if(root.getChildren().isEmpty())
            return root.getDepth();

        for(GenericTreeNode child :root.getChildren())
        {
            int tmp = checkMaxDepth(child);
            if(tmp > res)
                res = tmp;
        }

        return res;
    }

    public ArrayList<GenericTreeNode> toArray() {
        ArrayList<GenericTreeNode> result = new ArrayList<>();
        toArrayHelp(root, result);
        return result;
    }

    private void toArrayHelp(GenericTreeNode ref, ArrayList<GenericTreeNode> result) {
        if (ref == null) {
            return;
        }
        result.add(ref);
        for(GenericTreeNode gtn : ref.getChildren())
            toArrayHelp(gtn, result);
    }


}
