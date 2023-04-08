import javafx.event.EventHandler;

import javafx.geometry.NodeOrientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javafx.scene.layout.Region;

/**
 * Created by Richard and Ricardo on 5/3/17.
 * Edited by Dov and AJ primarily, with small additions from Phoenix and Andrew E
 *
 * BracketPane handles the display and management of all Brackets. It also provides navigational tools
 * to view sections of the Bracket at a time.
 */
public class BracketPane extends BorderPane {

        /**
         * Reference to the graphical representation of the nodes within the bracket.
         */
        private static ArrayList<BracketNode> nodes;
        /**
         * Used to initiate the paint of the bracket nodes
         */
        private ArrayList<StackPane> buttons;
        /**
         * Maps the text "buttons" to it's respective grid-pane
         */
        private HashMap<StackPane, Pane> panes;
        /**
         * Reference to the current bracket.
         */
        private Bracket currentBracket;
        /**
         * Reference to active subtree within current bracket.
         */
        private int displayedSubtree;
        /**
         * Keeps track of whether or not bracket has been finalized.
         */
        private boolean finalized;

        /**
         * Accesses nodes based on their position
         */
        private HashMap<Integer, BracketNode> nodeMap = new HashMap<>();

        /**
         * Whether or not a bracket is a simulated bracket.
         */
        private boolean isSimulated;
        /**
         * Holds the GridPane to be centered upon for any given display
         */
        private GridPane center;
        /**
         * The full display of a bracket
         */
        private GridPane fullPane;

