
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

        if (root != null) {
            numberOfNodes = auxiliaryGetNumberOfNodes(root) + 1; //1 for the root!
        }

        return numberOfNodes;
    }


    private int auxiliaryGetNumberOfNodes(GenericTreeNode node) {
        int numberOfNodes = node.getNumberOfChildren();

        for (GenericTreeNode child : node.getChildren()) {
            numberOfNodes += auxiliaryGetNumberOfNodes(child);
        }

        return numberOfNodes;
    }

    //finds a node in the tree recursivly. used as testing and code ease purposes. should not be sued in loop to much.
    public GenericTreeNode find(Integer id) {
        if (root == null) {
            return null;
        }
        return auxiliaryFind(root, id);
    }

    private GenericTreeNode auxiliaryFind(GenericTreeNode currentNode, Integer id) {
        if (currentNode.getId().equals(id))
            return currentNode;
        if (!currentNode.hasChildren())
            return null;
        for (GenericTreeNode child : currentNode.getChildren()) {
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

    public void addToTree(GenericTreeNode currentMutation) {
        currentMutation.getParent().addChild(currentMutation);
    }

    //finds first mutation that hasnt explored the singleChange. not to be used any more as the picking patern.
    public GenericTreeNode findFirstMutationWithout(GenericTreeNode mutation, SingleChange chosenChange) {
        int i, j;
        boolean noSonHasChosenChange = true;
        GenericTreeNode res = null;

        if (mutation.getChildren().isEmpty()) {
            return mutation;
        }

        for (i = 0; i < mutation.getChildren().size(); i++) {
            if (mutation.getChildren().get(i).getChosenChange().compare(chosenChange))
                noSonHasChosenChange = false;
        }

        if (noSonHasChosenChange)
            return mutation;

        for (j = 0; j < mutation.getChildren().size(); j++) {
            res = findFirstMutationWithout(mutation.getChildren().get(j), chosenChange);
        }
        return res; // should never be null unless the algorithm is not looking for something precise
    }
}