        /**
         * Handles clicked events for BracketNode objects
         */
        private EventHandler<MouseEvent> clicked = mouseEvent -> {
                //conditional added by matt 5/7 to differentiate between left and right mouse click
                if (mouseEvent.getButton().equals(MouseButton.PRIMARY) && !isSimulated) {
                        BracketNode n = (BracketNode) mouseEvent.getSource();
                        int treeNum = n.getPos();
                        int nextTreeNum = (treeNum - 1) / 2;
                        //opponentTreeNum stuff by Dov, n.getPos() replaces bracketMaps
                        int opponentTreeNum = 2 * nextTreeNum + 1;
                        if (opponentTreeNum == treeNum) opponentTreeNum = 2 * nextTreeNum + 2;

                        if (!nodeMap.get(nextTreeNum).getName().equals(n.getName()) && !nodeMap.get(opponentTreeNum).getName().equals("")) {
                                currentBracket.removeAbove((nextTreeNum));
                                clearAbove(treeNum);
                                nodeMap.get((n.getPos() - 1) / 2).setName(n.getName());
                                currentBracket.moveTeamUp(treeNum);
                        }
                }
                //added by matt 5/7, shows the teams info if you right click
                else if (mouseEvent.getButton().equals(MouseButton.SECONDARY)) {
                        String text = "";
                        BracketNode n = (BracketNode) mouseEvent.getSource();
                        int treeNum = n.getPos();
                        String teamName = currentBracket.getBracket().get(treeNum);
                        try {
                                TournamentInfo info = new TournamentInfo();
                                Team t = info.getTeam(teamName);
                                //by Tyler - added the last two pieces of info to the pop up window
                                if (!isSimulated) {
                                        text += "Team: " + teamName + " | Ranking: " + t.getRanking() +
                                                "\nMascot: " + t.getNickname() + "\nInfo: " + t.getInfo() +
                                                "\nAverage Offensive PPG: " + t.getOffensePPG() + "\nAverage Defensive PPG: " + t.getDefensePPG();
                                } else { //changes the alert to display score on the if the current BracketPane has been simulated - Phoenix
                                        //used code by Dov to get current match up
                                        int parentNum = (treeNum - 1) / 2;
                                        int childNum1 = 2 * parentNum + 2;
                                        int childNum2 = 2 * parentNum + 1;
                                        //points are displayed if this match was predicted correctly
                                        int points = (int)(32 / Math.pow(2 ,(int)(Math.log10(parentNum + 1) / Math.log10(2))));

                                        text += currentBracket.getBracket().get(childNum1) + "score: " + currentBracket.getTeamScore(childNum1) + " points\n" +
                                                currentBracket.getBracket().get(childNum2) + "score: " + currentBracket.getTeamScore(childNum2) + " points\n" +
                                                "The winner was: " + currentBracket.getBracket().get(parentNum) +
                                                ((nodeMap.get(parentNum).getTextFill().equals(Color.GREEN)) ? "\nYou got " + points + " points": "");


                                }
                        } catch (IOException e) {//if for some reason TournamentInfo isn't working, it will display info not found
                                text += "Info for " + teamName + "not found";
                        }
                        //create a popup with the team info
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.CLOSE);
                        alert.setTitle("March Madness Bracket Simulator");
                        alert.setHeaderText(null);
                        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                        alert.showAndWait();
                }
        };
        /**
         * Handles mouseEntered events for BracketNode objects
         */
        private EventHandler<MouseEvent> enter = mouseEvent -> {
                BracketNode tmp = (BracketNode) mouseEvent.getSource();
                tmp.setStyle("-fx-background-color: lightcyan;");
                tmp.setEffect(new InnerShadow(10, Color.LIGHTCYAN));
        };

        /**
         * Handles mouseExited events for BracketNode objects
         */
        private EventHandler<MouseEvent> exit = mouseEvent -> {
                BracketNode tmp = (BracketNode) mouseEvent.getSource();
                tmp.setStyle(null);
                tmp.setEffect(null);

        };

        /**
         *
         * Initializes the properties needed to construct a bracket.
         */
        public BracketPane(Bracket currentBracket, boolean isSimulated) {
                this.isSimulated = isSimulated;
                displayedSubtree=0; //seems to not do anything? Dov Z
                this.currentBracket = currentBracket;

                
                nodeMap = new HashMap<>();
                panes = new HashMap<>();
                nodes = new ArrayList<>();
                ArrayList<Root> roots = new ArrayList<>();

                center = new GridPane();

                buttons = new ArrayList<>();
                buttons.add(customButton("EAST"));
                buttons.add(customButton("WEST"));
                buttons.add(customButton("MIDWEST"));
                buttons.add(customButton("SOUTH"));
                buttons.add(customButton("FULL"));

                for (int m = 0; m < buttons.size() - 1; m++) {
                        roots.add(new Root(3 + m));
                        panes.put(buttons.get(m), roots.get(m));
                }
                Pane finalPane = createTopTwoPane();

                fullPane = new GridPane();
                GridPane gp1 = new GridPane();
                gp1.add(roots.get(0), 0, 0);
                gp1.add(roots.get(1), 0, 1);
                GridPane gp2 = new GridPane();
                gp2.add(roots.get(2), 0, 0);
                gp2.add(roots.get(3), 0, 1);
                gp2.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

                fullPane.add(gp1, 0, 0);
                fullPane.add(finalPane, 1, 0, 1, 2);
                fullPane.add(gp2, 2, 0);
                fullPane.setAlignment(Pos.CENTER);
                panes.put(buttons.get((buttons.size() - 1)), fullPane);
                finalPane.toBack();

                // Initializes the button grid
                GridPane buttonGrid = new GridPane();
                for (int i = 0; i < buttons.size(); i++)
                        buttonGrid.add(buttons.get(i), 0, i);
                buttonGrid.setAlignment(Pos.CENTER);

                // set default center to the button grid
                this.setCenter(buttonGrid);

                for (StackPane t : buttons) {
                        t.setOnMouseEntered(mouseEvent -> {
                                t.setStyle("-fx-background-color: lightblue;");
                                t.setEffect(new InnerShadow(10, Color.LIGHTCYAN));
                        });
                        t.setOnMouseExited(mouseEvent -> {
                                t.setStyle("-fx-background-color: orange;");
                                t.setEffect(null);
                        });
                        t.setOnMouseClicked(mouseEvent -> {
                                setCenter(null);
                                /*
                                 * Grant & Tyler
                                 * 			panes are added as ScrollPanes to retain center alignment when moving through full-view and region-view
                                 */
                                center.add(new ScrollPane(panes.get(t)), 0, 0);
                                center.setAlignment(Pos.CENTER);
                                setCenter(center);
                                //Grant 5/7 this is for clearing the tree it kind of works 
                                //Grant what does this mean Dov Z
                                displayedSubtree=buttons.indexOf(t)==7?0:buttons.indexOf(t)+3;
                                //fixes boolean statment above
                                if(displayedSubtree == 7) displayedSubtree = 0;
                        });
                }

        }

        /**
         * removes the text from every node above a given index in a tree
         * @param treeNum the starting index to clear from
         */
        private void clearAbove(int treeNum) {
                int nextTreeNum = (treeNum - 1) / 2;
                if (!nodeMap.get(nextTreeNum).getName().isEmpty()) {
                        nodeMap.get(nextTreeNum).setName("");
                        clearAbove(nextTreeNum);
                }
        }

        /**
         * Clears a division of all team selections
         * @return the current subtree being displayed
         * Dov
         */
        public int clear(){
                clearSubtree(displayedSubtree);

                return displayedSubtree;
        }

        /**
         * returns a gridPane containing the full display of brackets
         * @return the full display of brackets
         */
        public GridPane getFullPane() {
                return fullPane;
        }

        /**
         * Swaps to the Pane that corresponds to a given number
         * @param regionNum a number that indicates a region to be shown
         */
        public void switchToRegion(int regionNum){
                center = new GridPane();
                center.add(new ScrollPane(panes.get(buttons.get(regionNum==0?4:regionNum-3))), 0, 0);
                center.setAlignment(Pos.CENTER);
                setCenter(center);
                displayedSubtree = regionNum;
        }

        /**
         * Helpful method to retrieve our magical numbers.
         * Each number corresponds to the index of a BracketNode.
         *
         * @param root the root node (3,4,5,6)
         * @param pos  the position in the tree (8 (16) , 4 (8) , 2 (4) , 1 (2))
         * @return The list representing the valid values.
         */
        public ArrayList<Integer> helper(int root, int pos) {
                ArrayList<Integer> positions = new ArrayList<>();
                int base = 0;
                int tmp = (root * 2) + 1;
                if (pos == 8) base = 3;
                else if (pos == 4) base = 2;
                else if (pos == 2) base = 1;
                for (int i = 0; i < base; i++) tmp = (tmp * 2) + 1;
                for (int j = 0; j < pos * 2; j++) positions.add(tmp + j);
                return positions; //                while ((tmp = ((location * 2) + 1)) <= 127) ;
        }

        /**
         * Sets the current bracket to,
         *
         * @param target The bracket to replace currentBracket
         */
        public void setBracket(Bracket target) {
                currentBracket = target;
        }

        /**
         * Clears the sub tree from,
         *
         * @param position The position to clear after
         */
        public void clearSubtree(int position) {
                currentBracket.resetSubtree(position);
        }

        /**
         * Resets the bracket-display
         */
        public void resetBracket() {
                currentBracket.resetSubtree(0);
        }

        /**
         * Requests a message from current bracket to tell if the bracket
         * has been completed.
         *
         * @return True if completed, false otherwise.
         */
        public boolean isComplete() {
                List<Integer> empties = currentBracket.empties();
                for (Integer emptySpot : empties) {
                        nodeMap.get(emptySpot).highlightRect();
                }
                return currentBracket.isComplete();
        }

        /**
         * Highlights correctly predicted team wins
         * @param correctIndices the indices that correspond node with a correct prediction
         * Phoenix
         */
        public void highlightCorrect(ArrayList<Integer> correctIndices) {
                for (int i = 0; i < nodeMap.size() / 2; i++) {
                        Color color = (correctIndices.contains(i)) ? Color.GREEN : Color.RED;
                        nodeMap.get(i).highlightText(color);
                }
        }

        /** Returns whether or not the pane has been finalized
         * @return true if the current-bracket is complete and the value of finalized is also true.
         */
        public boolean isFinalized() {
                return currentBracket.isComplete() && finalized;
        }

        /**
         * @param isFinalized The value to set finalized to.
         */
        public void setFinalized(boolean isFinalized) {
                finalized = isFinalized && currentBracket.isComplete();
        }

        /**
         * Returns a custom "Button" with specified
         *
         * @param name The name of the button
         * @return pane The stack-pane "button"
         */
        private StackPane customButton(String name) {
                StackPane pane = new StackPane();
                Rectangle r = new Rectangle(100, 50, Color.TRANSPARENT);
                Text t = new Text(name);
                t.setTextAlignment(TextAlignment.CENTER);
                pane.getChildren().addAll(r, t);
                pane.setStyle("-fx-background-color: orange;");
                return pane;
        }

        /**
         * Creates and fills the Pane that will contain the top two teams in the bracket
         * This method was compressed by Dov
         * @return a Pane containing 3 nodes to represent the top two teams
         */
        public Pane createTopTwoPane() {
                Pane finalPane = new Pane();

                BracketNode[] nodeArr = new BracketNode[3];
                
                BracketNode nodeFinal0 = new BracketNode("", 162, 300, 100, 20);
                BracketNode nodeFinal1 = new BracketNode("", 75, 400, 100, 20);
                BracketNode nodeFinal2 = new BracketNode("", 250, 400, 100, 20);
                
                nodeArr[0] = nodeFinal0;
                nodeArr[1] = nodeFinal1;
                nodeArr[2] = nodeFinal2;

                for(int i = 0; i < nodeArr.length; i++) {
                        nodeArr[i].setName(currentBracket.getBracket().get(i));
                        finalPane.getChildren().add(nodeArr[i]);
                        nodeArr[i].setOnMouseClicked(clicked);
                        nodeArr[i].setOnMouseDragEntered(enter);
                        nodeArr[i].setOnMouseDragExited(exit);
                        nodeArr[i].setStyle("-fx-border-color: darkblue");
                }
                //bracketMaps replaced by nodeFinalx.setPos() by Dov
                nodeMap.put(1, nodeFinal1);
                nodeFinal1.setPos(1);
                nodeMap.put(2, nodeFinal2);
                nodeFinal2.setPos(2);
                nodeMap.put(0, nodeFinal0);
                nodeFinal0.setPos(0);

                finalPane.setMinWidth(400.0);

                return finalPane;
        }

        /**
         * Creates the graphical representation of a subtree.
         */
        private class Root extends Pane {
                /**
                 * The index location of the starting root itself
                 */
                private int location;

                /**
                 * Constructs a segment of the overall tree from a starting location
                 * @param location the index that this node corresponds to
                 */
                public Root(int location) {
                        this.location = location;
                        createVertices(420, 200, 100, 20, 0, 0);
                        createVertices(320, 120, 100, 200, 1, 0);
                        createVertices(220, 60, 100, 100, 2, 200);
                        createVertices(120, 35, 100, 50, 4, 100);
                        createVertices(20, 25, 100, 25, 8, 50);
                        for (BracketNode n : nodes) {
                                n.setOnMouseClicked(clicked);
                                n.setOnMouseEntered(enter);
                                n.setOnMouseExited(exit);
                        }
                }

                /**
                 * Creates 3 lines in appropriate location unless it is the last line.
                 * Adds these lines and "BracketNodes" to the Pane of this inner class
                 *bracketMaps replaced by setPos() by Dov
                 */
                private void createVertices(int iX, int iY, int iXO, int iYO, int num, int increment) {
                        int y = iY;
                        if (num == 0 && increment == 0) {
                                BracketNode last = new BracketNode("", iX, y - 20, iXO, 20);
                                nodes.add(last);
                                getChildren().addAll(new Line(iX, iY, iX + iXO, iY), last);
                                last.setName(currentBracket.getBracket().get(location));
                                nodeMap.put(location, last);
                                last.setPos(location);
                        } else {
                                ArrayList<BracketNode> aNodeList = new ArrayList<>();
                                for (int i = 0; i < num; i++) {
                                        Point2D tl = new Point2D(iX, y);
                                        Point2D tr = new Point2D(iX + iXO, y);
                                        Point2D bl = new Point2D(iX, y + iYO);
                                        Point2D br = new Point2D(iX + iXO, y + iYO);
                                        BracketNode nTop = new BracketNode("", iX, y - 20, iXO, 20);
                                        aNodeList.add(nTop);
                                        nodes.add(nTop);
                                        BracketNode nBottom = new BracketNode("", iX, y + (iYO - 20), iXO, 20);
                                        aNodeList.add(nBottom);
                                        nodes.add(nBottom);
                                        Line top = new Line(tl.getX(), tl.getY(), tr.getX(), tr.getY());
                                        Line bottom = new Line(bl.getX(), bl.getY(), br.getX(), br.getY());
                                        Line right = new Line(tr.getX(), tr.getY(), br.getX(), br.getY());
                                        getChildren().addAll(top, bottom, right, nTop, nBottom);
                                        y += increment;
                                }
                                ArrayList<Integer> tmpHelp = helper(location, num);
                                for (int j = 0; j < aNodeList.size(); j++) {
                                        aNodeList.get(j).setName(currentBracket.getBracket().get(tmpHelp.get(j)));
                                        nodeMap.put(tmpHelp.get(j), aNodeList.get(j));
                                        aNodeList.get(j).setPos(tmpHelp.get(j));
                                }
                        }

                }
        }

        /**
         * The BracketNode model for the Graphical display of the "Bracket"
         */
        private class BracketNode extends Pane {
                private String teamName;
                private Rectangle rect;
                private Label name;
                //position related code added by Dov
                private int position;

                /**
                 * Creates a BracketNode with,
                 *
                 * @param teamName The name if any
                 * @param x        The starting x location
                 * @param y        The starting y location
                 * @param rX       The width of the rectangle to fill pane
                 * @param rY       The height of the rectangle
                 */
                public BracketNode(String teamName, int x, int y, int rX, int rY) {
                        this.setLayoutX(x);
                        this.setLayoutY(y);
                        this.setMaxSize(rX, rY);
                        this.teamName = teamName;
                        rect = new Rectangle(rX, rY);
                        rect.setFill(Color.TRANSPARENT);
                        name = new Label(teamName);
                        name.setTranslateX(5);
                        getChildren().addAll(rect, name);
                }

                /**
                 * gets the positional index in the tree
                 * @return the position
                 */
                public int getPos() {
                        return position;
                }

                /**
                 * sets a new positional index
                 * @param newPos the new index
                 */
                public void setPos(int newPos) {
                        position = newPos;
                }

                /**
                 * @return teamName The teams name.
                 */
                public String getName() {
                        return teamName;
                }

                /**
                 * @param teamName The name to assign to the node.
                 */
                public void setName(String teamName) {
                        unhighlight();
                        this.teamName = teamName;
                        name.setText(teamName);
                }

                /**
                 * Highlights the current node (used to indicate it need to be filled before finalizing)
                 * Andrew E
                 */
                public void highlightRect() {
                        rect.setFill(Color.LIGHTPINK);
                }

                /**
                 * Highlights the text in a given Color
                 * @param color the color to be changed to
                 * Phoenix
                 */
                public void highlightText(Color color) {
                        name.setTextFill(color);
                }

                /**
                 * Gets the current color used to fill the text
                 * @return the current color
                 * Phoenix
                 */
                public Paint getTextFill() {
                        return name.getTextFill();
                }

                /**
                 * Removes highlight from the current node (called when the team name is updated).
                 * Andrew E
                 */
                public void unhighlight() {
                        rect.setFill(Color.TRANSPARENT);
                }
        }
}
